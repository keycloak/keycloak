package org.keycloak.example.basicauth;

import org.jboss.resteasy.annotations.cache.NoCache;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("service")
public class BasicAuthService {
    @GET
    @NoCache
    @Path("echo")
    public String echo(@QueryParam("value") String value) {
        return value;
    }
}
