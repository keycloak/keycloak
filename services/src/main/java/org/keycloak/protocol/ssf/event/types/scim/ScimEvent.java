package org.keycloak.protocol.ssf.event.types.scim;

import org.keycloak.protocol.ssf.event.types.SsfEvent;

public abstract class ScimEvent extends SsfEvent {

    public ScimEvent(String type) {
        super(type);
    }
}
