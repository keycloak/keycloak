package org.keycloak.services.resources.admin;

import org.codehaus.jackson.annotate.JsonProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.OAuth2Constants;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeLoader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Config;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.TokenService;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthRedirect;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdminConsole {
    protected static final Logger logger = Logger.getLogger(AdminConsole.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @Context
    protected KeycloakSession session;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    @Context
    protected Providers providers;

    @Context
    protected ProviderSession providerSession;

    @Context
    protected KeycloakApplication keycloak;

    protected AppAuthManager authManager;
    protected RealmModel realm;

    public AdminConsole(RealmModel realm) {
        this.realm = realm;
        this.authManager = new AppAuthManager(providerSession);
    }

    public static class WhoAmI {
        protected String userId;
        protected String realm;
        protected String displayName;

        @JsonProperty("createRealm")
        protected boolean createRealm;
        @JsonProperty("realm_access")
        protected Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();

        public WhoAmI() {
        }

        public WhoAmI(String userId, String realm, String displayName, boolean createRealm, Map<String, Set<String>> realmAccess) {
            this.userId = userId;
            this.realm = realm;
            this.displayName = displayName;
            this.createRealm = createRealm;
            this.realmAccess = realmAccess;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isCreateRealm() {
            return createRealm;
        }

        public void setCreateRealm(boolean createRealm) {
            this.createRealm = createRealm;
        }

        public Map<String, Set<String>> getRealmAccess() {
            return realmAccess;
        }

        public void setRealmAccess(Map<String, Set<String>> realmAccess) {
            this.realmAccess = realmAccess;
        }
    }

    @Path("config")
    @GET
    @Produces("application/json")
    @NoCache
    public ApplicationManager.InstallationAdapterConfig config() {
        ApplicationModel consoleApp = realm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
        if (consoleApp == null) {
            throw new NotFoundException("Could not find admin console application");
        }
        return new ApplicationManager().toInstallationRepresentation(realm, consoleApp, keycloak.getBaseUri(uriInfo));

    }

    @Path("whoami")
    @GET
    @Produces("application/json")
    @NoCache
    public Response whoAmI(final @Context HttpHeaders headers) {
        RealmManager realmManager = new RealmManager(session);
        UserModel user = authManager.authenticateBearerToken(realm, uriInfo, headers);
        if (user == null) {
            return Response.status(401).build();
        }
        String displayName;
        if (user.getFirstName() != null || user.getLastName() != null) {
            displayName = user.getFirstName();
            if (user.getLastName() != null) {
                displayName = displayName != null ? displayName + " " + user.getLastName() : user.getLastName();
            }
        } else {
            displayName = user.getLoginName();
        }

        RealmModel masterRealm = getAdminstrationRealm(realmManager);
        if (masterRealm == null)
            throw new NotFoundException("No realm found");
        boolean createRealm = false;
        if (realm.equals(masterRealm)) {
            createRealm = masterRealm.hasRole(user, masterRealm.getRole(AdminRoles.CREATE_REALM));
        }

        Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();
        addRealmAdminAccess(realmAccess, realm.getRoleMappings(user));

        return Response.ok(new WhoAmI(user.getId(), realm.getName(), displayName, createRealm, realmAccess)).build();
    }

    private void addRealmAdminAccess(Map<String, Set<String>> realmAdminAccess, Set<RoleModel> roles) {
        for (RoleModel r : roles) {
            if (r.getContainer() instanceof ApplicationModel) {
                ApplicationModel app = (ApplicationModel) r.getContainer();
                if (app.getName().endsWith(AdminRoles.APP_SUFFIX)) {
                    String realm = app.getName().substring(0, app.getName().length() - AdminRoles.APP_SUFFIX.length());
                    if (!realmAdminAccess.containsKey(realm)) {
                        realmAdminAccess.put(realm, new HashSet<String>());
                    }
                    realmAdminAccess.get(realm).add(r.getName());
                }
            }

            if (r.isComposite()) {
                addRealmAdminAccess(realmAdminAccess, r.getComposites());
            }
        }
    }




    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        URI redirect = AdminRoot.adminConsoleUrl(uriInfo).path("index.html").build(realm.getName());

        return Response.status(302).location(
                TokenService.logoutUrl(uriInfo).queryParam("redirect_uri", redirect.toString()).build(realm.getName())
        ).build();
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }

    private static FileTypeMap mimeTypes = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @GET
    public Response getMainPage() throws URISyntaxException {
        return Response.status(302).location(
                AdminRoot.adminConsoleUrl(uriInfo).path("index.html").build(realm.getName())
        ).build();
    }

    @GET
    @Path("js/keycloak.js")
    @Produces("text/javascript")
    public Response getKeycloakJs() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("keycloak.js");
        if (inputStream != null) {
            return Response.ok(inputStream).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    @GET
    @Path("{path:.+}")
    public Response getResource(@PathParam("path") String path) {
        try {
            Theme theme = ThemeLoader.createTheme(Config.getThemeAdmin(), Theme.Type.ADMIN);
            InputStream resource = theme.getResourceAsStream(path);
            if (resource != null) {
                String contentType = mimeTypes.getContentType(path);
                return Response.ok(resource).type(contentType).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get theme resource", e);
            return Response.serverError().build();
        }
    }

}
