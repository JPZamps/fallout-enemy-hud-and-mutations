package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Netherite scrap from the Nether's worst.
 *
 * Only bosses, legendaries and the upper elite ranks qualify, and only when they die in the
 * Nether, where scrap belongs. It is rolled independently of the legendary and elite loot, so
 * a plain Wither killed in the Nether can pay out too.
 */
public final class NetherSpoils {

    private NetherSpoils() {
    }

    public static List<ItemStack> roll(LivingEntity entity, RandomSource random) {
        if (!WastelandConfig.NETHERITE_SCRAP_ENABLED.get()
                || entity.level().dimension() != Level.NETHER) {
            return List.of();
        }

        boolean boss = MobEligibility.isBoss(entity);
        LegendaryPrefix prefix = LegendaryData.prefixOf(entity);
        EliteRank elite = LegendaryData.eliteOf(entity);
        boolean highRanked = prefix != null
                || (elite != null && elite.ordinal() >= EliteRank.ELITE.ordinal());
        if (!boss && !highRanked) {
            return List.of();
        }

        double chance = boss
                ? WastelandConfig.NETHERITE_SCRAP_BOSS_CHANCE.get()
                : WastelandConfig.NETHERITE_SCRAP_CHANCE.get();
        // Surviving to mutate doubles the odds, the same way it doubles everything else.
        if (prefix != null && LegendaryData.hasMutated(entity)) {
            chance = Math.min(1.0, chance * 2.0);
        }
        if (random.nextDouble() >= chance) {
            return List.of();
        }

        int count = boss ? 1 + random.nextInt(2) : 1;
        return List.of(new ItemStack(Items.NETHERITE_SCRAP, count));
    }
}
