/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oid4vc.issuance;

import java.time.Instant;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/*
 *  @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class TimeClaimNormalizerTest {

    @Test
    public void offStrategy_keepsOriginal() {
        Instant orig = Instant.parse("2025-01-02T03:04:05Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.OFF, 0L, TimeClaimNormalizer.RoundUnit.DAY);
        assertThat(n.normalize(orig), is(orig));
    }

    @Test
    public void roundDay_truncatesToStartOfDayUtc() {
        Instant orig = Instant.parse("2025-01-02T23:59:59Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.ROUND, 0L, TimeClaimNormalizer.RoundUnit.DAY);
        Instant normalized = n.normalize(orig);
        assertThat(normalized, is(Instant.parse("2025-01-02T00:00:00Z")));
    }

    @Test
    public void roundHour_truncatesToHour() {
        Instant orig = Instant.parse("2025-01-02T03:59:59Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.ROUND, 0L, TimeClaimNormalizer.RoundUnit.HOUR);
        Instant normalized = n.normalize(orig);
        assertThat(normalized, is(Instant.parse("2025-01-02T03:00:00Z")));
    }

    @Test
    public void roundMinute_truncatesToMinute() {
        Instant orig = Instant.parse("2025-01-02T03:04:59Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.ROUND, 0L, TimeClaimNormalizer.RoundUnit.MINUTE);
        Instant normalized = n.normalize(orig);
        assertThat(normalized, is(Instant.parse("2025-01-02T03:04:00Z")));
    }

    @Test
    public void roundSecond_truncatesToSecond() {
        Instant orig = Instant.parse("2025-01-02T03:04:05.987654Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.ROUND, 0L, TimeClaimNormalizer.RoundUnit.SECOND);
        Instant normalized = n.normalize(orig);
        assertThat(normalized, is(Instant.parse("2025-01-02T03:04:05Z")));
    }

    @Test
    public void defaultRoundUnit_isSecond() {
        Instant orig = Instant.parse("2025-01-02T03:04:05.987654Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.ROUND, null, null);
        Instant normalized = n.normalize(orig);
        assertThat(normalized, is(Instant.parse("2025-01-02T03:04:05Z")));
    }

    @Test
    public void randomize_withinWindow_doesNotShiftIntoFuture() {
        Instant orig = Instant.parse("2025-01-02T22:00:00Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.RANDOMIZE, 3600L, TimeClaimNormalizer.RoundUnit.DAY);

        Instant normalized = n.normalize(orig);

        assertFalse("Normalized time should not be after original time", normalized.isAfter(orig));
    }

    @Test
    public void randomize_withinWindow_alwaysBeforeOrEqualOriginal() {
        Instant orig = Instant.parse("2025-01-02T22:00:00Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.RANDOMIZE, 3600L, TimeClaimNormalizer.RoundUnit.DAY);
        Instant normalized = n.normalize(orig);
        assertFalse("Normalized time should not be after original time", normalized.isAfter(orig));
        // should be within the randomization window
        long diffSeconds = orig.getEpochSecond() - normalized.getEpochSecond();
        assertTrue("Randomized time should be within window", diffSeconds <= 3600);
    }

    @Test
    public void randomize_alwaysRandomizesWithinWindow() {
        Instant orig = Instant.parse("2025-01-02T22:00:00Z");
        TimeClaimNormalizer n = new TimeClaimNormalizer(TimeClaimNormalizer.Strategy.RANDOMIZE, 3600L, TimeClaimNormalizer.RoundUnit.DAY);

        Instant normalized = n.normalize(orig);

        // we always randomize within the window (0 to 3600 seconds back)
        // So normalized should be <= original and within the window
        assertFalse("Normalized time should not be after original time", normalized.isAfter(orig));
        long diffSeconds = orig.getEpochSecond() - normalized.getEpochSecond();
        assertTrue("Randomized time should be within window", diffSeconds <= 3600);
    }

}
