package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
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

    private static final Logger logger = Logger.getLogger(WelcomeResource.class);

    private static FileTypeMap mimeTypes = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @Context
    private UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

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
     * @param path
     * @return
     */
    @GET
    @Path("/welcome-content/{path}")
    @Produces("text/html")
    public Response getResource(@PathParam("path") String path) {
        try {
            Config.Scope config = Config.scope("theme");

            ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
            Theme theme = themeProvider.getTheme(config.get("welcomeTheme"), Theme.Type.WELCOME);
            InputStream resource = theme.getResourceAsStream(path);
            if (resource != null) {
                String contentType = mimeTypes.getContentType(path);

                CacheControl cacheControl = new CacheControl();
                cacheControl.setNoTransform(false);
                cacheControl.setMaxAge(config.getInt("staticMaxAge", -1));

                Response.ResponseBuilder builder = Response.ok(resource).type(contentType).cacheControl(cacheControl);
                return builder.build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get theme resource", e);
            return Response.serverError().build();
        }
    }

}
