package net.blacksmithzstudios.wasteland;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Grouped into sections so the generated TOML stays readable: mutation rules, loot tables,
 * which mobs take part, and the HUD's appearance.
 */
public class WastelandConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- mutation ----
    public static final ModConfigSpec.BooleanValue LEGENDARY_ENABLED;
    public static final ModConfigSpec.DoubleValue LEGENDARY_CHANCE;
    public static final ModConfigSpec.BooleanValue MUTATION_ENABLED;
    public static final ModConfigSpec.DoubleValue MUTATION_CHANCE;
    public static final ModConfigSpec.BooleanValue MUTATE_ONLY_WHEN_ENGAGED;
    public static final ModConfigSpec.DoubleValue MUTATION_RANGE;
    public static final ModConfigSpec.BooleanValue GLOWING_LEGENDARIES;
    public static final ModConfigSpec.BooleanValue PREFIX_SIGNATURES;
    public static final ModConfigSpec.DoubleValue LEGENDARY_STRENGTH;
    public static final ModConfigSpec.DoubleValue EASY_SCALE;
    public static final ModConfigSpec.DoubleValue NORMAL_SCALE;
    public static final ModConfigSpec.DoubleValue HARD_SCALE;

    // ---- elite ----
    public static final ModConfigSpec.BooleanValue ELITE_ENABLED;
    public static final ModConfigSpec.DoubleValue ELITE_CHANCE;
    public static final ModConfigSpec.DoubleValue ELITE_STRENGTH;
    public static final ModConfigSpec.BooleanValue ELITE_GEAR;

    // ---- loot ----
    public static final ModConfigSpec.BooleanValue LOOT_ENABLED;
    public static final ModConfigSpec.IntValue BONUS_LOOT_ROLLS;
    public static final ModConfigSpec.BooleanValue OVERCHARGED_ENCHANTMENTS;
    public static final ModConfigSpec.BooleanValue DOUBLE_VANILLA_LOOT;
    public static final ModConfigSpec.BooleanValue ELITE_LOOT;
    public static final ModConfigSpec.BooleanValue BONUS_EXPERIENCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GEAR_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SUPPLY_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_LOOT_RULES;

    // ---- gear ----
    public static final ModConfigSpec.BooleanValue GEAR_LEGENDS_ENABLED;
    public static final ModConfigSpec.DoubleValue LEGEND_WEAPON_CHANCE;
    public static final ModConfigSpec.DoubleValue LEGEND_ARMOR_CHANCE;

    // ---- mobs ----
    public static final ModConfigSpec.BooleanValue INCLUDE_BOSSES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_MOBS;

    // ---- detection (server side) ----
    public static final ModConfigSpec.BooleanValue DETECTION_ENABLED;
    public static final ModConfigSpec.DoubleValue DETECTION_RANGE;
    public static final ModConfigSpec.DoubleValue DANGER_RANGE;

    static {
        BUILDER.comment("When mobs become legendary and when they mutate.").push("mutation");
        LEGENDARY_ENABLED = BUILDER
                .comment("Master switch for legendary mobs: prefixes, their gear and their loot.",
                         "Turning this off leaves elites and the HUD untouched.")
                .define("legendaryEnabled", true);
        LEGENDARY_CHANCE = BUILDER
                .comment("Chance for an eligible mob to spawn legendary.")
                .defineInRange("legendaryChance", 0.04, 0.0, 1.0);
        MUTATION_ENABLED = BUILDER
                .comment("Master switch for the mutation mechanic.")
                .define("mutationEnabled", true);
        MUTATION_CHANCE = BUILDER
                .comment("Chance a legendary actually mutates when it first drops below half health.",
                         "Rolled once per mob: a failed roll means that one never mutates.")
                .defineInRange("mutationChance", 0.5, 0.0, 1.0);
        MUTATE_ONLY_WHEN_ENGAGED = BUILDER
                .comment("Only mutate if a player hurt the mob or it is hunting one.")
                .define("mutateOnlyWhenEngaged", true);
        MUTATION_RANGE = BUILDER
                .comment("How close the player must be, in blocks, for a mutation to trigger.")
                .defineInRange("mutationRange", 24.0, 1.0, 128.0);
        GLOWING_LEGENDARIES = BUILDER
                .comment("Give legendaries a permanent glow. Off by default: it reads as clutter.")
                .define("glowingLegendaries", false);
        PREFIX_SIGNATURES = BUILDER
                .comment("Emit one vanilla particle per legendary prefix, so the rolls",
                         "can be told apart in a fight.")
                .define("prefixSignatures", true);
        LEGENDARY_STRENGTH = BUILDER
                .comment("Scales every legendary stat bonus. 1.0 is the base tuning,",
                         "2.0 makes legendaries twice as far above a normal mob, 5.0 is brutal.")
                .defineInRange("legendaryStrength", 2.0, 0.1, 20.0);
        EASY_SCALE = BUILDER
                .comment("Stat and spawn-chance scaling per world difficulty.")
                .defineInRange("easyScale", 0.6, 0.0, 10.0);
        NORMAL_SCALE = BUILDER.defineInRange("normalScale", 1.0, 0.0, 10.0);
        HARD_SCALE = BUILDER.defineInRange("hardScale", 1.75, 0.0, 10.0);
        BUILDER.pop();

        BUILDER.comment("Higher-level enemies: much tougher, wearing real gear, marked with a skull.",
                        "A separate axis from the legendary prefixes - elites do not mutate on their own.")
                .push("elite");
        ELITE_ENABLED = BUILDER.define("eliteEnabled", true);
        ELITE_CHANCE = BUILDER
                .comment("Chance for an eligible mob to spawn as an elite. Scaled by difficulty.")
                .defineInRange("eliteChance", 0.08, 0.0, 1.0);
        ELITE_STRENGTH = BUILDER
                .comment("Scales every elite stat bonus, the same way legendaryStrength does.")
                .defineInRange("eliteStrength", 1.0, 0.1, 20.0);
        ELITE_GEAR = BUILDER
                .comment("Give elites armour matching their rank. Never replaces gear they already carry.")
                .define("eliteGear", true);
        BUILDER.pop();

        BUILDER.comment("What legendary kills drop. Items are registry ids, so other mods work here.")
                .push("loot");
        LOOT_ENABLED = BUILDER
                .comment("Drop bonus loot on a legendary kill at all.")
                .define("lootEnabled", true);
        BONUS_LOOT_ROLLS = BUILDER
                .comment("Supply rolls per legendary kill. Mutated mobs get double.")
                .defineInRange("bonusLootRolls", 3, 1, 20);
        DOUBLE_VANILLA_LOOT = BUILDER
                .comment("A mutated kill also doubles everything the mob normally drops.",
                         "Turn this off when playing with mods whose mobs drop unique items,",
                         "or the doubling will duplicate them.")
                .define("doubleVanillaLoot", true);
        ELITE_LOOT = BUILDER
                .comment("Elites multiply their own drops and add supplies, scaled by rank.",
                         "Without this an elite is only harder, never more rewarding.")
                .define("eliteLoot", true);
        BONUS_EXPERIENCE = BUILDER
                .comment("Legendary and elite kills award more experience, scaled the same way.")
                .define("bonusExperience", true);
        OVERCHARGED_ENCHANTMENTS = BUILDER
                .comment("Let drops carry enchantments past their vanilla maximum,",
                         "such as Protection VI or Sharpness VII.")
                .define("overchargedEnchantments", true);
        GEAR_ITEMS = BUILDER
                .comment("Signature gear pool: one of these is dropped, heavily enchanted.")
                .defineList("gearItems", List.of(
                        "minecraft:wooden_sword", "minecraft:stone_sword",
                        "minecraft:golden_sword", "minecraft:iron_sword", "minecraft:diamond_sword",
                        "minecraft:stone_axe", "minecraft:golden_axe",
                        "minecraft:iron_axe", "minecraft:diamond_axe",
                        "minecraft:bow", "minecraft:crossbow", "minecraft:trident", "minecraft:shield",
                        "minecraft:mace",
                        "minecraft:leather_helmet", "minecraft:leather_chestplate",
                        "minecraft:leather_leggings", "minecraft:leather_boots",
                        "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate",
                        "minecraft:chainmail_leggings", "minecraft:chainmail_boots",
                        "minecraft:golden_helmet", "minecraft:golden_chestplate",
                        "minecraft:golden_leggings", "minecraft:golden_boots",
                        "minecraft:iron_helmet", "minecraft:iron_chestplate",
                        "minecraft:iron_leggings", "minecraft:iron_boots",
                        "minecraft:diamond_helmet", "minecraft:diamond_chestplate",
                        "minecraft:diamond_leggings", "minecraft:diamond_boots",
                        "minecraft:turtle_helmet"
                ), entry -> entry instanceof String);
        SUPPLY_ITEMS = BUILDER
                .comment("Supply pool: consumables and currency rolled alongside the gear.")
                .defineList("supplyItems", List.of(
                        "minecraft:bread", "minecraft:cooked_beef", "minecraft:cooked_porkchop",
                        "minecraft:cooked_chicken", "minecraft:cooked_mutton", "minecraft:cooked_cod",
                        "minecraft:baked_potato", "minecraft:pumpkin_pie", "minecraft:sweet_berries",
                        "minecraft:honey_bottle", "minecraft:golden_carrot", "minecraft:golden_apple",
                        "minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot",
                        "minecraft:iron_ingot", "minecraft:copper_ingot", "minecraft:lapis_lazuli",
                        "minecraft:redstone", "minecraft:amethyst_shard", "minecraft:glowstone_dust",
                        "minecraft:blaze_powder", "minecraft:gunpowder", "minecraft:string",
                        "minecraft:leather", "minecraft:arrow", "minecraft:ender_pearl",
                        "minecraft:experience_bottle", "minecraft:torch",
                        "minecraft:wind_charge", "minecraft:breeze_rod",
                        "minecraft:ominous_bottle", "minecraft:trial_key",
                        "minecraft:armadillo_scute", "minecraft:wolf_armor"
                ), entry -> entry instanceof String);
        MOB_LOOT_RULES = BUILDER
                .comment("Per-mob drops for legendary kills, one rule per line.",
                         "Format: mobId ; itemId ; chance ; minCount-maxCount",
                         "Use * as the mob id to match every legendary.",
                         "Example: minecraft:creeper ; minecraft:nether_star ; 0.05 ; 1-1",
                         "Chance is 0.0 to 1.0, and doubles for a mutated kill.")
                .defineList("mobLootRules", List.of(
                        "minecraft:wither ; minecraft:nether_star ; 1.0 ; 3-4",
                        "minecraft:ender_dragon ; minecraft:end_crystal ; 1.0 ; 2-4"
                ), entry -> entry instanceof String);
        BUILDER.pop();

        BUILDER.comment("Legendary weapons and armour carried by legendary mobs,",
                        "in the spirit of Fallout's legendary effects.").push("gear");
        GEAR_LEGENDS_ENABLED = BUILDER
                .comment("Arm legendary mobs with a legendary-effect weapon or armour piece,",
                         "which they use in the fight and can drop.")
                .define("gearLegendsEnabled", true);
        LEGEND_WEAPON_CHANCE = BUILDER
                .comment("Chance a legendary mob carries a legendary weapon.")
                .defineInRange("legendWeaponChance", 0.5, 0.0, 1.0);
        LEGEND_ARMOR_CHANCE = BUILDER
                .comment("Chance a legendary mob wears a legendary armour piece.")
                .defineInRange("legendArmorChance", 0.35, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Which mobs take part. Any aggressive mob qualifies by default,",
                        "including those added by other mods.").push("mobs");
        INCLUDE_BOSSES = BUILDER
                .comment("Let bosses roll legendary and mutate too. A legendary Wither is rare and",
                         "brutal, and worth the nether stars in the loot rules below.",
                         "Bosses are still kept out of the target HUD, which is a client setting,",
                         "so their own boss bar stays the only health readout.")
                .define("includeBosses", true);
        MOB_BLACKLIST = BUILDER
                .comment("Entity ids that must never become legendary, for example minecraft:warden.")
                .defineList("mobBlacklist", List.of(), entry -> entry instanceof String);
        EXTRA_MOBS = BUILDER
                .comment("Entity ids to include even though they are not aggressive.")
                .defineList("extraMobs", List.of(), entry -> entry instanceof String);
        BUILDER.pop();

        BUILDER.comment("Server side of the stealth readout. How it looks is in the client config.")
                .push("detection");
        DETECTION_ENABLED = BUILDER
                .comment("Master switch: when off, the server never computes or sends detection state.")
                .define("detectionEnabled", true);
        DETECTION_RANGE = BUILDER
                .comment("Radius within which a hostile that has not noticed you reads as CAUTION.")
                .defineInRange("detectionRange", 15.0, 4.0, 128.0);
        DANGER_RANGE = BUILDER
                .comment("Radius within which a hostile that is hunting you reads as DANGER.",
                         "Wider on purpose: pillagers and skeletons lock on from well beyond",
                         "the distance at which a mob standing around is worth noticing.")
                .defineInRange("dangerRange", 48.0, 4.0, 128.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

}
