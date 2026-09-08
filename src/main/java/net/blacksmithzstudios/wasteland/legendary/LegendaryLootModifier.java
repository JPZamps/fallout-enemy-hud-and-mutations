package net.blacksmithzstudios.wasteland.legendary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Adds legendary rewards through the loot table rather than by spawning items on death.
 *
 * Going through the table is what makes the mod cooperate with everything else installed:
 * Looting, other mods' loot modifiers and datapack overrides all see these drops, and the
 * doubling below applies to whatever a modded mob normally drops, not just to our own items.
 */
public class LegendaryLootModifier extends LootModifier {

    public static final Codec<LegendaryLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> LootModifier.codecStart(instance).apply(instance, LegendaryLootModifier::new));

    public LegendaryLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!WastelandConfig.LOOT_ENABLED.get()) {
            return loot;
        }

        Entity killed = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(killed instanceof LivingEntity entity)) {
            return loot;
        }

        LegendaryPrefix prefix = LegendaryData.prefixOf(entity);
        EliteRank elite = LegendaryData.eliteOf(entity);
        if (prefix == null && elite == null) {
            return loot;
        }

        boolean mutated = prefix != null && LegendaryData.hasMutated(entity);

        // How many times the mob's own table is repeated. An elite that took five times as
        // long to kill should not pay out like an ordinary mob, and a mutation doubles again.
        // By this point that table has already been rolled into `loot`, modded drops included.
        int copies = 1;
        if (elite != null && WastelandConfig.ELITE_LOOT.get()) {
            copies = elite.lootCopies();
        }
        if (mutated && WastelandConfig.DOUBLE_VANILLA_LOOT.get()) {
            copies *= 2;
        }
        if (copies > 1) {
            ObjectArrayList<ItemStack> original = new ObjectArrayList<>(loot);
            for (int i = 1; i < copies; i++) {
                for (ItemStack stack : original) {
                    loot.add(stack.copy());
                }
            }
        }

        if (prefix != null) {
            loot.addAll(LegendaryLoot.buildDrops(context.getRandom(), prefix,
                    WastelandConfig.BONUS_LOOT_ROLLS.get(), mutated));
        }

        // An elite that is not also legendary still deserves supplies, and the upper ranks
        // deserve a signature piece of their own.
        if (elite != null && WastelandConfig.ELITE_LOOT.get()) {
            loot.addAll(LegendaryLoot.buildEliteDrops(context.getRandom(), elite,
                    prefix == null && elite.dropsSignatureGear()));
        }

        loot.addAll(MobLootRules.rollFor(entity, context.getRandom(), mutated));
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
