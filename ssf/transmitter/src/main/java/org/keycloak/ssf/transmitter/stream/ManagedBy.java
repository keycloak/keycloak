package org.keycloak.ssf.transmitter.stream;

/**
 * Origin / ownership marker on a stream — who registered it and which
 * surface "owns" the configuration conceptually. Set at creation time
 * and immutable thereafter.
 *
 * <p>Both modes accept admin and receiver-driven updates; the marker
 * is purely informational so the admin UI can render which surface
 * conceptually owns the stream and warn operators when they're about
 * to overwrite a receiver-managed configuration. The dispatcher's
 * gate doesn't read this — it's an admin/audit concern.
 */
public enum ManagedBy {

    /**
     * Stream was registered through the receiver-facing
     * {@code POST /streams} endpoint (SSF §8.1.1). The receiver client
     * is the conceptual owner of the configuration; admin edits are
     * still permitted but are flagged as overrides in the UI.
     *
     * <p>This is the default for streams persisted before the marker
     * was introduced (absent attribute → {@code RECEIVER}).
     */
    RECEIVER,

    /**
     * Stream was registered through the admin-facing
     * {@code POST /admin/realms/{realm}/ssf/clients/{clientId}/stream}
     * endpoint (driven by the Admin UI's Stream tab). The receiver
     * never called the SSF spec endpoints; the admin owns the
     * configuration. Receiver-driven updates are still accepted but
     * the operator drives the lifecycle.
     */
    KEYCLOAK;

    /**
     * Defensive parse used by {@link org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore}
     * when reading the persisted attribute. Unknown / unparseable
     * values fall back to {@link #RECEIVER} (the legacy default), so
     * malformed manual edits to the client attribute can't lock the
     * stream into an unrecognised mode.
     */
    public static ManagedBy parseOrDefault(String raw, ManagedBy fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
