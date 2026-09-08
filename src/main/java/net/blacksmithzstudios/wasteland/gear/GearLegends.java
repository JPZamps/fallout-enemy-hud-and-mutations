package net.blacksmithzstudios.wasteland.gear;

import com.mojang.serialization.Codec;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponents;

import java.util.function.Supplier;

/**
 * Reads and writes the legendary roll carried by a piece of gear.
 *
 * Since 1.20.5 an item's extra state lives in typed data components rather than loose NBT,
 * so each roll is a registered component holding the enum's name. It still survives being
 * dropped, picked up, stored in a chest and carried across worlds, and it still rides on an
 * ordinary vanilla item, which is what keeps the system free of new assets.
 */
public final class GearLegends {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, WastelandMod.MOD_ID);

    /** The weapon roll, stored as the enum constant's name. */
    public static final Supplier<DataComponentType<String>> WEAPON_LEGEND =
            COMPONENTS.register("weapon_legend", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** The armour roll, stored the same way. */
    public static final Supplier<DataComponentType<String>> ARMOR_LEGEND =
            COMPONENTS.register("armor_legend", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private GearLegends() {
    }

    public static WeaponLegend weaponOf(ItemStack stack) {
        String name = stack.get(WEAPON_LEGEND.get());
        return name == null ? null : WeaponLegend.byName(name);
    }

    public static ArmorLegend armorOf(ItemStack stack) {
        String name = stack.get(ARMOR_LEGEND.get());
        return name == null ? null : ArmorLegend.byName(name);
    }

    /** Stamps a weapon roll onto a stack and renames it to match. */
    public static void applyWeapon(ItemStack stack, WeaponLegend legend) {
        stack.set(WEAPON_LEGEND.get(), legend.name());
        rename(stack, legend.displayName(), legend.color());
    }

    /** Stamps an armour roll onto a stack and renames it to match. */
    public static void applyArmor(ItemStack stack, ArmorLegend legend) {
        stack.set(ARMOR_LEGEND.get(), legend.name());
        rename(stack, legend.displayName(), legend.color());
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

    private static void rename(ItemStack stack, String prefix, net.minecraft.ChatFormatting color) {
        stack.set(DataComponents.CUSTOM_NAME, Component
                .literal(prefix + " " + stack.getItem().getName(stack).getString())
                .withStyle(color));
    }
}
