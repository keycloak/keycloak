package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeLoader;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/theme")
public class ThemeResource {

    private static final Logger logger = Logger.getLogger(ThemeResource.class);

    private static FileTypeMap mimeTypes = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @GET
    @Path("/{themType}/{themeName}/{path:.*}")
    public Response getResource(@PathParam("themType") String themType, @PathParam("themeName") String themeName, @PathParam("path") String path) {
        try {
            Theme theme = ThemeLoader.createTheme(themeName, Theme.Type.valueOf(themType.toUpperCase()));
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
