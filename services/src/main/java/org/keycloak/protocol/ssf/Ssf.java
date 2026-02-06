package org.keycloak.protocol.ssf;

import org.keycloak.protocol.ssf.receiver.spi.SsfReceiverProvider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Entry-point to lookup the SsfProvider.
 */
public class Ssf {

    private Ssf() {}

    public static SsfReceiverProvider receiverProvider() {
        return getKeycloakSession().getProvider(SsfReceiverProvider.class);
    }
}
