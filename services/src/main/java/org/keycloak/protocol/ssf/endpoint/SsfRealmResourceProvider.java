package org.keycloak.protocol.ssf.endpoint;

import jakarta.ws.rs.Path;

import org.keycloak.protocol.ssf.Ssf;
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
     * Endpoint for SET Push delivery via HTTP.
     *
     * The endpoint is available via {@code $KC_ISSUER_URL/ssf/push}
     *
     * @return
     */
    @Path("/push")
    public SsfPushDeliveryResource pushEndpoint() {
        // push endpoint authentication checked by PushEndpoit directly.
        return Ssf.receiverProvider().pushDeliveryEndpoint();
    }


    @Override
    public void close() {
        // NOOP
    }

}
