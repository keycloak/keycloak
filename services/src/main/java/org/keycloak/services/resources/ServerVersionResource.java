package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.Version;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/version")
public class ServerVersionResource {

    protected static final Logger logger = Logger.getLogger(ServerVersionResource.class);

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersionPreflight() {
        logger.debugv("cors request from: {0}", request.getHttpHeaders().getRequestHeaders().getFirst("Origin"));
        return Cors.add(request, Response.ok()).allowedMethods("GET").auth().preflight().build();
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Version getVersion() {
        Cors.add(request).allowedOrigins("*").allowedMethods("GET").auth().build(response);
        return Version.SINGLETON;
    }
}
