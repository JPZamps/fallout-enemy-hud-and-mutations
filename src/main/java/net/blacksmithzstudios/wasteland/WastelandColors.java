package net.blacksmithzstudios.wasteland;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Colour options for the HUD.
 *
 * A colour may be written either as RRGGBB hex or as one of Minecraft's sixteen dye names,
 * so the config reads like the game rather than like a stylesheet. Parsed values are cached:
 * the HUD asks for three colours on every frame, and re-parsing a string at that rate is
 * pointless work.
 */
public final class WastelandColors {

    /** The sixteen vanilla dyes, at their item colours. */
    private static final Map<String, Integer> DYES = Map.ofEntries(
            Map.entry("white", 0xF9FFFE),
            Map.entry("orange", 0xF9801D),
            Map.entry("magenta", 0xC74EBD),
            Map.entry("light_blue", 0x3AB3DA),
            Map.entry("yellow", 0xFED83D),
            Map.entry("lime", 0x80C71F),
            Map.entry("pink", 0xF38BAA),
            Map.entry("gray", 0x474F52),
            Map.entry("light_gray", 0x9D9D97),
            Map.entry("cyan", 0x169C9C),
            Map.entry("purple", 0x8932B8),
            Map.entry("blue", 0x3C44AA),
            Map.entry("brown", 0x835432),
            Map.entry("green", 0x5E7C16),
            Map.entry("red", 0xB02E26),
            Map.entry("black", 0x1D1D21));

    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    private WastelandColors() {
    }

    /** The dye names, for the config comment, so the options are discoverable in the file. */
    public static String dyeList() {
        return String.join(", ", DYES.keySet().stream().sorted().toList());
    }

    /**
     * Resolves a config colour to an opaque ARGB int.
     *
     * @param fallback used when the value is neither a dye name nor valid hex, so a typo
     *                 costs the reader a wrong colour rather than a crash
     */
    public static int resolve(ForgeConfigSpec.ConfigValue<String> value, int fallback) {
        String raw = value.get();
        if (raw == null) {
            return fallback;
        }
        Integer cached = CACHE.get(raw);
        if (cached != null) {
            return cached;
        }

        int resolved = parse(raw, fallback);
        CACHE.put(raw, resolved);
        return resolved;
    }

    private static int parse(String raw, int fallback) {
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replace("#", "").replace(' ', '_');

        Integer dye = DYES.get(cleaned);
        if (dye != null) {
            return 0xFF000000 | dye;
        }
        try {
            return 0xFF000000 | Integer.parseInt(cleaned, 16);
        } catch (NumberFormatException malformed) {
            return fallback;
        }
    }
}
