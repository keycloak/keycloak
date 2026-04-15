package org.keycloak.ssf.receiver;

import org.keycloak.utils.KeycloakSessionUtil;

public final class SsfReceivers {

    private SsfReceivers() {
    }

    public static SsfReceiverProvider current() {
        var session = KeycloakSessionUtil.getKeycloakSession();
        if (session == null) {
            return null;
        }
        return session.getProvider(SsfReceiverProvider.class);
    }
}
