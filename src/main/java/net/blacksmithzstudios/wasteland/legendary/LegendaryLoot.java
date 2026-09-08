package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.gear.GearLegends;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legendary drops are ordinary items - vanilla or modded, whatever the config lists -
 * made interesting by the roll: an over-enchanted piece of gear plus a handful of supplies.
 */
public final class LegendaryLoot {

    private LegendaryLoot() {
    }

    /**
     * Builds the legendary reward. Returned rather than spawned, so it can be handed to the
     * loot table by {@link LegendaryLootModifier} and pass through every other loot hook.
     *
     * A legendary that survived long enough to mutate is worth twice as much: two signature
     * pieces instead of one, and double the supply rolls.
     */
    public static List<ItemStack> buildDrops(RandomSource random, LegendaryPrefix prefix,
                                             int rolls, boolean mutated) {
        List<ItemStack> drops = new ArrayList<>();

        List<Item> gear = gearPool();
        List<Item> supplies = supplyPool();

        int signatures = mutated ? 2 : 1;
        int supplyRolls = mutated ? rolls * 2 : rolls;

        // Signature pieces: gear pushed well past normal enchanting-table levels.
        for (int i = 0; i < signatures; i++) {
            ItemStack signature = new ItemStack(gear.get(random.nextInt(gear.size())));
            EnchantmentHelper.enchantItem(random, signature, 25 + random.nextInt(15), true);
            overcharge(signature, random, mutated);

            String label = mutated
                    ? LegendaryData.STAR + " Legendary " + signature.getItem().getDescription().getString()
                    : prefix.displayName() + " " + signature.getItem().getDescription().getString();
            signature.setHoverName(Component.literal(label).withStyle(prefix.color()));

            // The signature piece carries a legendary effect of its own, which also renames it.
            if (WastelandConfig.GEAR_LEGENDS_ENABLED.get()) {
                GearLegends.applyRandom(signature, random, signature.getItem() instanceof ArmorItem);
            }
            drops.add(signature);
        }

        // Plus the supply rolls.
        for (int i = 0; i < supplyRolls; i++) {
            drops.add(new ItemStack(supplies.get(random.nextInt(supplies.size())), 1 + random.nextInt(3)));
        }

        return drops;
    }

    private static List<? extends String> gearSource;
    private static List<Item> gearCache = List.of();
    private static List<? extends String> supplySource;
    private static List<Item> supplyCache = List.of();

    /** Resolves the gear pool once per config load rather than on every kill. */
    private static synchronized List<Item> gearPool() {
        List<? extends String> configured = WastelandConfig.GEAR_ITEMS.get();
        if (configured != gearSource) {
            gearCache = resolve(configured, Items.IRON_SWORD);
            gearSource = configured;
        }
        return gearCache;
    }

    private static synchronized List<Item> supplyPool() {
        List<? extends String> configured = WastelandConfig.SUPPLY_ITEMS.get();
        if (configured != supplySource) {
            supplyCache = resolve(configured, Items.BREAD);
            supplySource = configured;
        }
        return supplyCache;
    }

    /** Turns configured registry ids into items, quietly skipping ones no mod provides. */
    private static List<Item> resolve(List<? extends String> ids, Item fallback) {
        List<Item> items = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            items.add(fallback);
        }
        return List.copyOf(items);
    }

    /**
     * Supplies for an elite kill, scaled by rank, plus a signature piece for the upper ranks.
     * Elites are a separate axis from the legendary prefixes, so they pay out on their own.
     */
    public static List<ItemStack> buildEliteDrops(RandomSource random, EliteRank rank,
                                                  boolean withSignature) {
        List<ItemStack> drops = new ArrayList<>();
        List<Item> supplies = supplyPool();

        for (int i = 0; i < rank.supplyRolls(); i++) {
            drops.add(new ItemStack(supplies.get(random.nextInt(supplies.size())), 1 + random.nextInt(3)));
        }

        if (withSignature) {
            List<Item> gear = gearPool();
            ItemStack signature = new ItemStack(gear.get(random.nextInt(gear.size())));
            EnchantmentHelper.enchantItem(random, signature, 25 + random.nextInt(15), true);
            overcharge(signature, random, false);
            signature.setHoverName(Component
                    .literal(EliteRank.SKULL + " " + rank.title() + " "
                            + signature.getItem().getDescription().getString())
                    .withStyle(rank.color()));
            drops.add(signature);
        }

        return drops;
    }

    /**
     * Pushes the rolled enchantments past their vanilla ceiling - Protection VI, Sharpness VII
     * and the like. Minecraft honours levels above the maximum when they are set directly;
     * they simply cannot be reached through an enchanting table or an anvil.
     */
    private static void overcharge(ItemStack stack, RandomSource random, boolean mutated) {
        if (!WastelandConfig.OVERCHARGED_ENCHANTMENTS.get()) {
            return;
        }

        Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (enchantments.isEmpty()) {
            enchantments.put(Enchantments.UNBREAKING, Enchantments.UNBREAKING.getMaxLevel());
        }

        int headroom = mutated ? 3 : 2;
        boolean boostedOne = false;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            // The first is always overcharged; the rest are a coin flip, so drops vary.
            if (boostedOne && random.nextBoolean()) {
                continue;
            }
            entry.setValue(entry.getKey().getMaxLevel() + 1 + random.nextInt(headroom));
            boostedOne = true;
        }

        EnchantmentHelper.setEnchantments(enchantments, stack);
    }
}
