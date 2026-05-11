package org.keycloak.ssf.event.caep;

import org.keycloak.ssf.event.SsfEvent;

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
