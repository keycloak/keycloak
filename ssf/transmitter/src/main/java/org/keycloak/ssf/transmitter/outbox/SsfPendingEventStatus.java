package org.keycloak.ssf.transmitter.outbox;

/**
 * Lifecycle status of a row in the SSF push outbox ({@code SSF_PENDING_EVENT}).
 *
 * <p>The state machine is:
 * <pre>
 *     PENDING ──push succeeds──▶ DELIVERED
 *     PENDING ──retries exhausted──▶ DEAD_LETTER
 *     DEAD_LETTER ──admin "retry" action──▶ PENDING  (resets attempts, next_attempt_at)
 * </pre>
 *
 * <p>Rows in {@link #DELIVERED} are kept briefly for audit/idempotency
 * (jti dedup) and then purged by the drainer's housekeeping pass. Rows
 * in {@link #DEAD_LETTER} are retained for the configured
 * {@code outbox-dead-letter-retention} window (default 30d, anchored on
 * {@code createdAt}) and then purged by the same housekeeping pass. Set
 * the retention to {@code 0} to retain dead-letters indefinitely.
 */
public enum SsfPendingEventStatus {

    /**
     * Event is queued for delivery. The drainer picks up rows in this
     * state whose {@code next_attempt_at} is due.
     */
    PENDING,

    /**
     * Event was accepted by the receiver (2xx response) and needs no
     * further action.
     */
    DELIVERED,

    /**
     * All retry attempts have been exhausted without a successful
     * delivery. Requires admin intervention.
     */
    DEAD_LETTER
}
