package net.swofty.type.skyblockgeneric.slayer;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Number formats as Hypixel writes them around slayers.
 *
 * <p>Client mods parse these strings (SkyHanni's progress pattern is
 * {@code \(?([0-9,.]+k?)/([0-9,.]+k?)\)? .*}), so the shapes matter more than the looks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlayerFormat {

    /** Sidebar progress numbers: {@code 950}, {@code 1.2k}, {@code 4.8k}, {@code 33.6k}. */
    public static String sidebar(long value) {
        if (value < 1000) {
            return Long.toString(value);
        }
        return trimmed(value / 1000D) + "k";
    }

    /** Boss health on its name tag: {@code 950}, {@code 12k}, {@code 2.5M}, {@code 150M}. */
    public static String health(double value) {
        if (value < 1000) {
            return Long.toString(Math.max(0, Math.round(value)));
        }
        if (value < 1_000_000) {
            return trimmed(value / 1000D) + "k";
        }
        if (value < 1_000_000_000) {
            return trimmed(value / 1_000_000D) + "M";
        }
        return trimmed(value / 1_000_000_000D) + "B";
    }

    /** Chat numbers: {@code 2,400}. */
    public static String commas(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** Mob time on a boss: {@code 02:59}. */
    public static String timer(long millisLeft) {
        long seconds = Math.max(0, millisLeft / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private static String trimmed(double value) {
        // One decimal, dropped when it is zero: 6.0 -> "6", 4.8 -> "4.8". Rounded down so a
        // boss on 2,499,999 health does not read as already being at 2.5M.
        double floored = Math.floor(value * 10D) / 10D;
        if (floored == Math.rint(floored)) {
            return Long.toString((long) floored);
        }
        return String.format(Locale.ROOT, "%.1f", floored);
    }
}
