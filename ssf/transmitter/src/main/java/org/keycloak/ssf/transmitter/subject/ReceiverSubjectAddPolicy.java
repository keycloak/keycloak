package org.keycloak.ssf.transmitter.subject;

/**
 * Per-receiver policy controlling which subjects a receiver may
 * subscribe itself to via the receiver-facing
 * {@code POST /subjects/add} endpoint (SSF §8.1.3.2). Stored on the
 * receiver client as {@code ssf.receiverSubjectAddPolicy}.
 *
 * <p>Admin-driven adds from the admin console are not subject to this
 * policy — operators are trusted.
 *
 * <ul>
 *     <li>{@link #NONE} — receiver-driven adds are always denied.</li>
 *     <li>{@link #AUTHENTICATED} — the receiver may only add users that
 *         have a relationship with the receiver client: an active or
 *         offline user session with a client session for the receiver,
 *         or a granted consent for it. Organization subjects are
 *         denied because they implicitly subscribe every member.</li>
 *     <li>{@link #ANY} — the receiver may add any user or organization
 *         in the realm. An explicit admin grant of realm-wide
 *         monitoring.</li>
 * </ul>
 */
public enum ReceiverSubjectAddPolicy {
    NONE,
    AUTHENTICATED,
    ANY;

    public static final ReceiverSubjectAddPolicy DEFAULT = AUTHENTICATED;

    /**
     * Parses a case-insensitive string into a policy, returning
     * {@code fallback} when the input is {@code null}, blank, or not a
     * legal value.
     */
    public static ReceiverSubjectAddPolicy parseOrDefault(String value, ReceiverSubjectAddPolicy fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ReceiverSubjectAddPolicy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
