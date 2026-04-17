package org.keycloak.ssf.transmitter.emit;

/**
 * Outcome of a synthetic SSF event emission. Carries the dispatch
 * status and (on success) the {@code jti} of the SET that went out so
 * the caller can correlate it against transmitter logs / outbox state.
 */
public record EmitEventResult(EmitEventStatus status, String jti) {

    public static EmitEventResult dispatched(String jti) {
        return new EmitEventResult(EmitEventStatus.DISPATCHED, jti);
    }

    public static EmitEventResult dropped(EmitEventStatus status) {
        return new EmitEventResult(status, null);
    }
}
