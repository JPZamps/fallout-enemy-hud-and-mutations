package net.blacksmithzstudios.wasteland.client;

import net.blacksmithzstudios.wasteland.WastelandClientConfig;
import net.blacksmithzstudios.wasteland.WastelandColors;
import net.blacksmithzstudios.wasteland.legendary.MobEligibility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Fallout-style target readout: the target's name in small outlined caps, above a
 * fixed-width health bar held in an open bracket. The bar never shrinks - the portion
 * already lost simply reads as dark red behind the remaining health.
 *
 * Every size and colour here comes from the config. The player's own vanilla HUD is untouched.
 */
public final class TargetHud {

    /** Where vanilla's first boss bar sits, and how much room each one takes. */
    private static final int BOSS_BAR_TOP = 5;
    private static final int BOSS_BAR_HEIGHT = 19;

    /** Extra height the bracket's arms gain above the bar. Zero keeps them flush with it. */
    private static final int BRACKET_ARM = 0;

    private static final int BAR_FILL_FALLBACK = 0xFFF0522A;
    private static final int BAR_LOST_FALLBACK = 0xFF4A1207;
    private static final int NAME_FALLBACK = 0xFFFF3B2A;
    /** Softened outline: translucent, so it reads as a rim rather than a second glyph. */
    private static final int OUTLINE = 0x99000000;

    /** How fast the bar drains toward a new value, in fraction per frame. */
    private static final float DRAIN_SPEED = 0.02F;

    private static LivingEntity lastTarget;
    private static float displayedFraction;

    private TargetHud() {
    }

    /** Registered by {@link HudOverlays}. */
    static void render(GuiGraphicsExtractor graphics, int screenWidth, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || minecraft.player.isSpectator()) {
            return;
        }
        LivingEntity target = pickTarget(minecraft, partialTick);
        if (target == null || !target.isAlive()) {
            lastTarget = null;
            return;
        }
        if (WastelandClientConfig.HUD_SKIP_BOSSES.get() && MobEligibility.isBoss(target)) {
            lastTarget = null;
            return; // bosses already have their own bar
        }

        float fraction = Math.max(0.0F, Math.min(1.0F, target.getHealth() / target.getMaxHealth()));
        if (target != lastTarget) {
            lastTarget = target;
            displayedFraction = fraction; // snap when switching targets
        } else if (displayedFraction > fraction) {
            displayedFraction = Math.max(fraction, displayedFraction - DRAIN_SPEED);
        } else {
            displayedFraction = fraction;
        }

        int barWidth = WastelandClientConfig.TARGET_BAR_WIDTH.get();
        int barHeight = WastelandClientConfig.TARGET_BAR_HEIGHT.get();
        int topMargin = WastelandClientConfig.TARGET_TOP_MARGIN.get();
        float nameScale = WastelandClientConfig.TARGET_NAME_SCALE.get().floatValue();

        int barFill = WastelandColors.resolve(WastelandClientConfig.TARGET_BAR_COLOR, BAR_FILL_FALLBACK);
        int barLost = WastelandColors.resolve(WastelandClientConfig.TARGET_BAR_LOST_COLOR, BAR_LOST_FALLBACK);
        int nameColor = WastelandColors.resolve(WastelandClientConfig.TARGET_NAME_COLOR, NAME_FALLBACK);

        // The legendary flags live in server-side persistent data, which never reaches the
        // client. The custom name does sync, so that is what the readout reads.
        String name = target.getCustomName() != null
                ? target.getCustomName().getString()
                : target.getType().getDescription().getString();

        int centreX = screenWidth / 2;
        int bossOffset = bossBarOffset(minecraft);
        drawOutlinedName(graphics, minecraft.font, name.toUpperCase(Locale.ROOT),
                centreX, topMargin + bossOffset, nameScale, nameColor);

        int barTop = topMargin + bossOffset + Math.round(minecraft.font.lineHeight * nameScale)
                + WastelandClientConfig.TARGET_NAME_TO_BAR_GAP.get();
        int left = centreX - barWidth / 2;

        drawBracket(graphics, left, barTop, barWidth, barHeight, barFill);

        graphics.fill(left, barTop, left + barWidth, barTop + barHeight, barLost);
        graphics.fill(left, barTop, left + Math.round(barWidth * displayedFraction), barTop + barHeight, barFill);
    }

    /**
     * How far to drop the readout so it clears any boss bars.
     *
     * Vanilla draws boss bars from the top down, one per active boss event, so with a Wither
     * or Ender Dragon on screen the readout would otherwise sit right on top of them.
     */
    private static int bossBarOffset(Minecraft minecraft) {
        int bars = minecraft.gui.hud.getBossOverlay().events.size();
        return bars == 0 ? 0 : BOSS_BAR_TOP + bars * BOSS_BAR_HEIGHT;
    }

    /**
     * Finds what the player is looking at.
     *
     * Minecraft's own {@code crosshairPickEntity} stops at the player's interaction reach, a
     * few blocks, so it can only ever see something already in melee. This casts its own ray
     * out to the configured range, stopping at the first solid block so a mob cannot be read
     * through a wall.
     */
    private static LivingEntity pickTarget(Minecraft minecraft, float partialTick) {
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            return null;
        }

        double range = WastelandClientConfig.TARGET_RANGE.get();
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 look = camera.getViewVector(partialTick);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blocked = camera.level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera));
        double limit = blocked.getType() == HitResult.Type.MISS
                ? range
                : blocked.getLocation().distanceTo(eye);

        AABB search = camera.getBoundingBox().expandTowards(look.scale(limit)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, eye, eye.add(look.scale(limit)),
                search, candidate -> !candidate.isSpectator() && candidate.isPickable(), limit * limit);

        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * The open box holding the bar: a floor directly under it and two arms up its sides,
     * nothing across the top. Flush with the bar and in the same colour as the health left.
     */
    private static void drawBracket(GuiGraphicsExtractor graphics, int left, int barTop,
                                    int barWidth, int barHeight, int color) {
        int outerLeft = left - 1;
        int outerRight = left + barWidth + 1;
        int floor = barTop + barHeight;
        int armTop = barTop - BRACKET_ARM;

        graphics.fill(outerLeft, floor, outerRight, floor + 1, color);      // floor
        graphics.fill(outerLeft, armTop, left, floor, color);               // left arm
        graphics.fill(left + barWidth, armTop, outerRight, floor, color);   // right arm
    }

    /** Scaled-down text with a light black rim on the four cardinal sides. */
    private static void drawOutlinedName(GuiGraphicsExtractor graphics, Font font, String name,
                                         int centreX, int top, float scale, int color) {
        Component label = Component.literal(name).withStyle(ChatFormatting.BOLD);

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        int x = Math.round(centreX / scale) - font.width(label) / 2;
        int y = Math.round(top / scale);

        graphics.text(font, label, x - 1, y, OUTLINE, false);
        graphics.text(font, label, x + 1, y, OUTLINE, false);
        graphics.text(font, label, x, y - 1, OUTLINE, false);
        graphics.text(font, label, x, y + 1, OUTLINE, false);
        graphics.text(font, label, x, y, color, false);

        graphics.pose().popMatrix();
    }
}
