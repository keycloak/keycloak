package org.keycloak.ssf.services;

import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.ssf.transmitter.resources.SsfTransmitterResource;
import org.keycloak.ssf.transmitter.support.SsfAuthUtil;

/**
 * Exposes the realm specific SSF resource endpoints.
 */
public class SsfRealmResourceProvider implements RealmResourceProvider {

    protected final KeycloakSession session;

    public SsfRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * Entry point for the SSF transmitter.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/transmitter}
     */
    @Path(Ssf.SSF_TRANSMITTER_PATH)
    public SsfTransmitterResource transmitter() {
        if (!Ssf.isTransmitterEnabled(session.getContext().getRealm())) {
            return null;
        }
        var authResult = SsfAuthUtil.authenticate();
        return new SsfTransmitterResource(session, authResult, SsfTransmitter.of(session));
    }

    @Override
    public void close() {
        // NOOP
    }

}
