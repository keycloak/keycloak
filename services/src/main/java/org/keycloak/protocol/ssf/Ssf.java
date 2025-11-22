package org.keycloak.protocol.ssf;

import org.keycloak.protocol.ssf.spi.SsfProvider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Entry-point to lookup the SsfProvider.
 */
public class Ssf {

    private Ssf() {}

    public static SsfProvider ssfProvider() {
        return getKeycloakSession().getProvider(SsfProvider.class);
    }
}
