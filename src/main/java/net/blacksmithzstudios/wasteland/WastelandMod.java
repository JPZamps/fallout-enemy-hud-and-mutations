package net.blacksmithzstudios.wasteland;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.blacksmithzstudios.wasteland.gear.GearLegends;
import net.blacksmithzstudios.wasteland.legendary.LegendaryLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

/**
 * Fallout Enemy HUD and Mutations - a Fallout-flavoured layer on pure vanilla content.
 *
 * Two systems, no new assets:
 *   - a Fallout style target readout and stealth indicator
 *   - legendary mobs that mutate mid-fight, elite ranks, and loot to match
 */
@Mod(WastelandMod.MOD_ID)
public class WastelandMod {

    public static final String MOD_ID = "wasteland";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Legendary rewards are added through the loot table, so every other loot hook sees them. */
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    static {
        LOOT_MODIFIERS.register("legendary_bonus", () -> LegendaryLootModifier.CODEC);
    }

    public WastelandMod(IEventBus modBus, ModContainer container) {
        LOOT_MODIFIERS.register(modBus);
        GearLegends.COMPONENTS.register(modBus);

        // Named after the mod, not the internal mod id, so the files are findable.
        container.registerConfig(ModConfig.Type.COMMON, WastelandConfig.SPEC,
                "falloutenemyhud-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, WastelandClientConfig.SPEC,
                "falloutenemyhud-client.toml");

        LOGGER.info("Fallout Enemy HUD and Mutations loaded");
    }
}
