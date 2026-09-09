package net.blacksmithzstudios.wasteland.net;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Works out, on the server, whether anything has noticed the player, and tells their client
 * when the answer changes.
 */
@Mod.EventBusSubscriber(modid = WastelandMod.MOD_ID)
public final class DetectionTracker {

    public static final int HIDDEN = 0;
    public static final int CAUTION = 1;
    public static final int DANGER = 2;

    /** Recomputed twice a second: fast enough to feel live, cheap enough to ignore. */
    private static final int INTERVAL = 10;

    private static final Map<UUID, Integer> lastSent = new HashMap<>();

    private DetectionTracker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % INTERVAL != 0 || !WastelandConfig.DETECTION_ENABLED.get()) {
            return;
        }

        // Packed as state * 1000 + closeness, so a change in either resends.
        int packed = compute(player);
        Integer previous = lastSent.get(player.getUUID());
        if (previous != null && previous == packed) {
            return;
        }
        lastSent.put(player.getUUID(), packed);
        WastelandNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DetectionPacket(packed / 1000, packed % 1000));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastSent.remove(event.getEntity().getUUID());
    }

    /**
     * DANGER once something is actually hunting you, CAUTION while hostiles are merely inside
     * the radius, HIDDEN when the radius is empty. Also reports how close the nearest one is,
     * which is what makes the brackets close in.
     *
     * @return state * 1000 + closeness, where closeness runs 0 (at the edge) to 100 (on top of you)
     */
    private static int compute(ServerPlayer player) {
        double cautionRange = WastelandConfig.DETECTION_RANGE.get();
        double dangerRange = Math.max(cautionRange, WastelandConfig.DANGER_RANGE.get());

        // Scan out to whichever radius is larger: something can hunt you from well beyond
        // the distance at which a mob merely standing there is worth a CAUTION.
        AABB box = player.getBoundingBox().inflate(dangerRange);

        int state = HIDDEN;
        double nearestHunterSqr = Double.MAX_VALUE;
        double nearestNearbySqr = Double.MAX_VALUE;

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box,
                candidate -> candidate.isAlive() && candidate instanceof Enemy)) {
            double distanceSqr = mob.distanceToSqr(player);

            // Being hunted is a danger at any distance inside the wider radius.
            if (mob.getTarget() == player && distanceSqr <= dangerRange * dangerRange) {
                state = DANGER;
                nearestHunterSqr = Math.min(nearestHunterSqr, distanceSqr);
                continue;
            }

            // Otherwise it only counts once it is genuinely close. The box is a cube and the
            // radius is a sphere, so the corners have to be discarded.
            if (distanceSqr <= cautionRange * cautionRange) {
                state = Math.max(state, CAUTION);
                nearestNearbySqr = Math.min(nearestNearbySqr, distanceSqr);
            }
        }

        // The brackets track whatever set the state, measured against that state's own range.
        double nearestSqr = state == DANGER ? nearestHunterSqr : nearestNearbySqr;
        double against = state == DANGER ? dangerRange : cautionRange;

        int closeness = 0;
        if (nearestSqr < Double.MAX_VALUE) {
            double nearest = Math.sqrt(nearestSqr);
            closeness = (int) Math.round(Math.max(0.0, Math.min(1.0, 1.0 - nearest / against)) * 100);
        }
        return state * 1000 + closeness;
    }
}
