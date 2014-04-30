package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.models.Config;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeLoader;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/admin")
public class AdminResource {

    private static final Logger logger = Logger.getLogger(AdminResource.class);

    private static FileTypeMap mimeTypes = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @Context
    private UriInfo uriInfo;

    @GET
    public Response getResource() throws URISyntaxException {
        String requestUri = uriInfo.getRequestUri().toString();
        if (!requestUri.endsWith("/")) {
            return Response.seeOther(new URI(requestUri + "/")).build();
        } else {
            return getResource("index.html");
        }
    }

    @GET
    @Path("/{path:.*}")
    public Response getResource(@PathParam("path") String path) {
        try {
            Theme theme = ThemeLoader.createTheme(Config.getThemeAdmin(), Theme.Type.ADMIN);
            InputStream resource = theme.getResourceAsStream(path);
            if (resource != null) {
                return Response.ok(resource).type(mimeTypes.getContentType(path)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get theme resource", e);
            return Response.serverError().build();
        }
    }

}
