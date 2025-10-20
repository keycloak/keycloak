package org.keycloak.protocol.ssf.event.types.risc;

import org.keycloak.protocol.ssf.event.types.SsfEvent;

public abstract class RiscEvent extends SsfEvent {

    public RiscEvent(String type) {
        super(type);
    }
}
