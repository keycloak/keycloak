package org.keycloak.common.util;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeTest {

    private static final long TWENTY_YEARS_IN_SECONDS = TimeUnit.DAYS.toSeconds(365 * 20);

    @Test
    void currentTimeSecondsDoesNotOverflowWithLargeOffset() {
        long now = Time.currentTimeSeconds();
        long future = now + TWENTY_YEARS_IN_SECONDS;

        assertTrue(future > now, "Adding a 20-year offset must not overflow to a value less than now");
    }

    @Test
    void currentTimeOverflowsWithLargeOffset() {
        int now = Time.currentTime();
        int future = now + (int) TWENTY_YEARS_IN_SECONDS;

        assertTrue(future < 0, "int-based currentTime overflows with a 20-year addition, demonstrating the Y2038 bug");
    }
}
