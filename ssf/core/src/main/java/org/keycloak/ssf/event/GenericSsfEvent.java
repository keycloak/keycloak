package org.keycloak.ssf.event;

/**
 * Fallback {@link SsfEvent} if we encounter an unknown SsfEvent type.
 */
public class GenericSsfEvent extends SsfEvent {

    /**
     * Required by Jackson: {@code SsfEventMapJsonDeserializer} materialises unknown
     * event payloads via {@code treeToValue(eventData, GenericSsfEvent.class)} and
     * sets the actual event type URI afterwards through {@link #setEventType}.
     */
    public GenericSsfEvent() {
        this(null);
    }

    public GenericSsfEvent(String eventType) {
        super(eventType);

        // Generic events don't have an alias by default
        setAlias(null);
    }
}
