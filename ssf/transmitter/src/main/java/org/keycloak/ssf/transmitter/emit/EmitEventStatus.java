package org.keycloak.ssf.transmitter.emit;

/**
 * Outcome categories for a synthetic SSF event emission attempt.
 *
 * <p>Returned to the trusted emitter (an IAM management client) so it
 * can distinguish a successful dispatch from a filter-dropped event.
 * Stable wire values — used as the {@code status} field in the admin
 * emit endpoint response.
 */
public enum EmitEventStatus {

    /** Event accepted, signed SET enqueued for push delivery. */
    DISPATCHED("dispatched"),

    /** Receiver does not subscribe to this event type ({@code events_requested}). */
    DROPPED_FILTERED("dropped_filtered"),

    /** Subject is not subscribed to receive events for this receiver. */
    DROPPED_UNSUBSCRIBED("dropped_unsubscribed"),

    /** Subject couldn't be resolved to a Keycloak user or organization. */
    SUBJECT_NOT_FOUND("subject_not_found"),

    /** Receiver has no SSF stream registered yet. */
    STREAM_NOT_FOUND("stream_not_found"),

    /** Event type alias / URI is not registered with the transmitter. */
    UNKNOWN_EVENT_TYPE("unknown_event_type"),

    /**
     * Event type is registered but not allowed to be emitted via the
     * synthetic emitter API — e.g. the SSF stream-management events
     * (verification, stream-updated) which are protocol-internal and
     * must only be issued by the transmitter itself.
     */
    EVENT_TYPE_NOT_EMITTABLE("event_type_not_emittable"),

    /** Payload missing required fields or malformed. */
    INVALID_REQUEST("invalid_request"),

    /**
     * Event payload deserialised cleanly but the event class' own
     * {@code validate()} hook rejected it — typically a missing
     * spec-required field (e.g. {@code change_type} on
     * {@code CaepCredentialChange}). Distinct from
     * {@link #INVALID_REQUEST} so admin callers can tell a
     * malformed-JSON / wrong-type problem from a missing-field
     * problem. Wire value matches
     * {@link org.keycloak.ssf.event.SsfEventValidationException#MESSAGE_KEY}
     * so the i18n key is the same for both layers.
     */
    INVALID_EVENT_DATA("invalid_event_data");

    private final String wireValue;

    EmitEventStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
