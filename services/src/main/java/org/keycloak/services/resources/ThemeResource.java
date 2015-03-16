package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Version;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.MimeTypeUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Theme resource
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/resources")
public class ThemeResource {

    private static final Logger logger = Logger.getLogger(ThemeResource.class);

    @Context
    private KeycloakSession session;

    /**
     * Get theme content
     *
     * @param themType
     * @param themeName
     * @param path
     * @return
     */
    @GET
    @Path("/{version}/{themeType}/{themeName}/{path:.*}")
    public Response getResource(@PathParam("version") String version, @PathParam("themeType") String themType, @PathParam("themeName") String themeName, @PathParam("path") String path) {
        if (!version.equals(Version.RESOURCES_VERSION)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
            Theme theme = themeProvider.getTheme(themeName, Theme.Type.valueOf(themType.toUpperCase()));
            InputStream resource = theme.getResourceAsStream(path);
            if (resource != null) {
                CacheControl cacheControl = new CacheControl();
                cacheControl.setNoTransform(false);
                cacheControl.setMaxAge(Config.scope("theme").getInt("staticMaxAge", -1));

                return Response.ok(resource).type(MimeTypeUtil.getContentType(path)).cacheControl(cacheControl).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get theme resource", e);
            return Response.serverError().build();
        }
    }

}
