package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.common.util.MimeTypeUtil;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.util.CacheControlUtil;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/")
public class WelcomeResource {

    private static final Logger logger = Logger.getLogger(WelcomeResource.class);

    private boolean bootstrap;

    @Context
    private UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public WelcomeResource(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    /**
     * Welcome page of Keycloak
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @Produces("text/html")
    public Response getWelcomePage() throws URISyntaxException {
        checkBootstrap();

        String requestUri = uriInfo.getRequestUri().toString();
        if (!requestUri.endsWith("/")) {
            return Response.seeOther(new URI(requestUri + "/")).build();
        } else {
            return createWelcomePage(null, null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUser(final MultivaluedMap<String, String> formData) {
        checkBootstrap();

        if (!bootstrap) {
            return createWelcomePage(null, null);
        } else {
            if (!isLocal()) {
                logger.errorv("Rejected non-local attempt to create initial user from {0}", session.getContext().getConnection().getRemoteAddr());
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            String username = formData.getFirst("username");
            String password = formData.getFirst("password");
            String passwordConfirmation = formData.getFirst("passwordConfirmation");

            if (username == null || username.length() == 0) {
                return createWelcomePage(null, "Username is missing");
            }

            if (password == null || password.length() == 0) {
                return createWelcomePage(null, "Password is missing");
            }

            if (!password.equals(passwordConfirmation)) {
                return createWelcomePage(null, "Password and confirmation doesn't match");
            }

            ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
            if (applianceBootstrap.isNoMasterUser()) {
                bootstrap = false;
                applianceBootstrap.createMasterRealmUser(session, username, password);

                logger.infov("Created initial admin user with username {0}", username);
                return createWelcomePage("User created", null);
            } else {
                logger.warnv("Rejected attempt to create initial user as user is already created");
                return createWelcomePage(null, "Users already exists");
            }
        }
    }

    private Response createWelcomePage(String successMessage, String errorMessage) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("bootstrap", bootstrap);
            if (bootstrap) {
                map.put("localUser", isLocal());
            }
            if (successMessage != null) {
                map.put("successMessage", successMessage);
            }
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }
            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", getTheme());
            return Response.status(errorMessage == null ? Response.Status.OK : Response.Status.BAD_REQUEST).entity(result).cacheControl(CacheControlUtil.noCache()).build();
        } catch (Exception e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkBootstrap() {
        if (bootstrap) {
            bootstrap  = new ApplianceBootstrap(session).isNoMasterUser();
        }
    }

    private boolean isLocal() {
        return session.getContext().getUri().getRequestUri().getHost().equals("localhost");
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
            InputStream resource = getTheme().getResourceAsStream(path);
            if (resource != null) {
                String contentType = MimeTypeUtil.getContentType(path);
                Response.ResponseBuilder builder = Response.ok(resource).type(contentType).cacheControl(CacheControlUtil.getDefaultCacheControl());
                return builder.build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Theme getTheme() {
        Config.Scope config = Config.scope("theme");
        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        try {
            return themeProvider.getTheme(config.get("welcomeTheme"), Theme.Type.WELCOME);
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
