package org.keycloak.protocol.ssf.event.types.stream;

import org.keycloak.protocol.ssf.event.types.SsfEvent;

/**
 * Base class for all SSF stream related events.
 */
public abstract class StreamEvent extends SsfEvent {

    public StreamEvent(String eventType) {
        super(eventType);
    }
}
