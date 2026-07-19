package org.keycloak.ssf.event.stream;

import org.keycloak.ssf.event.SsfEvent;

/**
 * Base class for all SSF stream related events.
 */
public abstract class SsfStreamEvent extends SsfEvent {

    public SsfStreamEvent(String eventType) {
        super(eventType);
    }
}
