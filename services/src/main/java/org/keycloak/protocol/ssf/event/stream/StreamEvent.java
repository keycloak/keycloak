package org.keycloak.protocol.ssf.event.stream;

import org.keycloak.protocol.ssf.event.SsfEvent;

/**
 * Base class for all SSF stream related events.
 */
public abstract class StreamEvent extends SsfEvent {

    public StreamEvent(String eventType) {
        super(eventType);
    }
}
