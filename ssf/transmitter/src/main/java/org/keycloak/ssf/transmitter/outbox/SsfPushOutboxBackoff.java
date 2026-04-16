package org.keycloak.ssf.transmitter.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes the exponential-backoff next-attempt timestamp for a failed
 * push delivery, and decides when a row has exhausted its budget and
 * should be transitioned to {@link SsfPendingEventStatus#DEAD_LETTER}.
 *
 * <p>Default Curve (attempt number → wait before next try):
 * <pre>
 *     1 →   1s
 *     2 →   5s
 *     3 →  30s
 *     4 →   2m
 *     5 →  10m
 *     6 →   1h
 *     7 →   6h
 *     8+ →  24h (capped)
 * </pre>
 *
 * <p>A small uniform jitter (±25% of the base delay) is mixed in on each
 * computation so that a flood of events queued in the same tick don't
 * all wake up to retry in the same millisecond — spreading the retry
 * load in clustered deployments and across receivers.
 *
 * <p>Dead-letter threshold defaults to {@link #DEFAULT_MAX_ATTEMPTS} —
 * covers roughly a day of retries under the curve above, after which
 * continued retrying on a permanently-broken receiver (wrong endpoint
 * URL, revoked auth token, …) just wastes transmitter work.
 */
public class SsfPushOutboxBackoff {

    public static final int DEFAULT_MAX_ATTEMPTS = 8;

    public static final List<Duration> DEFAULT_CURVE = List.of(
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

    public SsfPushOutboxBackoff() {
        this(DEFAULT_MAX_ATTEMPTS);
    }

    public SsfPushOutboxBackoff(int maxAttempts) {
        this(maxAttempts, DEFAULT_CURVE);
    }

    public SsfPushOutboxBackoff(int maxAttempts, List<Duration> curve) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1, got " + maxAttempts);
        }
        if (curve == null || curve.isEmpty()) {
            throw new IllegalArgumentException("curve must not be null or empty");
        }
        this.maxAttempts = maxAttempts;
        this.curve = curve;
    }

    /**
     * Returns {@code true} if the row has burned through its retry
     * budget and the drainer should transition it to
     * {@link SsfPendingEventStatus#DEAD_LETTER DEAD_LETTER} instead of
     * scheduling another attempt.
     *
     * @param attempts Number of attempts that have already been made
     *                 (i.e. {@code entity.getAttempts()} after the
     *                 current failure has been accounted for).
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
        // Uniform jitter in [-jitterRange, +jitterRange]
        long jitterMillis = ThreadLocalRandom.current()
                .nextLong(-jitterRangeMillis, jitterRangeMillis + 1);
        long delayMillis = Math.max(0, baseMillis + jitterMillis);
        return now.plusMillis(delayMillis);
    }

    protected Duration baseDelayFor(int attempts) {
        // attempts is 1-based after the first failed try.
        var curve = getCurve();
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
