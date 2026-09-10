package org.keycloak.ssf.event.stream;

import org.keycloak.ssf.event.SsfEvent;

/**
 * Base class for all SSF stream related events.
 */
public abstract class SsfStreamEvent extends SsfEvent {

    public static final String EVENT_TYPE_BASE_URI = "https://schemas.openid.net/secevent/ssf/event-type/";

    public SsfStreamEvent(String eventType) {
        super(eventType);
    }
}
