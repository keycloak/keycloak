package org.keycloak.services.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/")
public class WelcomeResource {

    @Context
    private UriInfo uriInfo;

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Produces("text/html")
    public Response getWelcomePage() throws URISyntaxException {
        String requestUri = uriInfo.getRequestUri().toString();
        if (!requestUri.endsWith("/")) {
            return Response.seeOther(new URI(requestUri + "/")).build();
        } else {
            return getResource("index.html");
        }
    }

    /**
     * Resources for welcome page
     *
     * @param name
     * @return
     */
    @GET
    @Path("/welcome-content/{name}")
    @Produces("text/html")
    public Response getResource(@PathParam("name") String name) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("welcome-content/" + name);
        if (inputStream != null) {
            return Response.ok(inputStream).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

}
