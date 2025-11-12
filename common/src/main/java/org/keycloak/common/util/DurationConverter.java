package org.keycloak.common.util;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class DurationConverter {

    private static final String PERIOD = "P";
    private static final String PERIOD_OF_TIME = "PT";
    public static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");
    private static final Pattern DIGITS_AND_UNIT = Pattern.compile("^(?:[-+]?\\d+(?:\\.\\d+)?(?i)[hms])+$");
    private static final Pattern DAYS = Pattern.compile("^[-+]?\\d+(?i)d$");
    private static final Pattern MILLIS = Pattern.compile("^[-+]?\\d+(?i)ms$");

    /**
     * If the {@code value} starts with a number, then:
     * <ul>
     * <li>If the value is only a number, it is treated as a number of seconds.</li>
     * <li>If the value is a number followed by {@code ms}, it is treated as a number of milliseconds.</li>
     * <li>If the value is a number followed by {@code h}, {@code m}, or {@code s}, it is prefixed with {@code PT}
     * and {@link Duration#parse(CharSequence)} is called.</li>
     * <li>If the value is a number followed by {@code d}, it is prefixed with {@code P}
     * and {@link Duration#parse(CharSequence)} is called.</li>
     * </ul>
     *
     * Otherwise, {@link Duration#parse(CharSequence)} is called.
     *
     * @param value a string duration
     * @return the parsed {@link Duration}
     * @throws IllegalArgumentException in case of parse failure
     */
    public static Duration parseDuration(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (DIGITS.asPredicate().test(value)) {
            return Duration.ofSeconds(Long.parseLong(value));
        } else if (MILLIS.asPredicate().test(value)) {
            return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
        }

        try {
            if (DIGITS_AND_UNIT.asPredicate().test(value)) {
                return Duration.parse(PERIOD_OF_TIME + value);
            } else if (DAYS.asPredicate().test(value)) {
                return Duration.parse(PERIOD + value);
            }

            return Duration.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Checks whether the given value represents a positive duration.
     *
     * @param value a string duration following the same format as in {@link #parseDuration(String)}
     * @return true if the value represents a positive duration, false otherwise
     */
    public static boolean isPositiveDuration(String value) {
        Duration duration = parseDuration(value);
        return duration != null && !duration.isNegative() && !duration.isZero();
    }
}
