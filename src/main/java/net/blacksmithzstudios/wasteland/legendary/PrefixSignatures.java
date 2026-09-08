package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ambient particles that tell the eight rolls apart during a fight.
 *
 * Without this, Savage and Sturdy differ only in numbers the player never sees. One vanilla
 * particle per prefix teaches the roster without a line of text or a single new asset.
 */
@Mod.EventBusSubscriber(modid = WastelandMod.MOD_ID)
public final class PrefixSignatures {

    /** Ticks between puffs. Often enough to read, sparse enough to stay quiet. */
    private static final int INTERVAL = 12;

    private PrefixSignatures() {
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || entity.tickCount % INTERVAL != 0) {
            return;
        }
        if (!WastelandConfig.PREFIX_SIGNATURES.get()) {
            return;
        }
        // Every legendary is named, and checking the name costs nothing. Without this the
        // lookup below would create an empty persistent-data tag on every mob in the world.
        if (!entity.hasCustomName()) {
            return;
        }

        LegendaryPrefix prefix = LegendaryData.prefixOf(entity);
        if (prefix == null) {
            return;
        }

        // A mutated legendary keeps its roll's particle but burns hotter alongside it.
        int count = LegendaryData.hasMutated(entity) ? 4 : 2;
        level.sendParticles(prefix.signature(),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                count, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.01);

        if (LegendaryData.hasMutated(entity)) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
                    1, 0.3, 0.3, 0.3, 0.005);
        }
    }

    /** The Vampiric roll shows its steal on the spot, so the drain is visible, not inferred. */
    public static void showLifesteal(LivingEntity attacker) {
        if (!WastelandConfig.PREFIX_SIGNATURES.get() || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.HEART,
                attacker.getX(), attacker.getY() + attacker.getBbHeight(), attacker.getZ(),
                3, 0.3, 0.2, 0.3, 0.02);
    }
}
