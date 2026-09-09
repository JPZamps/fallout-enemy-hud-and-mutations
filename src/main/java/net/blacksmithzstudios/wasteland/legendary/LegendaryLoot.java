package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.gear.GearLegends;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Legendary drops are ordinary items - vanilla or modded, whatever the config lists -
 * made interesting by the roll: an over-enchanted piece of gear plus a handful of supplies.
 */
public final class LegendaryLoot {

    /** How many enchantments a signature piece carries at most. */
    private static final int MAX_ENCHANTMENTS = 4;

    private LegendaryLoot() {
    }

    /**
     * Builds the legendary reward. Returned rather than spawned, so it can be handed to the
     * loot table by {@link LegendaryLootModifier} and pass through every other loot hook.
     *
     * A legendary that survived long enough to mutate is worth twice as much: two signature
     * pieces instead of one, and double the supply rolls.
     */
    public static List<ItemStack> buildDrops(RegistryAccess registries, RandomSource random,
                                             LegendaryPrefix prefix, int rolls, boolean mutated) {
        List<ItemStack> drops = new ArrayList<>();

        List<Item> gear = gearPool();
        List<Item> supplies = supplyPool();

        int signatures = mutated ? 2 : 1;
        int supplyRolls = mutated ? rolls * 2 : rolls;

        for (int i = 0; i < signatures; i++) {
            ItemStack signature = new ItemStack(gear.get(random.nextInt(gear.size())));
            enchantBeyondVanilla(signature, registries, random, mutated);

            String label = mutated
                    ? LegendaryData.STAR + " Legendary " + signature.getItem().getName(signature).getString()
                    : prefix.displayName() + " " + signature.getItem().getName(signature).getString();
            signature.set(DataComponents.CUSTOM_NAME, Component.literal(label).withStyle(prefix.color()));

            // The signature piece carries a legendary effect of its own, which also renames it.
            if (WastelandConfig.GEAR_LEGENDS_ENABLED.get()) {
                GearLegends.applyRandom(signature, random, isArmour(signature));
            }
            drops.add(signature);
        }

        for (int i = 0; i < supplyRolls; i++) {
            drops.add(new ItemStack(supplies.get(random.nextInt(supplies.size())), 1 + random.nextInt(3)));
        }

        return drops;
    }

    /**
     * What counts as armour in 26.2: ArmorItem no longer exists, and a piece is armour
     * because it carries an equippable component pointing at an armour slot.
     */
    private static boolean isArmour(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }
        EquipmentSlot slot = equippable.slot();
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    /**
     * Supplies for an elite kill, scaled by rank, plus a signature piece for the upper ranks.
     * Elites are a separate axis from the legendary prefixes, so they pay out on their own.
     */
    public static List<ItemStack> buildEliteDrops(RegistryAccess registries, RandomSource random,
                                                  EliteRank rank, boolean withSignature) {
        List<ItemStack> drops = new ArrayList<>();
        List<Item> supplies = supplyPool();

        for (int i = 0; i < rank.supplyRolls(); i++) {
            drops.add(new ItemStack(supplies.get(random.nextInt(supplies.size())), 1 + random.nextInt(3)));
        }

        if (withSignature) {
            List<Item> gear = gearPool();
            ItemStack signature = new ItemStack(gear.get(random.nextInt(gear.size())));
            enchantBeyondVanilla(signature, registries, random, false);
            signature.set(DataComponents.CUSTOM_NAME, Component
                    .literal(EliteRank.SKULL + " " + rank.title() + " "
                            + signature.getItem().getName(signature).getString())
                    .withStyle(rank.color()));
            drops.add(signature);
        }

        return drops;
    }

    /**
     * Enchants a piece past the vanilla ceiling: Protection VI, Sharpness VII and the like,
     * levels no enchanting table or anvil can reach.
     *
     * Since 1.21 enchantments are data driven, so rather than asking vanilla to roll a set we
     * pick from the registry ourselves. That keeps the over-levelling honest, respects each
     * enchantment's own compatibility rules, and picks up enchantments from other mods for free.
     */
    private static void enchantBeyondVanilla(ItemStack stack, RegistryAccess registries,
                                             RandomSource random, boolean mutated) {
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);

        List<Holder.Reference<Enchantment>> candidates = lookup.listElements()
                .filter(holder -> holder.value().canEnchant(stack))
                .filter(holder -> !holder.is(EnchantmentTags.CURSE))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (candidates.isEmpty()) {
            return;
        }

        List<Holder<Enchantment>> chosen = new ArrayList<>();
        int wanted = 1 + random.nextInt(MAX_ENCHANTMENTS);
        while (chosen.size() < wanted && !candidates.isEmpty()) {
            Holder<Enchantment> pick = candidates.remove(random.nextInt(candidates.size()));
            // Vanilla decides what may sit alongside what; Sharpness and Smite still exclude.
            if (chosen.stream().allMatch(other -> Enchantment.areCompatible(other, pick))) {
                chosen.add(pick);
            }
        }

        boolean overchargeAllowed = WastelandConfig.OVERCHARGED_ENCHANTMENTS.get();
        int headroom = mutated ? 3 : 2;

        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            for (Holder<Enchantment> holder : chosen) {
                int max = holder.value().getMaxLevel();
                int level = overchargeAllowed ? max + 1 + random.nextInt(headroom) : max;
                mutable.set(holder, level);
            }
        });
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
            Identifier key = Identifier.tryParse(id);
            Item item = key == null ? null : BuiltInRegistries.ITEM.getValue(key);
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            items.add(fallback);
        }
        return List.copyOf(items);
    }
}
