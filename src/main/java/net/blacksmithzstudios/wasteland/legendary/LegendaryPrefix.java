package net.blacksmithzstudios.wasteland.legendary;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.RandomSource;

import java.util.UUID;

/**
 * The legendary rolls. Every effect is expressed through vanilla attributes or
 * vanilla behaviour, so nothing here needs a texture, a model or a new item.
 */
public enum LegendaryPrefix {

    MUTATED   ("Mutated",    ChatFormatting.GREEN,       1.5,  0.0,  0.0,  0.0, ParticleTypes.HAPPY_VILLAGER),
    SAVAGE    ("Savage",     ChatFormatting.RED,         0.0,  1.2,  0.0,  0.0, ParticleTypes.CRIT),
    QUICK     ("Quick",      ChatFormatting.AQUA,        0.0,  0.0,  0.35, 0.0, ParticleTypes.CLOUD),
    STURDY    ("Sturdy",     ChatFormatting.GRAY,        0.4,  0.0,  0.0, 12.0, ParticleTypes.ELECTRIC_SPARK),
    BLOODIED  ("Bloodied",   ChatFormatting.DARK_RED,   -0.5,  2.0,  0.1,  0.0, ParticleTypes.DAMAGE_INDICATOR),
    VAMPIRIC  ("Vampiric",   ChatFormatting.DARK_PURPLE, 0.3,  0.6,  0.0,  0.0, ParticleTypes.SOUL),
    EXPLOSIVE ("Explosive",  ChatFormatting.GOLD,        0.0,  0.4,  0.0,  0.0, ParticleTypes.SMOKE),
    RELENTLESS("Relentless", ChatFormatting.LIGHT_PURPLE,0.8,  0.5,  0.15, 4.0, ParticleTypes.END_ROD);

    /** Fixed UUIDs so the modifiers are idempotent across a save/load cycle. */
    private static final UUID HEALTH_ID = UUID.fromString("6f3a1d0e-8f21-4a55-9b1c-2f1d7c1a0001");
    private static final UUID DAMAGE_ID = UUID.fromString("6f3a1d0e-8f21-4a55-9b1c-2f1d7c1a0002");
    private static final UUID SPEED_ID  = UUID.fromString("6f3a1d0e-8f21-4a55-9b1c-2f1d7c1a0003");
    private static final UUID ARMOR_ID  = UUID.fromString("6f3a1d0e-8f21-4a55-9b1c-2f1d7c1a0004");

    private final String displayName;
    private final ChatFormatting color;
    private final double healthBonus;
    private final double damageBonus;
    private final double speedBonus;
    private final double armorBonus;
    private final SimpleParticleType signature;

    LegendaryPrefix(String displayName, ChatFormatting color,
                    double healthBonus, double damageBonus, double speedBonus, double armorBonus,
                    SimpleParticleType signature) {
        this.displayName = displayName;
        this.color = color;
        this.healthBonus = healthBonus;
        this.damageBonus = damageBonus;
        this.speedBonus = speedBonus;
        this.armorBonus = armorBonus;
        this.signature = signature;
    }

    /** The ambient particle that tells this roll apart in a fight, without any UI. */
    public SimpleParticleType signature() {
        return signature;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    public static LegendaryPrefix roll(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    public static LegendaryPrefix byName(String name) {
        for (LegendaryPrefix prefix : values()) {
            if (prefix.name().equals(name)) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * @param strength scales every bonus, so one config value and the world difficulty can
     *                 make legendaries as dangerous as the player wants without retuning each roll
     */
    public void applyTo(LivingEntity entity, double strength) {
        addMultiplier(entity.getAttribute(Attributes.MAX_HEALTH),      HEALTH_ID, "wasteland_health", healthBonus * strength);
        addMultiplier(entity.getAttribute(Attributes.ATTACK_DAMAGE),   DAMAGE_ID, "wasteland_damage", damageBonus * strength);
        addMultiplier(entity.getAttribute(Attributes.MOVEMENT_SPEED),  SPEED_ID,  "wasteland_speed",  speedBonus);
        addFlat      (entity.getAttribute(Attributes.ARMOR),           ARMOR_ID,  "wasteland_armor",  armorBonus * strength);
        entity.setHealth(entity.getMaxHealth());
    }

    /** Strips every modifier this class applies, so a cleared mob is genuinely ordinary again. */
    public static void removeFrom(LivingEntity entity) {
        removeModifier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID);
        removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID);
        removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
        removeModifier(entity.getAttribute(Attributes.ARMOR), ARMOR_ID);
        entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
    }

    private static void removeModifier(AttributeInstance attribute, UUID id) {
        if (attribute != null && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    private static void addMultiplier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null || amount == 0.0 || attribute.getModifier(id) != null) {
            return;
        }
        attribute.addPermanentModifier(
                new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void addFlat(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null || amount == 0.0 || attribute.getModifier(id) != null) {
            return;
        }
        attribute.addPermanentModifier(
                new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
}
