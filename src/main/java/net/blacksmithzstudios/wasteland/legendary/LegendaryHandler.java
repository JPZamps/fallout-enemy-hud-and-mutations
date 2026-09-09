package net.blacksmithzstudios.wasteland.legendary;

import com.mojang.logging.LogUtils;
import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.blacksmithzstudios.wasteland.gear.ArmorLegend;
import net.blacksmithzstudios.wasteland.gear.GearLegends;
import net.blacksmithzstudios.wasteland.gear.WeaponLegend;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber(modid = WastelandMod.MOD_ID)
public class LegendaryHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Roll every hostile mob once, the moment it enters the world. */
    @SubscribeEvent
    public static void onSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity mob)) {
            return;
        }
        // Eligibility first: reading persistent data creates a tag on every entity that asks,
        // and most entities joining a world are never candidates.
        if (!MobEligibility.canBeLegendary(mob)) {
            return;
        }
        if (LegendaryData.wasRolled(mob)) {
            return; // already handled, e.g. loaded from disk
        }
        LegendaryData.markRolled(mob);

        double difficulty = difficultyScale(mob);

        // Elites and legendaries roll independently; a mob can end up both.
        if (WastelandConfig.ELITE_ENABLED.get()
                && mob.getRandom().nextDouble() < WastelandConfig.ELITE_CHANCE.get() * difficulty) {
            makeElite(mob, EliteRank.roll(mob.getRandom()));
        }

        if (WastelandConfig.LEGENDARY_ENABLED.get()
                && mob.getRandom().nextDouble() < WastelandConfig.LEGENDARY_CHANCE.get() * difficulty) {
            makeLegendary(mob, LegendaryPrefix.roll(mob.getRandom()));
        }
    }

    /** Promotes an entity to legendary. Shared by natural spawns and the /wasteland command. */
    public static void makeLegendary(LivingEntity entity, LegendaryPrefix prefix) {
        LegendaryData.markRolled(entity);
        LegendaryData.setPrefix(entity, prefix);
        prefix.applyTo(entity, WastelandConfig.LEGENDARY_STRENGTH.get() * difficultyScale(entity));

        rename(entity);
        if (WastelandConfig.GLOWING_LEGENDARIES.get()) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            armWithLegendaryGear(mob);
        }
    }

    /**
     * Gives the mob gear carrying a Fallout-style legendary effect, which it fights with and
     * always drops.
     *
     * Where the mob already holds a weapon the roll is stamped onto that weapon rather than
     * replacing it: handing a skeleton a sword would leave it with a bow-shaped AI and nothing
     * to shoot. Only an empty hand gets a new item.
     */
    private static void armWithLegendaryGear(Mob mob) {
        if (!WastelandConfig.GEAR_LEGENDS_ENABLED.get()) {
            return;
        }
        RandomSource random = mob.getRandom();

        if (random.nextDouble() < WastelandConfig.LEGEND_WEAPON_CHANCE.get()) {
            ItemStack held = mob.getMainHandItem();
            ItemStack weapon = held.isEmpty() ? new ItemStack(Items.IRON_SWORD) : held;
            GearLegends.applyWeapon(weapon, WeaponLegend.roll(random));
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            mob.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        }

        if (random.nextDouble() < WastelandConfig.LEGEND_ARMOR_CHANCE.get()) {
            EquipmentSlot slot = ARMOR_SLOTS[random.nextInt(ARMOR_SLOTS.length)];
            ItemStack worn = mob.getItemBySlot(slot);
            ItemStack piece = worn.isEmpty() ? new ItemStack(defaultArmor(slot)) : worn;
            GearLegends.applyArmor(piece, ArmorLegend.roll(random));
            mob.setItemSlot(slot, piece);
            mob.setDropChance(slot, 1.0F);
        }
    }

    /** Promotes an entity to an elite: a much tougher version of the same mob. */
    public static void makeElite(LivingEntity entity, EliteRank rank) {
        LegendaryData.markRolled(entity);
        LegendaryData.setElite(entity, rank);
        rank.applyTo(entity, WastelandConfig.ELITE_STRENGTH.get() * difficultyScale(entity));

        rename(entity);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
    }

    /** Rebuilds the nameplate from whatever the entity currently is. */
    public static void rename(LivingEntity entity) {
        entity.setCustomName(Component.literal(LegendaryData.displayNameOf(entity))
                .withStyle(LegendaryData.colorOf(entity)));
        entity.setCustomNameVisible(false); // shows on look, like a Fallout target readout
    }

    /** Harder worlds make more of them, and make them hit harder. */
    public static double difficultyScale(LivingEntity entity) {
        return switch (entity.level().getDifficulty()) {
            case PEACEFUL, EASY -> WastelandConfig.EASY_SCALE.get();
            case NORMAL -> WastelandConfig.NORMAL_SCALE.get();
            case HARD -> WastelandConfig.HARD_SCALE.get();
        };
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static Item defaultArmor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Items.IRON_HELMET;
            case CHEST -> Items.IRON_CHESTPLATE;
            case LEGS -> Items.IRON_LEGGINGS;
            default -> Items.IRON_BOOTS;
        };
    }

    /** Mutation: dropped below half health, a legendary heals to full and powers up. Once. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        try {
            handleHurt(event);
        } catch (Throwable failure) {
            LOGGER.error("Mutation handling failed on {}", event.getEntity().getType(), failure);
        }
    }

    private static void handleHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            applyVampiricLifesteal(attacker, event.getAmount());
        }

        if (victim.level().isClientSide() || !WastelandConfig.MUTATION_ENABLED.get()) {
            return;
        }
        if (!LegendaryData.isLegendary(victim) || LegendaryData.mutationRolled(victim)) {
            return;
        }
        if (WastelandConfig.MUTATE_ONLY_WHEN_ENGAGED.get() && !isEngagedWithPlayer(victim, event)) {
            return;
        }
        if (victim.getHealth() - event.getAmount() > victim.getMaxHealth() * 0.5F) {
            return;
        }

        // Spend the single roll here, win or lose, so a mob that failed never rolls again.
        LegendaryData.markMutationRolled(victim);
        if (victim.getRandom().nextDouble() >= WastelandConfig.MUTATION_CHANCE.get()) {
            return;
        }

        event.setAmount(0.0F);
        mutate(victim);
    }

    /** The Fallout mutation: full heal plus a power spike, once per lifetime. */
    public static void mutate(LivingEntity victim) {
        LegendaryData.markMutated(victim);
        LegendaryData.markMutationRolled(victim);
        if (LegendaryData.prefixOf(victim) != null) {
            rename(victim);
        }
        victim.setHealth(victim.getMaxHealth());
        victim.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 1));
        victim.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 0));
        victim.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));

        Level level = victim.level();
        level.playSound(null, victim.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    40, 0.5, 0.7, 0.5, 0.05);
        }
    }

    /**
     * A legendary may only turn while a player is genuinely in the fight: the player either
     * struck the blow or is being hunted, and is close enough to witness it either way.
     */
    private static boolean isEngagedWithPlayer(LivingEntity victim, LivingIncomingDamageEvent event) {
        double range = WastelandConfig.MUTATION_RANGE.get();

        if (event.getSource().getEntity() instanceof Player striker) {
            return striker.distanceToSqr(victim) <= range * range;
        }
        if (victim instanceof Mob mob && mob.getTarget() instanceof Player hunted) {
            return hunted.distanceToSqr(victim) <= range * range;
        }
        return false;
    }

    private static void applyVampiricLifesteal(LivingEntity attacker, float amount) {
        if (attacker.level().isClientSide() || LegendaryData.prefixOf(attacker) != LegendaryPrefix.VAMPIRIC) {
            return;
        }
        attacker.heal(amount * 0.5F);
        PrefixSignatures.showLifesteal(attacker);
    }

    /**
     * A tougher enemy is worth more experience. Without this an elite that took five times
     * as long to kill awards exactly what an ordinary one would.
     */
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!WastelandConfig.BONUS_EXPERIENCE.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();

        float multiplier = 1.0F;
        EliteRank elite = LegendaryData.eliteOf(entity);
        if (elite != null) {
            multiplier *= elite.experienceMultiplier();
        }
        if (LegendaryData.prefixOf(entity) != null) {
            multiplier *= LegendaryData.hasMutated(entity) ? 3.0F : 2.0F;
        }

        if (multiplier > 1.0F) {
            event.setDroppedExperience(Math.round(event.getDroppedExperience() * multiplier));
        }
    }

    /** The Explosive roll's parting gift. Loot is handled by the loot modifier. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        LegendaryPrefix prefix = LegendaryData.prefixOf(entity);
        if (prefix == null) {
            return;
        }

        if (prefix == LegendaryPrefix.EXPLOSIVE
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            // Damage-only blast: no terrain grief.
            entity.level().explode(entity, entity.getX(), entity.getY(), entity.getZ(),
                    3.0F, Level.ExplosionInteraction.NONE);
        }

        // Loot itself is added by LegendaryLootModifier, through the mob's own loot table.
    }
}
