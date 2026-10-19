package org.keycloak.events.outbox;

import java.time.Instant;

import org.keycloak.models.KeycloakSession;

/**
 * Observer hooks the {@link OutboxDrainerTask} invokes around a tick
 * and on every row transition it performs. Lets a consumer record
 * metrics (delivery counters, dead-letter alerts, queue depth
 * snapshots) without subclassing the drainer or re-deriving the
 * transition from the handler's return value.
 *
 * <p>Every method has a no-op default, so implementations override
 * only what they need. A handler that wants these callbacks can
 * implement this interface as well — the drainer's three-argument
 * constructor uses the handler as listener in that case.
 *
 * <p>Listener failures are logged and swallowed by the drainer; they
 * never affect the row's transition or the rest of the tick.
 *
 * <p>All callbacks run inside the drainer's transaction, before it
 * commits. A listener that needs the row's new state durably
 * committed (e.g. to notify an external system) should do so from
 * {@link #onTickEnd} or defer the work to a transaction completion
 * hook.
 */
public interface OutboxDrainerListener {

    OutboxDrainerListener NOOP = new OutboxDrainerListener() {
    };

    /** Why the drainer moved a row to {@code DEAD_LETTER}. */
    enum DeadLetterCause {
        /** {@link OutboxDeliveryOutcome#RETRY} with no attempts left. */
        ATTEMPTS_EXHAUSTED,
        /** The handler returned {@link OutboxDeliveryOutcome#DEAD_LETTER}. */
        HANDLER,
        /** The handler returned {@link OutboxDeliveryOutcome#ORPHANED}. */
        ORPHANED
    }

    default void onTickStart(KeycloakSession session, String entryKind) {
    }

    default void onTickEnd(KeycloakSession session, OutboxDrainerTickSummary summary) {
    }

    default void onDelivered(KeycloakSession session, OutboxEntry row) {
    }

    /**
     * A failed attempt was recorded and the row rescheduled per the
     * backoff curve. {@code row.getAttempts()} already reflects the
     * failed attempt.
     */
    default void onRetryScheduled(KeycloakSession session, OutboxEntry row, Instant nextAttemptAt, String reason) {
    }

    /** The row was rescheduled without counting an attempt. */
    default void onDeferred(KeycloakSession session, OutboxEntry row, Instant notBefore, String reason) {
    }

    /**
     * The row reached {@code DEAD_LETTER}. The row's status and
     * {@code last_error} are already updated when this is called.
     */
    default void onDeadLetter(KeycloakSession session, OutboxEntry row, DeadLetterCause cause, String reason) {
    }
}
