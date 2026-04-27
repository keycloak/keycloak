package org.keycloak.ssf.transmitter.outbox;

/**
 * Lifecycle status of a row in the SSF push outbox ({@code SSF_EVENT}).
 *
 * <p>The state machine is:
 * <pre>
 *     PENDING ──push succeeds──▶ DELIVERED
 *     PENDING ──retries exhausted──▶ DEAD_LETTER
 *     PENDING ──stream paused──▶ HELD
 *     HELD ──stream resumed (status enabled)──▶ PENDING
 *     PENDING / HELD ──stream disabled──▶ (deleted)
 *     DEAD_LETTER ──admin "retry" action──▶ PENDING  (resets attempts, next_attempt_at)
 * </pre>
 *
 * <p>Per the SSF spec a {@code disabled} stream MUST NOT transmit AND
 * "will not hold any events for later transmission", so the in-flight
 * backlog is discarded on the transition rather than parked — see
 * {@code SsfPendingEventStore.deleteUndeliveredForClient}. DELIVERED
 * and DEAD_LETTER rows are kept (jti dedup / post-failure audit).
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
    DEAD_LETTER,

    /**
     * Event is parked because the owning stream is in the
     * {@code paused} status. Neither the PUSH drainer nor the
     * POLL endpoint serves rows in this state. When the stream is
     * resumed (status returns to {@code enabled}), the held rows are
     * bulk-transitioned back to {@link #PENDING} in original arrival
     * order so the receiver gets the held SETs as if the pause had
     * never happened.
     */
    HELD
}
