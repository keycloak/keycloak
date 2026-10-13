package org.keycloak.ssf.event;

import org.keycloak.ssf.SsfException;

/**
 * Thrown by {@link SsfEvent#validate()} when an event instance is
 * missing a field the SSF / CAEP / RISC spec marks as REQUIRED — or
 * a custom event implementation enforces its own invariants.
 *
 * <p>Carries a stable {@link #MESSAGE_KEY} ({@code invalid_event_data})
 * plus structured {@code eventAlias} and {@code field} fields so
 * callers (REST emit response, admin UI) can compose a localised
 * message from the pieces instead of parsing an English string. The
 * inherited {@link #getMessage()} returns a mechanical
 * {@code "<key>: <alias>.<field>"} composition for log lines and
 * non-localised callers.
 *
 * <p>Caught by the synthetic-emit pipeline and surfaced to the caller
 * as an {@code invalid_event_payload} response so an operator pushing
 * a hand-rolled JSON event sees exactly which field is missing
 * instead of a half-populated SET landing at the receiver.
 *
 * <p>Native event production never throws this — the SSF event listener
 * builds events from typed Keycloak event details that always supply
 * the spec-required fields. The exception type lives on this layer
 * (rather than under {@code transmitter}) so {@link SsfEvent} subclasses
 * can throw it from their {@link SsfEvent#validate()} override without
 * pulling a transmitter-layer dependency into {@code ssf/core}.
 */
public class SsfEventValidationException extends SsfException {

    /**
     * Stable, i18n-friendly message key for every validation failure.
     * Callers that localise messages key off this plus the
     * {@link #getEventAlias()} / {@link #getField()} structured fields.
     */
    public static final String MESSAGE_KEY = "invalid_event_data";

    private final String eventAlias;
    private final String field;

    public SsfEventValidationException(String eventAlias, String field) {
        super(MESSAGE_KEY + ": " + eventAlias + "." + field);
        this.eventAlias = eventAlias;
        this.field = field;
    }

    public String getMessageKey() {
        return MESSAGE_KEY;
    }

    /**
     * Alias of the event that failed validation
     * (e.g. {@code CaepCredentialChange}). Sourced from
     * {@link SsfEvent#getAlias()} at the throw site.
     */
    public String getEventAlias() {
        return eventAlias;
    }

    /**
     * Wire-name of the field that failed validation
     * (e.g. {@code change_type}). Use the {@code @JsonProperty} value,
     * not the Java field name, so the operator sees the same identifier
     * they used in the JSON body.
     */
    public String getField() {
        return field;
    }
}
