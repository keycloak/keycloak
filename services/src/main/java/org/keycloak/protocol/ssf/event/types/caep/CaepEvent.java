package org.keycloak.protocol.ssf.event.types.caep;

import org.keycloak.protocol.ssf.event.types.SsfEvent;

public abstract class CaepEvent extends SsfEvent {

    public CaepEvent(String type) {
        super(type);
    }
}
