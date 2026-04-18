package org.keycloak.ssf.services;

import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.resources.SsfTransmitterResource;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * Exposes the realm specific SSF resource endpoints.
 */
public class SsfRealmResourceProvider implements RealmResourceProvider {

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Entry point for the SSF transmitter.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/transmitter}
     *
     * @return
     */
    @Path(Ssf.SSF_TRANSMITTER_PATH)
    public SsfTransmitterResource transmitter() {
        var authResult = SsfAuthUtil.authenticate();
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        return new SsfTransmitterResource(session, authResult, transmitter);
    }

    @Override
    public void close() {
        // NOOP
    }

}
