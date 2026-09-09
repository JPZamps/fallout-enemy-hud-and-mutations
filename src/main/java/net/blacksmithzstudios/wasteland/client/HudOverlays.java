package net.blacksmithzstudios.wasteland.client;

import net.blacksmithzstudios.wasteland.WastelandClientConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The single place every overlay is registered.
 *
 * Deliberately one class with one handler: two same-named nested classes in one package
 * silently shared an event wrapper in an earlier version, and one overlay registered twice
 * while the other never registered at all.
 */
@EventBusSubscriber(modid = WastelandMod.MOD_ID, value = Dist.CLIENT)
public final class HudOverlays {

    private static final Identifier TARGET =
            Identifier.fromNamespaceAndPath(WastelandMod.MOD_ID, "target");
    private static final Identifier DETECTION =
            Identifier.fromNamespaceAndPath(WastelandMod.MOD_ID, "detection");

    private HudOverlays() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, TARGET, (GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) -> {
            if (WastelandClientConfig.HUD_ENABLED.get()) {
                TargetHud.render(graphics, graphics.guiWidth(), delta.getGameTimeDeltaPartialTick(false));
            }
        });

        event.registerAbove(VanillaGuiLayers.CROSSHAIR, DETECTION, (GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) -> {
            if (WastelandClientConfig.DETECTION_INDICATOR.get()) {
                DetectionHud.render(graphics, graphics.guiWidth(), graphics.guiHeight());
            }
        });
    }
}
