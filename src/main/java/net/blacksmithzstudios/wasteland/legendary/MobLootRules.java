package net.blacksmithzstudios.wasteland.legendary;

import net.minecraft.core.registries.BuiltInRegistries;
import net.blacksmithzstudios.wasteland.WastelandConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-mob drop rules written as plain config lines, so a pack author can say
 * "a legendary creeper drops a nether star five percent of the time" without touching code.
 *
 * <pre>mobId ; itemId ; chance ; minCount-maxCount</pre>
 *
 * A malformed line is skipped rather than crashing the game - config is hand-edited, and a
 * typo in one rule should never cost someone their world.
 */
public final class MobLootRules {

    private MobLootRules() {
    }

    private record Rule(String mobId, Item item, double chance, int min, int max) {
    }

    /**
     * Rules are re-parsed only when the config list itself changes. ModConfigSpec hands back
     * the same list instance until a reload, so an identity check is enough - and it keeps a
     * string parse out of every single legendary death.
     */
    private static List<? extends String> parsedFrom;
    private static List<Rule> parsed = List.of();

    private static synchronized List<Rule> rules() {
        List<? extends String> configured = WastelandConfig.MOB_LOOT_RULES.get();
        if (configured != parsedFrom) {
            List<Rule> built = new ArrayList<>();
            for (String line : configured) {
                Rule rule = parse(line);
                if (rule != null) {
                    built.add(rule);
                }
            }
            parsed = List.copyOf(built);
            parsedFrom = configured;
        }
        return parsed;
    }

    public static List<ItemStack> rollFor(LivingEntity entity, RandomSource random, boolean mutated) {
        List<ItemStack> drops = new ArrayList<>();
        String mobId = idOf(entity);

        for (Rule rule : rules()) {
            if (!rule.mobId.equals("*") && !rule.mobId.equals(mobId)) {
                continue;
            }

            // Surviving to mutate doubles the odds, the same way it doubles everything else.
            double chance = mutated ? Math.min(1.0, rule.chance * 2.0) : rule.chance;
            if (random.nextDouble() >= chance) {
                continue;
            }

            int count = rule.min + (rule.max > rule.min ? random.nextInt(rule.max - rule.min + 1) : 0);
            if (count > 0) {
                drops.add(new ItemStack(rule.item, count));
            }
        }
        return drops;
    }

    private static Rule parse(String line) {
        String[] parts = line.split(";");
        if (parts.length < 3) {
            return null;
        }

        String mobId = parts[0].trim();
        Identifier itemKey = Identifier.tryParse(parts[1].trim());
        Item item = itemKey == null ? null : BuiltInRegistries.ITEM.get(itemKey);
        if (item == null || item == Items.AIR) {
            return null; // an item no installed mod provides
        }

        double chance;
        try {
            chance = Double.parseDouble(parts[2].trim());
        } catch (NumberFormatException malformed) {
            return null;
        }

        int min = 1;
        int max = 1;
        if (parts.length >= 4) {
            String[] range = parts[3].trim().split("-");
            try {
                min = Integer.parseInt(range[0].trim());
                max = range.length > 1 ? Integer.parseInt(range[1].trim()) : min;
            } catch (NumberFormatException malformed) {
                return null;
            }
        }
        if (min < 0 || max < min) {
            return null;
        }

        return new Rule(mobId, item, chance, min, max);
    }

    private static String idOf(LivingEntity entity) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null ? key.toString() : "";
    }
}
