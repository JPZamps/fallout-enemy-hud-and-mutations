package net.blacksmithzstudios.wasteland.gear;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Reads and writes the legendary roll carried by a piece of gear.
 *
 * The roll lives in the stack's own NBT, so it survives being dropped, picked up, stored in a
 * chest and carried across worlds - and it rides on ordinary vanilla items, which is what
 * keeps the whole system free of new assets.
 */
public final class GearLegends {

    private static final String WEAPON_KEY = "WastelandWeaponLegend";
    private static final String ARMOR_KEY = "WastelandArmorLegend";

    private GearLegends() {
    }

    public static WeaponLegend weaponOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(WEAPON_KEY) ? null : WeaponLegend.byName(tag.getString(WEAPON_KEY));
    }

    public static ArmorLegend armorOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(ARMOR_KEY) ? null : ArmorLegend.byName(tag.getString(ARMOR_KEY));
    }

    /** Stamps a weapon roll onto a stack and renames it to match. */
    public static void applyWeapon(ItemStack stack, WeaponLegend legend) {
        stack.getOrCreateTag().putString(WEAPON_KEY, legend.name());
        stack.setHoverName(Component
                .literal(legend.displayName() + " " + stack.getItem().getDescription().getString())
                .withStyle(legend.color()));
    }

    /** Stamps an armour roll onto a stack and renames it to match. */
    public static void applyArmor(ItemStack stack, ArmorLegend legend) {
        stack.getOrCreateTag().putString(ARMOR_KEY, legend.name());
        stack.setHoverName(Component
                .literal(legend.displayName() + " " + stack.getItem().getDescription().getString())
                .withStyle(legend.color()));
    }

    /** Rolls whichever kind suits the item, and returns whether anything was applied. */
    public static boolean applyRandom(ItemStack stack, RandomSource random, boolean armor) {
        if (stack.isEmpty()) {
            return false;
        }
        if (armor) {
            applyArmor(stack, ArmorLegend.roll(random));
        } else {
            applyWeapon(stack, WeaponLegend.roll(random));
        }
        return true;
    }
}
