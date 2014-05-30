package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.freemarker.ExtendingThemeManager;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.provider.ProviderSession;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
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
@Path("/theme")
public class ThemeResource {

    private static final Logger logger = Logger.getLogger(ThemeResource.class);

    private static FileTypeMap mimeTypes = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @Context
    private ProviderSession providerSession;

    /**
     * Get theme content
     *
     * @param themType
     * @param themeName
     * @param path
     * @return
     */
    @GET
    @Path("/{themeType}/{themeName}/{path:.*}")
    public Response getResource(@PathParam("themeType") String themType, @PathParam("themeName") String themeName, @PathParam("path") String path) {
        try {
            ExtendingThemeManager themeManager = new ExtendingThemeManager(providerSession);
            Theme theme = themeManager.createTheme(themeName, Theme.Type.valueOf(themType.toUpperCase()));
            InputStream resource = theme.getResourceAsStream(path);
            if (resource != null) {
                CacheControl cacheControl = new CacheControl();
                cacheControl.setNoTransform(false);
                cacheControl.setMaxAge(themeManager.getStaticMaxAge());

                return Response.ok(resource).type(mimeTypes.getContentType(path)).cacheControl(cacheControl).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get theme resource", e);
            return Response.serverError().build();
        }
    }

}
