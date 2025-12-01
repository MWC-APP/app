package ch.inf.usi.mindbricks.util;

import java.util.Locale;

/**
 * Utility class for formatting hours.
 */
public final class Hours {

    /**
     * Private constructor to prevent instantiation.
     */
    private Hours(){
        // no-op
    }

    /**
     * Formats the given hours as a string.
     * @param hours the number of hours
     * @return formatted string with format "X.Y h" (i.e., "2 h" or "2.5 h")
     */
    public static String formatHours(float hours) {
        if (hours == (long) hours) {
            return String.format(Locale.US, "%d h", (long) hours);
        }
        return String.format(Locale.US, "%.1f h", hours);
    }
}
