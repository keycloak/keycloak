package org.keycloak.protocol.ssf.endpoint;

import jakarta.ws.rs.Path;
import org.jboss.logging.Logger;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * Exposes the realm specific SSF resource endpoints.
 */
public class SsfRealmResourceProvider implements RealmResourceProvider {

    protected static final Logger log = Logger.getLogger(SsfRealmResourceProvider.class);

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
        return Ssf.ssfProvider().pushDeliveryEndpoint();
    }


    @Override
    public void close() {
        // NOOP
    }

}
