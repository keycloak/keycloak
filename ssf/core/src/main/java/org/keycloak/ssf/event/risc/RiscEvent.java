package org.keycloak.ssf.event.risc;

import org.keycloak.ssf.event.SsfEvent;

/**
 * Generic RiscEvent.
 *
 * See: https://openid.net/specs/openid-risc-1_0-final.html
 */
public abstract class RiscEvent extends SsfEvent {

    public RiscEvent(String type) {
        super(type);
    }
}
