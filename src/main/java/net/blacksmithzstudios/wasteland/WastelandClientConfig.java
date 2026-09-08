package net.blacksmithzstudios.wasteland;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Everything the HUD draws.
 *
 * Kept apart from the common config on purpose: how the readout looks is each player's own
 * business, not the server's. On a dedicated server this spec is never loaded, and reading it
 * from anything that runs server-side would throw - so only client classes may touch it.
 */
public final class WastelandClientConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUD_SKIP_BOSSES;
    public static final ForgeConfigSpec.DoubleValue TARGET_RANGE;
    public static final ForgeConfigSpec.BooleanValue DETECTION_INDICATOR;
    public static final ForgeConfigSpec.BooleanValue DETECTION_ONLY_SNEAKING;
    public static final ForgeConfigSpec.IntValue TARGET_BAR_WIDTH;
    public static final ForgeConfigSpec.IntValue TARGET_BAR_HEIGHT;
    public static final ForgeConfigSpec.IntValue TARGET_TOP_MARGIN;
    public static final ForgeConfigSpec.IntValue TARGET_NAME_TO_BAR_GAP;
    public static final ForgeConfigSpec.DoubleValue TARGET_NAME_SCALE;
    public static final ForgeConfigSpec.ConfigValue<String> TARGET_BAR_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> TARGET_BAR_LOST_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> TARGET_NAME_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> HIDDEN_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> CAUTION_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> DANGER_COLOR;

    static {
        String colourHelp = "Either RRGGBB hex or a Minecraft dye name: " + WastelandColors.dyeList() + ".";

        BUILDER.comment("The Fallout-style target readout.").push("hud");
        HUD_ENABLED = BUILDER
                .comment("Show the target name and health bar at the top of the screen.")
                .define("hudEnabled", true);
        HUD_SKIP_BOSSES = BUILDER
                .comment("Hide the readout for bosses, which already have their own boss bar.")
                .define("hudSkipBosses", true);
        TARGET_RANGE = BUILDER
                .comment("How far the target readout can pick up a mob you are looking at, in blocks.")
                .defineInRange("targetRange", 25.0, 3.0, 128.0);
        TARGET_BAR_WIDTH = BUILDER
                .comment("Width of the target health bar, in GUI pixels.")
                .defineInRange("targetBarWidth", 86, 20, 400);
        TARGET_BAR_HEIGHT = BUILDER.defineInRange("targetBarHeight", 3, 1, 20);
        TARGET_TOP_MARGIN = BUILDER
                .comment("Distance from the top of the screen to the target name.")
                .defineInRange("targetTopMargin", 3, 0, 200);
        TARGET_NAME_TO_BAR_GAP = BUILDER
                .comment("Vertical gap between the name and the bar. Negative pulls the bar up.")
                .defineInRange("targetNameToBarGap", 0, -20, 40);
        TARGET_NAME_SCALE = BUILDER
                .comment("Target name size, as a fraction of the vanilla font.")
                .defineInRange("targetNameScale", 0.75, 0.25, 2.0);
        TARGET_BAR_COLOR = BUILDER
                .comment("Colour of the remaining health. Also used for the bracket.", colourHelp)
                .define("targetBarColor", "F0522A");
        TARGET_BAR_LOST_COLOR = BUILDER
                .comment("Colour of the health already lost.", colourHelp)
                .define("targetBarLostColor", "4A1207");
        TARGET_NAME_COLOR = BUILDER
                .comment("Colour of the target name.", colourHelp)
                .define("targetNameColor", "FF3B2A");
        BUILDER.pop();

        BUILDER.comment("The stealth readout above the crosshair.").push("detection");
        DETECTION_INDICATOR = BUILDER
                .comment("Show the HIDDEN / CAUTION / DANGER readout.")
                .define("detectionIndicator", true);
        DETECTION_ONLY_SNEAKING = BUILDER
                .comment("Only show it while sneaking, the way Fallout does.")
                .define("detectionOnlyWhileSneaking", true);
        HIDDEN_COLOR = BUILDER.comment("Colour when nothing is nearby.", colourHelp)
                .define("hiddenColor", "lime");
        CAUTION_COLOR = BUILDER.comment("Colour when hostiles are nearby but unaware.", colourHelp)
                .define("cautionColor", "yellow");
        DANGER_COLOR = BUILDER.comment("Colour when something is hunting you.", colourHelp)
                .define("dangerColor", "F03A2A");
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WastelandClientConfig() {
    }
}
