package net.blacksmithzstudios.wasteland.net;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Works out, on the server, whether anything has noticed the player, and tells their client
 * when the answer changes.
 */
@EventBusSubscriber(modid = WastelandMod.MOD_ID)
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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
        PacketDistributor.sendToPlayer(player, new DetectionPayload(packed / 1000, packed % 1000));
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
        double range = WastelandConfig.DETECTION_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);

        int state = HIDDEN;
        double nearestSqr = Double.MAX_VALUE;

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box,
                candidate -> candidate.isAlive() && candidate instanceof Enemy)) {
            double distanceSqr = mob.distanceToSqr(player);
            if (distanceSqr > range * range) {
                continue; // the box is a cube; the radius is a sphere
            }
            nearestSqr = Math.min(nearestSqr, distanceSqr);

            // Anything in the radius is at least a CAUTION; one that has found you is DANGER.
            state = Math.max(state, mob.getTarget() == player ? DANGER : CAUTION);
        }

        int closeness = 0;
        if (nearestSqr < Double.MAX_VALUE) {
            double nearest = Math.sqrt(nearestSqr);
            closeness = (int) Math.round(Math.max(0.0, Math.min(1.0, 1.0 - nearest / range)) * 100);
        }
        return state * 1000 + closeness;
    }
}
