package org.keycloak.protocol.ssf.endpoint;

import jakarta.ws.rs.Path;
import org.jboss.logging.Logger;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.services.resource.RealmResourceProvider;

public class SsfRealmResourceProvider implements RealmResourceProvider {

    protected static final Logger log = Logger.getLogger(SsfRealmResourceProvider.class);

    @Override
    public Object getResource() {
        return this;
    }

    /**
     * $ISSUER/ssf/push
     *
     * @return
     */
    @Path("/push")
    public SsfPushDeliveryResource pushEndpoint() {
        // push endpoint authentication checked by PushEndpoit directly.
        return Ssf.currentSsfProvider().pushDeliveryEndpoint();
    }


    @Override
    public void close() {
        // NOOP
    }

}
