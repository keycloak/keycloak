package org.keycloak.testsuite.util;

import org.keycloak.common.util.Time;

import static org.junit.Assert.fail;

public final class DateTimeAssert {

    private static final long DEFAULT_TOLERANCE_MS = 10_000;

    private DateTimeAssert() {
        // nothing to do
    }

    public static void assertTimestampIsCloseToNow(long timestamp) {
        assertTimestampIsCloseToNow(timestamp, DEFAULT_TOLERANCE_MS);
    }

    public static void assertTimestampIsCloseToNow(long timestamp, long toleranceMs) {
        if (toleranceMs < 0) {
            throw new IllegalArgumentException();
        }

        long now = Time.currentTimeMillis();
        long difference = now - timestamp;

        if (Math.abs(difference) > toleranceMs) {
            String errorMessage = String.format(
                    "Difference between now <%d> and timestamp <%d> is <%d>, which exceeds the absolute tolerance of <%d>.",
                    now, timestamp, difference, toleranceMs);
            fail(errorMessage);
        }
    }
}
