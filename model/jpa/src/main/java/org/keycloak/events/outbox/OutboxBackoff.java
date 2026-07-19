/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
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
package org.keycloak.events.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes the exponential-backoff next-attempt timestamp for a failed
 * outbox delivery, and decides when a row has exhausted its budget and
 * should be transitioned to {@code DEAD_LETTER}.
 *
 * <p>The backoff curve and the dead-letter threshold are configured per
 * {@link OutboxConfig#backoff()}, so different consumers (SSF push,
 * webhooks, ...) can pick curves that match their delivery semantics.
 *
 * <p>A small uniform jitter (±25% of the base delay) is mixed in on
 * each computation so a flood of rows enqueued in the same tick don't
 * all wake up to retry in the same millisecond — spreading the retry
 * load in clustered deployments and across receivers.
 */
public class OutboxBackoff {

    public static final int DEFAULT_MAX_ATTEMPTS = 8;

    /**
     * Default HTTP-push curve — sensible for receivers that respond to
     * an HTTP POST. Consumers with different delivery semantics
     * (e.g. internal queue write) should provide their own.
     */
    public static final List<Duration> DEFAULT_HTTP_PUSH_CURVE = List.of(
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            Duration.ofHours(6),
            Duration.ofHours(24)
    );

    protected final int maxAttempts;
    protected final List<Duration> curve;

    public OutboxBackoff() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_HTTP_PUSH_CURVE);
    }

    public OutboxBackoff(int maxAttempts, List<Duration> curve) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1, got " + maxAttempts);
        }
        Objects.requireNonNull(curve, "curve");
        if (curve.isEmpty()) {
            throw new IllegalArgumentException("curve must not be empty");
        }
        this.maxAttempts = maxAttempts;
        this.curve = curve;
    }

    /**
     * Returns {@code true} if the row has burned through its retry
     * budget and should be transitioned to {@code DEAD_LETTER} instead
     * of scheduling another attempt.
     *
     * @param attempts the value of {@code attempts} after the current
     *                 failure has been accounted for.
     */
    public boolean isExhausted(int attempts) {
        return attempts >= maxAttempts;
    }

    /**
     * Computes the {@code next_attempt_at} timestamp for a row whose
     * {@code attempts} counter has just been incremented to the given
     * value after a failure. Callers should only invoke this when
     * {@link #isExhausted(int)} returns false.
     */
    public Instant computeNextAttemptAt(Instant now, int attempts) {
        Duration base = baseDelayFor(attempts);
        long baseMillis = base.toMillis();
        long jitterRangeMillis = Math.max(1, baseMillis / 4);
        long jitterMillis = ThreadLocalRandom.current()
                .nextLong(-jitterRangeMillis, jitterRangeMillis + 1);
        long delayMillis = Math.max(0, baseMillis + jitterMillis);
        return now.plusMillis(delayMillis);
    }

    /**
     * Sum of the curve up to {@link #maxAttempts} entries — the
     * worst-case time a row can spend in PENDING under the natural
     * retry path before exhausting attempts. Operators tuning
     * {@code pendingMaxAge} backstops should keep that value
     * comfortably above this floor.
     */
    public Duration getMaxNaturalRetryDuration() {
        Duration total = Duration.ZERO;
        int n = Math.min(maxAttempts, curve.size());
        for (int i = 0; i < n; i++) {
            total = total.plus(curve.get(i));
        }
        return total;
    }

    protected Duration baseDelayFor(int attempts) {
        // attempts is 1-based after the first failed try.
        int idx = Math.min(Math.max(attempts, 1), curve.size()) - 1;
        return curve.get(idx);
    }

    public List<Duration> getCurve() {
        return curve;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
