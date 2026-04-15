package org.keycloak.ssf.transmitter;

import org.keycloak.utils.KeycloakSessionUtil;

public final class SsfTransmitter {

    private SsfTransmitter() {
    }

    public static SsfTransmitterProvider current() {
        var session = KeycloakSessionUtil.getKeycloakSession();
        if (session == null) {
            return null;
        }
        return session.getProvider(SsfTransmitterProvider.class);
    }
}
