package net.blacksmithzstudios.wasteland.client;

import net.blacksmithzstudios.wasteland.WastelandClientConfig;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The single place every overlay is registered.
 *
 * Deliberately one class with one handler: Forge's event bus generates its ASM wrapper from
 * the declaring class's simple name, method name and event type, so two same-named nested
 * classes in one package silently share a wrapper and one overlay registers twice while the
 * other never registers at all.
 */
@Mod.EventBusSubscriber(modid = WastelandMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HudOverlays {

    private HudOverlays() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("wasteland_target", (gui, graphics, partialTick, width, height) -> {
            if (WastelandClientConfig.HUD_ENABLED.get()) {
                TargetHud.render(graphics, width, partialTick);
            }
        });

        event.registerAboveAll("wasteland_detection", (gui, graphics, partialTick, width, height) -> {
            if (WastelandClientConfig.DETECTION_INDICATOR.get()) {
                DetectionHud.render(graphics, width, height);
            }
        });
    }
}
