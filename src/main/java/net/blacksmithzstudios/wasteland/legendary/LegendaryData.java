package net.blacksmithzstudios.wasteland.legendary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/** Legendary state lives in the entity's Forge persistent data, so it survives save/load. */
public final class LegendaryData {

    private static final String PREFIX_KEY = "WastelandLegendary";
    private static final String MUTATED_KEY = "WastelandMutated";

    private LegendaryData() {
    }

    public static boolean isLegendary(LivingEntity entity) {
        return prefixOf(entity) != null;
    }

    public static LegendaryPrefix prefixOf(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(PREFIX_KEY)) {
            return null;
        }
        return LegendaryPrefix.byName(data.getString(PREFIX_KEY));
    }

    public static void setPrefix(LivingEntity entity, LegendaryPrefix prefix) {
        entity.getPersistentData().putString(PREFIX_KEY, prefix.name());
    }

    /** A legendary mutates exactly once per lifetime, as in Fallout. */
    public static boolean hasMutated(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(MUTATED_KEY);
    }

    public static void markMutated(LivingEntity entity) {
        entity.getPersistentData().putBoolean(MUTATED_KEY, true);
    }

    /** True once the entity has been processed, legendary or not, so we only roll once. */
    public static boolean wasRolled(LivingEntity entity) {
        return entity.getPersistentData().contains(PREFIX_KEY)
                || entity.getPersistentData().getBoolean("WastelandRolled");
    }

    public static void markRolled(LivingEntity entity) {
        entity.getPersistentData().putBoolean("WastelandRolled", true);
    }

    private static final String ELITE_KEY = "WastelandElite";

    public static EliteRank eliteOf(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(ELITE_KEY) ? EliteRank.byName(data.getString(ELITE_KEY)) : null;
    }

    public static void setElite(LivingEntity entity, EliteRank rank) {
        entity.getPersistentData().putString(ELITE_KEY, rank.name());
    }

    /** Removes every trace of the mod from an entity: tags, modifiers, glow and name. */
    public static void clear(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(PREFIX_KEY);
        data.remove(MUTATED_KEY);
        data.remove(ELITE_KEY);
        data.remove("WastelandMutationRolled");
        data.remove("WastelandRolled");

        LegendaryPrefix.removeFrom(entity);
        EliteRank.removeFrom(entity);
        entity.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    /** Whether the one mutation roll has already been spent on this mob. */
    public static boolean mutationRolled(LivingEntity entity) {
        return entity.getPersistentData().getBoolean("WastelandMutationRolled");
    }

    public static void markMutationRolled(LivingEntity entity) {
        entity.getPersistentData().putBoolean("WastelandMutationRolled", true);
    }

    /** The star that marks a mutated legendary. U+2605 ships with the vanilla font. */
    public static final String STAR = "★";

    /**
     * What the target readout and the entity nameplate should say: the plain mob name,
     * the prefixed name once it is legendary, or the starred Legendary name once it has
     * mutated.
     */
    public static String displayNameOf(LivingEntity entity) {
        String base = entity.getType().getDescription().getString();
        LegendaryPrefix prefix = prefixOf(entity);
        EliteRank elite = eliteOf(entity);

        StringBuilder name = new StringBuilder();

        if (prefix != null && hasMutated(entity)) {
            // "Legendary" replaces the prefix, but the elite rank still stacks on top of it,
            // giving names like "Legendary Commandant Skeleton".
            name.append(STAR).append(" Legendary ");
            if (elite != null) {
                name.append(elite.title()).append(' ');
            }
            return name.append(base).toString();
        }

        if (elite != null) {
            name.append(EliteRank.SKULL).append(' ').append(elite.title()).append(' ');
        }
        if (prefix != null) {
            name.append(prefix.displayName()).append(' ');
        }
        return name.isEmpty() ? base : name.append(base).toString();
    }

    /** The colour the name should carry: the elite rank wins, then the prefix. */
    public static net.minecraft.ChatFormatting colorOf(LivingEntity entity) {
        EliteRank elite = eliteOf(entity);
        if (elite != null && !hasMutated(entity)) {
            return elite.color();
        }
        LegendaryPrefix prefix = prefixOf(entity);
        return prefix != null ? prefix.color() : net.minecraft.ChatFormatting.WHITE;
    }
}
