package org.keycloak.ssf.transmitter.outbox;

/**
 * SSF-specific {@code entryKind} constants for the generic outbox
 * ({@link org.keycloak.events.outbox.OutboxStore}). Two kinds because push
 * and poll have genuinely different runtime paths:
 *
 * <ul>
 *   <li>{@link #PUSH} — drained by a server-side
 *       {@link org.keycloak.events.outbox.OutboxDrainerTask} that hands rows
 *       to the receiver's HTTP endpoint.</li>
 *   <li>{@link #POLL} — never drained; rows wait until the receiver's
 *       POLL request reads them from the outbox.</li>
 * </ul>
 *
 * <p>Within both kinds the row's {@code entryType} carries the SSF
 * security event type (e.g. {@code session-revoked}) and
 * {@code metadata} carries the SSF-specific extensions (streamId,
 * etc.) that don't have first-class generic columns.
 */
public final class SsfOutboxKinds {

    /** Outbox kind for SET rows delivered via HTTP push (RFC 8935). */
    public static final String PUSH = "ssf-push";

    /** Outbox kind for SET rows served on receiver POLL (RFC 8936). */
    public static final String POLL = "ssf-poll";

    private SsfOutboxKinds() {
    }
}
