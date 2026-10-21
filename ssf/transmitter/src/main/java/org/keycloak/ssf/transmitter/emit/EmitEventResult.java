package org.keycloak.ssf.transmitter.emit;

import java.util.Map;

import org.keycloak.ssf.event.SsfEvent;

/**
 * Outcome of a synthetic SSF event emission. Carries the dispatch
 * status, (on success) the {@code jti} of the SET that went out so the
 * caller can correlate it against transmitter logs / outbox state, and
 * an optional human-readable message used to surface validation
 * failures (e.g. payload-shape mismatch against the registered event
 * class) so the admin endpoint can return a 400 with a useful body.
 * The optional {@code params} map carries machine-readable fields tied
 * to the specific failure (e.g. which subject member of a complex
 * subject failed to resolve) so REST callers can localize or react
 * programmatically without parsing the English {@code message}. Params
 * must never carry resolved entity data (usernames, aliases) — emit is
 * callable by role-scoped service accounts, so echoing resolved
 * identifiers would disclose realm metadata.
 */
public record EmitEventResult(EmitEventStatus status, String jti, String message, SsfEvent event,
                              Map<String, String> params) {

    public static EmitEventResult dispatched(String jti, SsfEvent typedEvent) {
        return new EmitEventResult(EmitEventStatus.DISPATCHED, jti, null, typedEvent, null);
    }

    public static EmitEventResult dropped(EmitEventStatus status) {
        return new EmitEventResult(status, null, null, null, null);
    }

    public static EmitEventResult dropped(EmitEventStatus status, String message) {
        return new EmitEventResult(status, null, message, null, null);
    }

    public static EmitEventResult dropped(EmitEventStatus status, String message, Map<String, String> params) {
        return new EmitEventResult(status, null, message, null, params);
    }
}
