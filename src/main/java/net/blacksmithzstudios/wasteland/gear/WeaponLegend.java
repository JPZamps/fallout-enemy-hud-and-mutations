package net.blacksmithzstudios.wasteland.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Fallout's legendary weapon effects, rebuilt from vanilla mechanics.
 *
 * Each one fires when the holder lands a hit. Nothing here needs a new item or texture: the
 * effect rides on an ordinary vanilla weapon as an NBT tag, and the name says which roll it is.
 */
public enum WeaponLegend {

    EXPLOSIVE("Explosive", ChatFormatting.GOLD),
    FREEZING("Freezing", ChatFormatting.AQUA),
    WOUNDING("Wounding", ChatFormatting.DARK_GREEN),
    VAMPIRE("Vampire's", ChatFormatting.DARK_RED),
    CRIPPLING("Crippling", ChatFormatting.GRAY),
    STAGGERING("Staggering", ChatFormatting.YELLOW),
    PLAGUED("Plagued", ChatFormatting.DARK_PURPLE),
    EXECUTIONER("Executioner's", ChatFormatting.RED);

    private final String displayName;
    private final ChatFormatting color;

    WeaponLegend(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public ChatFormatting color() {
        return color;
    }

    public static WeaponLegend roll(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    public static WeaponLegend byName(String name) {
        for (WeaponLegend legend : values()) {
            if (legend.name().equals(name)) {
                return legend;
            }
        }
        return null;
    }

    /**
     * Applies the effect of a landed hit.
     *
     * @return the damage to deal instead of the incoming amount
     */
    public float onHit(LivingEntity attacker, LivingEntity victim, float amount, boolean allowExplosion) {
        Level level = victim.level();

        switch (this) {
            case EXPLOSIVE -> {
                // Damage-only blast, so the weapon never grieves the terrain. Never chained
                // off damage an explosion already caused.
                if (allowExplosion) {
                    level.explode(attacker, victim.getX(), victim.getY(), victim.getZ(),
                            1.5F, Level.ExplosionInteraction.NONE);
                }
            }

            case FREEZING -> {
                // Vanilla powder-snow freezing, reused as a weapon effect.
                victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 60, victim.getTicksFrozen() + 140));
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            }

            case WOUNDING -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));

            case PLAGUED -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));

            case VAMPIRE -> attacker.heal(amount * 0.25F);

            case CRIPPLING -> victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 1));

            case STAGGERING -> {
                // Shoved directly away from the attacker. Done by hand because knockback()
                // changed shape in 26.2 and now wants a damage source of its own.
                double dx = victim.getX() - attacker.getX();
                double dz = victim.getZ() - attacker.getZ();
                double length = Math.sqrt(dx * dx + dz * dz);
                if (length > 1.0E-4) {
                    victim.push(dx / length * 0.8, 0.2, dz / length * 0.8);
                }
            }

            case EXECUTIONER -> {
                // Hits harder the closer the target is to dying.
                if (victim.getHealth() <= victim.getMaxHealth() * 0.4F) {
                    return amount * 1.6F;
                }
            }
        }
        return amount;
    }
}
