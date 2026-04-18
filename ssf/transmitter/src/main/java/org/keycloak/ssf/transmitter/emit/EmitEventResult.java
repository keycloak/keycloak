package org.keycloak.ssf.transmitter.emit;

/**
 * Outcome of a synthetic SSF event emission. Carries the dispatch
 * status, (on success) the {@code jti} of the SET that went out so the
 * caller can correlate it against transmitter logs / outbox state, and
 * an optional human-readable message used to surface validation
 * failures (e.g. payload-shape mismatch against the registered event
 * class) so the admin endpoint can return a 400 with a useful body.
 */
public record EmitEventResult(EmitEventStatus status, String jti, String message) {

    public static EmitEventResult dispatched(String jti) {
        return new EmitEventResult(EmitEventStatus.DISPATCHED, jti, null);
    }

    public static EmitEventResult dropped(EmitEventStatus status) {
        return new EmitEventResult(status, null, null);
    }

    public static EmitEventResult dropped(EmitEventStatus status, String message) {
        return new EmitEventResult(status, null, message);
    }
}
