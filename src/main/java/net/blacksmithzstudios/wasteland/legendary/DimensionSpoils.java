package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Spoils that belong to a particular dimension: netherite scrap from the Nether, End
 * materials from the End.
 *
 * Only bosses, legendaries and the upper elite ranks qualify, and only where the reward
 * belongs. Rolled independently of the legendary and elite loot, so a plain boss can pay out
 * too.
 */
public final class DimensionSpoils {

    private DimensionSpoils() {
    }

    public static List<ItemStack> roll(LivingEntity entity, RandomSource random) {
        Level level = entity.level();
        if (level.dimension() == Level.NETHER) {
            return rollNether(entity, random);
        }
        if (level.dimension() == Level.END) {
            return rollEnd(entity, random);
        }
        return List.of();
    }

    private static List<ItemStack> rollNether(LivingEntity entity, RandomSource random) {
        if (!WastelandConfig.NETHERITE_SCRAP_ENABLED.get() || !qualifies(entity)) {
            return List.of();
        }
        boolean boss = MobEligibility.isBoss(entity);
        double chance = boss
                ? WastelandConfig.NETHERITE_SCRAP_BOSS_CHANCE.get()
                : WastelandConfig.NETHERITE_SCRAP_CHANCE.get();
        if (random.nextDouble() >= mutationBonus(entity, chance)) {
            return List.of();
        }
        return List.of(new ItemStack(Items.NETHERITE_SCRAP, boss ? 1 + random.nextInt(2) : 1));
    }

    private static List<ItemStack> rollEnd(LivingEntity entity, RandomSource random) {
        if (!WastelandConfig.END_SPOILS_ENABLED.get() || !qualifies(entity)) {
            return List.of();
        }
        boolean boss = MobEligibility.isBoss(entity);
        double chance = boss
                ? WastelandConfig.END_SPOILS_BOSS_CHANCE.get()
                : WastelandConfig.END_SPOILS_CHANCE.get();
        if (random.nextDouble() >= mutationBonus(entity, chance)) {
            return List.of();
        }

        List<Item> pool = endPool();
        List<ItemStack> drops = new ArrayList<>();
        int rolls = boss ? 2 + random.nextInt(3) : 1;
        for (int i = 0; i < rolls; i++) {
            drops.add(new ItemStack(pool.get(random.nextInt(pool.size())), 1 + random.nextInt(2)));
        }
        return drops;
    }

    /** Bosses, legendaries, and elites from the Elite rank up. */
    private static boolean qualifies(LivingEntity entity) {
        if (MobEligibility.isBoss(entity)) {
            return true;
        }
        if (LegendaryData.prefixOf(entity) != null) {
            return true;
        }
        EliteRank elite = LegendaryData.eliteOf(entity);
        return elite != null && elite.ordinal() >= EliteRank.ELITE.ordinal();
    }

    /** Surviving to mutate doubles the odds, the same way it doubles everything else. */
    private static double mutationBonus(LivingEntity entity, double chance) {
        if (LegendaryData.prefixOf(entity) != null && LegendaryData.hasMutated(entity)) {
            return Math.min(1.0, chance * 2.0);
        }
        return chance;
    }

    private static List<? extends String> endSource;
    private static List<Item> endCache = List.of();

    /** Resolved once per config load, like the other pools. */
    private static synchronized List<Item> endPool() {
        List<? extends String> configured = WastelandConfig.END_SPOILS_ITEMS.get();
        if (configured != endSource) {
            endCache = LegendaryLoot.resolvePool(configured, Items.ENDER_PEARL);
            endSource = configured;
        }
        return endCache;
    }
}
