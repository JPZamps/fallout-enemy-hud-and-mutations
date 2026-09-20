package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * What a boss leaves behind, spawned directly rather than added to a loot table.
 *
 * This exists because the Ender Dragon rolls no loot table whatsoever: everything the loot
 * modifier does is invisible to it. The Wither does roll one, but it holds only the nether
 * star, and a Wither is a creature of the Nether no matter which dimension you drag it into,
 * so its rewards ignore where it died.
 */
public final class BossSpoils {

    private BossSpoils() {
    }

    public static void dropFor(LivingEntity entity) {
        if (!WastelandConfig.BOSS_SPOILS_ENABLED.get() || !MobEligibility.isBoss(entity)) {
            return;
        }

        RandomSource random = entity.getRandom();
        List<ItemStack> drops = new ArrayList<>();

        if (entity.getType() == bossType("wither")) {
            drops.add(stack(Items.NETHER_STAR, 1, 2, random));
            drops.add(stack(Items.DIAMOND, 4, 8, random));
            drops.add(stack(Items.NETHERITE_SCRAP, 2, 5, random));
        } else if (entity.getType() == bossType("ender_dragon")) {
            drops.add(stack(Items.NETHERITE_INGOT, 1, 3, random));
            List<Item> pool = DimensionSpoils.endPool();
            int rolls = 3 + random.nextInt(4);
            for (int i = 0; i < rolls; i++) {
                drops.add(new ItemStack(pool.get(random.nextInt(pool.size())), 1 + random.nextInt(2)));
            }
        } else {
            return; // a modded boss: leave its own rewards alone
        }

        // A legendary boss is worth more, and one that mutated more still.
        int multiplier = 1;
        if (LegendaryData.prefixOf(entity) != null) {
            multiplier = LegendaryData.hasMutated(entity) ? 3 : 2;
        }

        Level level = entity.level();
        for (ItemStack stack : drops) {
            for (int i = 0; i < multiplier; i++) {
                level.addFreshEntity(new ItemEntity(level,
                        entity.getX(), entity.getY() + 0.5, entity.getZ(), stack.copy()));
            }
        }
    }

    private static ItemStack stack(Item item, int min, int max, RandomSource random) {
        return new ItemStack(item, min + random.nextInt(max - min + 1));
    }

    /** Looked up by name so the two vanilla bosses can be named without a hard import. */
    private static EntityType<?> bossType(String name) {
        return name.equals("wither") ? WITHER : ENDER_DRAGON;
    }

    private static final EntityType<?> WITHER = MobEligibility.witherType();
    private static final EntityType<?> ENDER_DRAGON = MobEligibility.enderDragonType();
}
