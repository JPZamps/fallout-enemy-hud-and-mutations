package net.blacksmithzstudios.wasteland.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fallout's legendary armour effects. Where the weapon rolls fire on a hit landed, these
 * fire on a hit taken, or tick quietly while the piece is worn.
 */
public enum ArmorLegend {

    SENTINEL("Sentinel's", ChatFormatting.BLUE),
    UNYIELDING("Unyielding", ChatFormatting.RED),
    CHAMELEON("Chameleon", ChatFormatting.GREEN),
    CUSHIONED("Cushioned", ChatFormatting.AQUA),
    POWERED("Powered", ChatFormatting.YELLOW),
    THORNED("Thorned", ChatFormatting.DARK_GRAY);

    private final String displayName;
    private final ChatFormatting color;

    ArmorLegend(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    public static ArmorLegend roll(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    public static ArmorLegend byName(String name) {
        for (ArmorLegend legend : values()) {
            if (legend.name().equals(name)) {
                return legend;
            }
        }
        return null;
    }

    /**
     * Applies the effect of a hit taken.
     *
     * @return the damage to take instead of the incoming amount
     */
    public float onHurt(LivingEntity wearer, LivingEntity attacker, float amount, boolean reflectable) {
        switch (this) {
            case SENTINEL -> {
                // Rewards holding ground, exactly as it does in Fallout.
                if (wearer.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                    return amount * 0.7F;
                }
            }
            case THORNED -> {
                // Only reflect a direct blow. Reflecting reflected damage puts two thorned
                // wearers into an unbounded ping-pong inside a single hurt event.
                if (attacker != null && reflectable) {
                    attacker.hurt(wearer.damageSources().thorns(wearer), amount * 0.25F);
                }
            }
            default -> {
            }
        }
        return amount;
    }

    /** Applies the always-on effects. Called on a slow tick, not every frame. */
    public void onWornTick(LivingEntity wearer) {
        switch (this) {
            case UNYIELDING -> {
                // Stronger the worse things are going.
                if (wearer.getHealth() <= wearer.getMaxHealth() * 0.3F) {
                    wearer.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 1, true, false));
                }
            }
            case CHAMELEON -> {
                if (wearer.isCrouching() && wearer.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                    wearer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false));
                }
            }
            case POWERED -> wearer.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
            default -> {
            }
        }
    }

    /** Cushioned softens falls; everything else leaves them alone. */
    public float fallMultiplier() {
        return this == CUSHIONED ? 0.4F : 1.0F;
    }
}
