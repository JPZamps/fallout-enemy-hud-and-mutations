package net.blacksmithzstudios.wasteland;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.blacksmithzstudios.wasteland.legendary.LegendaryLootModifier;
import net.blacksmithzstudios.wasteland.net.WastelandNetwork;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Fallout Enemy HUD and Mutations - a Fallout-flavoured layer on pure vanilla content.
 *
 * Two systems, no new assets:
 *   - a Pip-Boy style vitals HUD pinned to the top of the screen
 *   - legendary mobs that mutate mid-fight and drop hand-rolled vanilla loot
 */
@Mod(WastelandMod.MOD_ID)
public class WastelandMod {

    public static final String MOD_ID = "wasteland";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Legendary rewards are added through the loot table, so every other loot hook sees them. */
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    static {
        LOOT_MODIFIERS.register("legendary_bonus", () -> LegendaryLootModifier.CODEC);
    }

    public WastelandMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LOOT_MODIFIERS.register(modBus);

        WastelandNetwork.register();
        // Named after the mod, not the internal mod id, so the files are findable.
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WastelandConfig.SPEC,
                "falloutenemyhud-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WastelandClientConfig.SPEC,
                "falloutenemyhud-client.toml");
        LOGGER.info("Fallout Enemy HUD and Mutations loaded");
    }
}
