package org.keycloak.protocol.ssf.event.types.caep;

import org.keycloak.protocol.ssf.event.types.SsfEvent;

/**
 * Generic CaepEvent.
 *
 * See: https://openid.net/specs/openid-caep-1_0-final.html
 */
public abstract class CaepEvent extends SsfEvent {

    public CaepEvent(String type) {
        super(type);
    }
}
