package org.keycloak.events.outbox;

import java.time.Duration;

/**
 * Counters for one {@link OutboxDrainerTask} tick, handed to
 * {@link OutboxDrainerListener#onTickEnd}. The row counters cover the
 * drain pass; {@code stalePromoted}, {@code purgedDelivered} and
 * {@code purgedDeadLetter} are the housekeeping steps that follow it.
 *
 * <p>{@code claimed} is the batch size the tick took a lease on.
 * {@code released} counts claimed rows handed back unprocessed because
 * the tick budget ran out; {@code claimLost} counts rows whose claim
 * was taken over (lease expired, admin re-arm) before the tick could
 * record a transition for them.
 */
public record OutboxDrainerTickSummary(String entryKind,
                                       int claimed,
                                       int processed,
                                       int delivered,
                                       int retried,
                                       int deferred,
                                       int deadLettered,
                                       int released,
                                       int claimLost,
                                       int stalePromoted,
                                       int purgedDelivered,
                                       int purgedDeadLetter,
                                       Duration duration) {
}
