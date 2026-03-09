package org.keycloak.protocol.ssf.event.risc;

import org.keycloak.protocol.ssf.event.SsfEvent;

/**
 * Generic RISC event.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public abstract class RiscEvent extends SsfEvent {

    public RiscEvent(String type) {
        super(type);
    }
}
