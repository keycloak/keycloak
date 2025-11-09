package org.keycloak.protocol.ssf;

import org.keycloak.protocol.ssf.spi.SsfProvider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

public class Ssf {

    public static final String APPLICATION_SECEVENT_JWT_TYPE = "application/secevent+jwt";

    public static final String SECEVENT_JWT_TYPE = "secevent+jwt";

    private Ssf() {}

    public static SsfProvider currentSsfProvider() {
        return getKeycloakSession().getProvider(SsfProvider.class);
    }
}
