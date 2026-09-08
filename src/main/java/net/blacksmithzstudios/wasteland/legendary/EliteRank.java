package net.blacksmithzstudios.wasteland.legendary;

import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;



/**
 * Higher-level enemies, the way Fallout marks something well above your level: far more
 * health, real gear, and a skull beside the name.
 *
 * This is a separate axis from the legendary prefixes. An elite is simply a tougher version
 * of the same mob - it does not mutate on its own, so the danger is raw stats rather than a
 * mid-fight transformation.
 */
public enum EliteRank {

    TOUGH      ("Tough",      ChatFormatting.WHITE,        1.6, 1.20,  2.0, 30),
    HARDENED   ("Hardened",   ChatFormatting.GRAY,         2.2, 1.40,  4.0, 22),
    SEASONED   ("Seasoned",   ChatFormatting.YELLOW,       2.8, 1.60,  6.0, 16),
    VETERAN    ("Veteran",    ChatFormatting.GOLD,         3.4, 1.85,  8.0, 12),
    ELITE      ("Elite",      ChatFormatting.AQUA,         4.0, 2.05, 10.0,  9),
    COMMANDANT ("Commandant", ChatFormatting.LIGHT_PURPLE, 4.6, 2.25, 12.0,  6),
    WARLORD    ("Warlord",    ChatFormatting.RED,          5.4, 2.50, 14.0,  3),
    GENERAL    ("General",    ChatFormatting.DARK_RED,     6.2, 2.80, 16.0,  2);

    /** The skull Fallout puts beside an enemy far above your level. U+2620. */
    public static final String SKULL = "☠";

    private static final ResourceLocation HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(WastelandMod.MOD_ID, "wasteland_elite_health");
    private static final ResourceLocation DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(WastelandMod.MOD_ID, "wasteland_elite_damage");
    private static final ResourceLocation ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(WastelandMod.MOD_ID, "wasteland_elite_armor");
    private static final ResourceLocation KNOCKBACK_ID =
            ResourceLocation.fromNamespaceAndPath(WastelandMod.MOD_ID, "wasteland_elite_knockback");

    private final String title;
    private final ChatFormatting color;
    private final double healthBonus;
    private final double damageBonus;
    private final double armorBonus;
    private final int weight;

    EliteRank(String title, ChatFormatting color,
              double healthBonus, double damageBonus, double armorBonus, int weight) {
        this.title = title;
        this.color = color;
        this.healthBonus = healthBonus;
        this.damageBonus = damageBonus;
        this.armorBonus = armorBonus;
        this.weight = weight;
    }

    public String title() {
        return title;
    }

    public ChatFormatting color() {
        return color;
    }

    /** Weighted roll: a Tough is common, a General is something you tell people about. */
    public static EliteRank roll(RandomSource random) {
        int total = 0;
        for (EliteRank rank : values()) {
            total += rank.weight;
        }
        int draw = random.nextInt(total);
        for (EliteRank rank : values()) {
            draw -= rank.weight;
            if (draw < 0) {
                return rank;
            }
        }
        return TOUGH;
    }

    public static EliteRank byName(String name) {
        for (EliteRank rank : values()) {
            if (rank.name().equals(name)) {
                return rank;
            }
        }
        return null;
    }

    public void applyTo(LivingEntity entity, double strength) {
        addMultiplier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, healthBonus * strength);
        addMultiplier(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, damageBonus * strength);
        addFlat(entity.getAttribute(Attributes.ARMOR), ARMOR_ID, armorBonus * strength);
        // Heavier enemies should not be trivially chain-knocked away from you.
        addFlat(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, Math.min(0.8, 0.15 * (ordinal() + 1)));
        entity.setHealth(entity.getMaxHealth());

        if (entity instanceof Mob mob) {
            equip(mob);
        }
    }

    public static void removeFrom(LivingEntity entity) {
        removeModifier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID);
        removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID);
        removeModifier(entity.getAttribute(Attributes.ARMOR), ARMOR_ID);
        removeModifier(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID);
        entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
    }

    /** Real armour, so the rank is visible before the first hit lands. */
    private void equip(Mob mob) {
        if (!WastelandConfig.ELITE_GEAR.get()) {
            return;
        }
        boolean diamond = ordinal() >= VETERAN.ordinal();
        putIfEmpty(mob, EquipmentSlot.HEAD, diamond ? Items.DIAMOND_HELMET : Items.IRON_HELMET);
        putIfEmpty(mob, EquipmentSlot.CHEST, diamond ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE);
        if (ordinal() >= HARDENED.ordinal()) {
            putIfEmpty(mob, EquipmentSlot.LEGS, diamond ? Items.DIAMOND_LEGGINGS : Items.IRON_LEGGINGS);
            putIfEmpty(mob, EquipmentSlot.FEET, diamond ? Items.DIAMOND_BOOTS : Items.IRON_BOOTS);
        }
    }

    /** Never overwrite what the mob already carries - that is how skeletons lose their bows. */
    private static void putIfEmpty(Mob mob, EquipmentSlot slot, Item item) {
        if (mob.getItemBySlot(slot).isEmpty()) {
            mob.setItemSlot(slot, new ItemStack(item));
            mob.setDropChance(slot, 0.15F);
        }
    }

    private static void addMultiplier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null || amount == 0.0 || attribute.getModifier(id) != null) {
            return;
        }
        attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void addFlat(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null || amount == 0.0 || attribute.getModifier(id) != null) {
            return;
        }
        attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
        if (attribute != null && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }
}
