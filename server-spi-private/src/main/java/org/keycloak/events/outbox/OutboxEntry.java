package org.keycloak.events.outbox;

import java.time.Instant;

/**
 * Read-only view of one row of the generic outbox, as handed to an
 * {@code OutboxDeliveryHandler} and to {@code OutboxDrainerListener}
 * callbacks. The storage entity implements it; consumers program
 * against this interface so they depend neither on JPA nor on the
 * entity's mutable state — transitions are the drainer's job and go
 * through the store.
 *
 * <p>Two-axis classification: {@link #getEntryKind() entryKind} names
 * the consumer ("ssf-push", "scim", ...) and {@link #getEntryType()
 * entryType} the concrete message type within it. {@link #getOwnerId()
 * ownerId} is the destination (SSF receiver client, SCIM target,
 * webhook) and {@link #getContainerId() containerId} an optional
 * sub-grouping (SSF stream). {@link #getPayload() payload} and
 * {@link #getMetadata() metadata} are opaque text owned by the
 * consumer.
 */
public interface OutboxEntry {

    String getId();

    String getEntryKind();

    String getRealmId();

    String getOwnerId();

    /** Optional sub-grouping within {@code (entryKind, ownerId)}; may be {@code null}. */
    String getContainerId();

    /** Dedup key within {@code (entryKind, ownerId)}, e.g. the SET's jti. */
    String getCorrelationId();

    String getEntryType();

    OutboxEntryStatus getStatus();

    String getPayload();

    /** Consumer-owned extension data; may be {@code null}. */
    String getMetadata();

    /** Delivery attempts counted so far, not including the one in progress. */
    int getAttempts();

    Instant getNextAttemptAt();

    /** Operator-facing error of the most recent failed attempt; may be {@code null}. */
    String getLastError();

    Instant getCreatedAt();

    /** Set once the row reached {@code DELIVERED}; {@code null} before. */
    Instant getDeliveredAt();

    /** Token of the drainer tick currently delivering this row; {@code null} when not in flight. */
    String getClaimToken();

    /** End of that tick's lease; {@code null} when not in flight. */
    Instant getClaimedUntil();
}
