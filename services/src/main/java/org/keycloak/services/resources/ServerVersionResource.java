package org.keycloak.services.resources;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.Version;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/version")
public class ServerVersionResource {

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Version getVersion() {
        return Version.SINGLETON;
    }
}
