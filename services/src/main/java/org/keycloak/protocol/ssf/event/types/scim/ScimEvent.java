package org.keycloak.protocol.ssf.event.types.scim;

import org.keycloak.protocol.ssf.event.types.SsfEvent;

/**
 * Generic ScimEvent.
 *
 * See: https://www.ietf.org/archive/id/draft-ietf-scim-events-16.html
 */
public abstract class ScimEvent extends SsfEvent {

    public ScimEvent(String type) {
        super(type);
    }
}
