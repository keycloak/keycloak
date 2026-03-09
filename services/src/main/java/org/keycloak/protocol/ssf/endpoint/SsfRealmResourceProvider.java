package org.keycloak.protocol.ssf.endpoint;

import jakarta.ws.rs.Path;

import org.keycloak.protocol.ssf.receiver.resources.SsfReceiversResource;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterResource;
import org.keycloak.services.resource.RealmResourceProvider;

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
    @Path("/receivers")
    public SsfReceiversResource receivers() {
        return new SsfReceiversResource();
    }

    /**
     * Entry point for the SSF transmitter.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/transmitter}
     *
     * @return
     */
    @Path("/transmitter")
    public SsfTransmitterResource transmitter() {
        return new SsfTransmitterResource();
    }

    @Override
    public void close() {
        // NOOP
    }

}
