package org.keycloak.services.resources;

import org.keycloak.Config;
import org.keycloak.Version;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Get keycloak.js file for javascript clients
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/js")
public class JsResource {

    /**
     * Get keycloak.js file for javascript clients
     *
     * @return
     */
    @GET
    @Path("/keycloak.js")
    @Produces("text/javascript")
    public Response getJs() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("keycloak.js");
        if (inputStream != null) {
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoTransform(false);
            cacheControl.setMaxAge(Config.scope("theme").getInt("staticMaxAge", -1));

            return Response.ok(inputStream).type("text/javascript").cacheControl(cacheControl).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{version}/keycloak.js")
    @Produces("text/javascript")
    public Response getJsWithVersion(@PathParam("version") String version) {
        if (!version.equals(Version.RESOURCES_VERSION)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return getJs();
    }

    @GET
    @Path("/keycloak.min.js")
    @Produces("text/javascript")
    public Response getMinJs() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("keycloak.min.js");
        if (inputStream != null) {
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoTransform(false);
            cacheControl.setMaxAge(Config.scope("theme").getInt("staticMaxAge", -1));

            return Response.ok(inputStream).type("text/javascript").cacheControl(cacheControl).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{version}/keycloak.min.js")
    @Produces("text/javascript")
    public Response getMinJsWithVersion(@PathParam("version") String version) {
        if (!version.equals(Version.RESOURCES_VERSION)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return getMinJs();
    }

}
