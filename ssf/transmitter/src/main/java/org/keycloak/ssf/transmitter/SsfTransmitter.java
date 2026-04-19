package org.keycloak.ssf.transmitter;

import org.keycloak.models.KeycloakSession;

public final class SsfTransmitter {

    private SsfTransmitter() {
    }

    /**
     * Resolves the {@link SsfTransmitterProvider} for the given session.
     * Single entry point for the lookup so we have one place to add
     * caching, logging, or test instrumentation around it later.
     *
     * @param session the active Keycloak session; must not be {@code null}.
     * @return the SSF transmitter provider bound to {@code session}.
     */
    public static SsfTransmitterProvider of(KeycloakSession session) {
        return session.getProvider(SsfTransmitterProvider.class);
    }

}
