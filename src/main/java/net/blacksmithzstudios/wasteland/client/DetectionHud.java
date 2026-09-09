package net.blacksmithzstudios.wasteland.client;

import net.blacksmithzstudios.wasteland.WastelandClientConfig;
import net.blacksmithzstudios.wasteland.WastelandColors;
import net.blacksmithzstudios.wasteland.net.DetectionTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Fallout's stealth readout: HIDDEN while the area is clear, CAUTION once hostiles are inside
 * the radius, DANGER once one is actually hunting you.
 *
 * The brackets carry the distance the way the original does - wide open when the nearest
 * threat is at the edge of the radius, closing in tight as it approaches. Shown only while
 * crouched, since it is a sneaking tool rather than permanent screen furniture.
 */
public final class DetectionHud {

    private static final int ABOVE_CROSSHAIR = 26;
    private static final float SCALE = 0.75F;
    private static final int OUTLINE = 0x99000000;

    /** Bracket offset from the label at the two extremes, in scaled pixels. */
    private static final int GAP_FAR = 22;
    private static final int GAP_NEAR = 2;

    private static final int HIDDEN_FALLBACK = 0xFF4CD964;
    private static final int CAUTION_FALLBACK = 0xFFE0B23B;
    private static final int DANGER_FALLBACK = 0xFFF03A2A;

    /** Smooths the bracket travel so it glides instead of stepping twice a second. */
    private static final float EASE = 0.15F;
    private static float displayedCloseness;

    private DetectionHud() {
    }

    /** Registered by {@link HudOverlays}. */
    static void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || minecraft.player.isSpectator()) {
            return;
        }
        if (WastelandClientConfig.DETECTION_ONLY_SNEAKING.get() && !minecraft.player.isCrouching()) {
            return;
        }

        String label;
        int color;
        switch (ClientDetection.state()) {
            case DetectionTracker.DANGER -> {
                label = "DANGER";
                color = WastelandColors.resolve(WastelandClientConfig.DANGER_COLOR, DANGER_FALLBACK);
            }
            case DetectionTracker.CAUTION -> {
                label = "CAUTION";
                color = WastelandColors.resolve(WastelandClientConfig.CAUTION_COLOR, CAUTION_FALLBACK);
            }
            default -> {
                label = "HIDDEN";
                color = WastelandColors.resolve(WastelandClientConfig.HIDDEN_COLOR, HIDDEN_FALLBACK);
            }
        }

        displayedCloseness += (ClientDetection.closeness() - displayedCloseness) * EASE;
        int gap = Math.round(GAP_FAR - (GAP_FAR - GAP_NEAR) * displayedCloseness);

        Component text = Component.literal(label).withStyle(ChatFormatting.BOLD);
        Component open = Component.literal("[").withStyle(ChatFormatting.BOLD);
        Component close = Component.literal("]").withStyle(ChatFormatting.BOLD);

        graphics.pose().pushMatrix();
        graphics.pose().scale(SCALE, SCALE);

        int centreX = Math.round((screenWidth / 2.0F) / SCALE);
        int y = Math.round((screenHeight / 2.0F - ABOVE_CROSSHAIR) / SCALE);
        int halfLabel = minecraft.font.width(text) / 2;

        draw(graphics, text, centreX - halfLabel, y, color);
        draw(graphics, open, centreX - halfLabel - gap - minecraft.font.width(open), y, color);
        draw(graphics, close, centreX + halfLabel + gap, y, color);

        graphics.pose().popMatrix();
    }

    /** Draws the glyph with a light black rim on the four cardinal sides. */
    private static void draw(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.text(minecraft.font, text, x - 1, y, OUTLINE, false);
        graphics.text(minecraft.font, text, x + 1, y, OUTLINE, false);
        graphics.text(minecraft.font, text, x, y - 1, OUTLINE, false);
        graphics.text(minecraft.font, text, x, y + 1, OUTLINE, false);
        graphics.text(minecraft.font, text, x, y, color, false);
    }
}
