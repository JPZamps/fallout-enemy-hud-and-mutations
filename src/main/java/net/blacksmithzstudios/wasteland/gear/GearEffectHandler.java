package net.blacksmithzstudios.wasteland.gear;

import com.mojang.logging.LogUtils;
import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Runs the legendary gear effects. Everything is driven off the NBT tag on the stack, so it
 * works the same whether the holder is a legendary mob or the player who looted it from one.
 */
@Mod.EventBusSubscriber(modid = WastelandMod.MOD_ID)
public final class GearEffectHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Worn effects are refreshed on a slow tick; the instances they grant outlast the gap. */
    private static final int WORN_INTERVAL = 20;

    private GearEffectHandler() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || !WastelandConfig.GEAR_LEGENDS_ENABLED.get()) {
            return;
        }

        // Forge's event bus logs listener failures through log4j, which in this dev environment
        // throws a LinkageError of its own and destroys the original stack trace. Catching here
        // means a bug in these effects is reported instead of vanishing behind that.
        try {
            applyEffects(event);
        } catch (Throwable failure) {
            LOGGER.error("Legendary gear effect failed on {}", event.getEntity().getType(), failure);
        }
    }

    private static void applyEffects(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        float amount = event.getAmount();

        // An explosion must never set off another explosion, and reflected damage must never
        // be reflected again: either one recurses without a bound.
        boolean fromExplosion = event.getSource().is(DamageTypeTags.IS_EXPLOSION);
        boolean fromThorns = event.getSource().is(DamageTypes.THORNS);
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity source ? source : null;

        // Attacker's weapon fires first, then the victim's armour answers.
        if (attacker != null) {
            WeaponLegend weapon = GearLegends.weaponOf(attacker.getMainHandItem());
            if (weapon != null) {
                amount = weapon.onHit(attacker, victim, amount, !fromExplosion);
            }
        }

        for (ItemStack piece : victim.getArmorSlots()) {
            ArmorLegend armor = GearLegends.armorOf(piece);
            if (armor != null) {
                amount = armor.onHurt(victim, attacker, amount, !fromThorns);
            }
        }

        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!WastelandConfig.GEAR_LEGENDS_ENABLED.get()) {
            return;
        }
        float multiplier = 1.0F;
        for (ItemStack piece : event.getEntity().getArmorSlots()) {
            ArmorLegend armor = GearLegends.armorOf(piece);
            if (armor != null) {
                multiplier = Math.min(multiplier, armor.fallMultiplier());
            }
        }
        if (multiplier < 1.0F) {
            event.setDamageMultiplier(event.getDamageMultiplier() * multiplier);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()
                || entity.tickCount % WORN_INTERVAL != 0
                || !WastelandConfig.GEAR_LEGENDS_ENABLED.get()) {
            return;
        }
        for (ItemStack piece : entity.getArmorSlots()) {
            ArmorLegend armor = GearLegends.armorOf(piece);
            if (armor != null) {
                armor.onWornTick(entity);
            }
        }
    }
}
