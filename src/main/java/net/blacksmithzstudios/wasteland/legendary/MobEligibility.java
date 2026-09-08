package net.blacksmithzstudios.wasteland.legendary;

import net.minecraft.core.registries.BuiltInRegistries;
import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;

/**
 * Decides which mobs the mod touches.
 *
 * The default is deliberately broad: anything aggressive qualifies, which covers modded
 * mobs too, since hostile mobs from other mods implement the same {@link Enemy} marker.
 * Bosses are excluded so their own boss bar stays the only health readout on screen.
 */
public final class MobEligibility {

    private MobEligibility() {
    }

    /** Bosses draw their own bar; a second one on top would be noise. */
    public static boolean isBoss(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        return type == EntityType.ENDER_DRAGON
                || type == EntityType.WITHER
                || type.is(Tags.EntityTypes.BOSSES);
    }

    /** Whether this mob may be promoted to legendary. */
    public static boolean canBeLegendary(LivingEntity entity) {
        if (!WastelandConfig.INCLUDE_BOSSES.get() && isBoss(entity)) {
            return false;
        }

        String id = idOf(entity);
        if (WastelandConfig.MOB_BLACKLIST.get().contains(id)) {
            return false;
        }
        if (WastelandConfig.EXTRA_MOBS.get().contains(id)) {
            return true;
        }
        return entity instanceof Enemy;
    }


    private static String idOf(LivingEntity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null ? key.toString() : "";
    }
}
