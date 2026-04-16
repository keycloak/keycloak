package org.keycloak.ssf.services;

import jakarta.ws.rs.Path;

import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.receiver.resources.SsfReceiversResource;
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
     * Entry point for the SSF receiver.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/receivers}
     *
     * @return
     */
    @Path(Ssf.SSF_RECEIVERS_PATH)
    public SsfReceiversResource receivers() {
        // authentication is perform on by validating SET signature and with Push auth header on stream level
        return new SsfReceiversResource(KeycloakSessionUtil.getKeycloakSession());
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
        return new SsfTransmitterResource(KeycloakSessionUtil.getKeycloakSession(), authResult);
    }

    @Override
    public void close() {
        // NOOP
    }

}
