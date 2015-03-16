package org.keycloak.services.resources.admin;

import org.codehaus.jackson.annotate.JsonProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.ClientConnection;
import org.keycloak.Config;
import org.keycloak.Version;
import org.keycloak.freemarker.BrowserSecurityHeaderSetup;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.util.MimeTypeUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    protected ClientConnection clientConnection;

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
    protected KeycloakApplication keycloak;

    protected AppAuthManager authManager;
    protected RealmModel realm;

    public AdminConsole(RealmModel realm) {
        this.realm = realm;
        this.authManager = new AppAuthManager();
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

    /**
     * Adapter configuration for the admin console for this realm
     *
     * @return
     */
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

    /**
     * Permission information
     *
     * @param headers
     * @return
     */
    @Path("whoami")
    @GET
    @Produces("application/json")
    @NoCache
    public Response whoAmI(final @Context HttpHeaders headers) {
        RealmManager realmManager = new RealmManager(session);
        AuthenticationManager.AuthResult authResult = authManager.authenticateBearerToken(session, realm, uriInfo, clientConnection, headers);
        if (authResult == null) {
            return Response.status(401).build();
        }
        UserModel user= authResult.getUser();
        String displayName;
        if ((user.getFirstName() != null && !user.getFirstName().trim().equals("")) || (user.getLastName() != null && !user.getLastName().trim().equals(""))) {
            displayName = user.getFirstName();
            if (user.getLastName() != null) {
                displayName = displayName != null ? displayName + " " + user.getLastName() : user.getLastName();
            }
        } else {
            displayName = user.getUsername();
        }

        RealmModel masterRealm = getAdminstrationRealm(realmManager);
        Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();
        if (masterRealm == null)
            throw new NotFoundException("No realm found");
        boolean createRealm = false;
        if (realm.equals(masterRealm)) {
            logger.debug("setting up realm access for a master realm user");
            createRealm = user.hasRole(masterRealm.getRole(AdminRoles.CREATE_REALM));
            addMasterRealmAccess(realm, user, realmAccess);
        } else {
            logger.debug("setting up realm access for a realm user");
            addRealmAccess(realm, user, realmAccess);
        }

        return Response.ok(new WhoAmI(user.getId(), realm.getName(), displayName, createRealm, realmAccess)).build();
    }

    private void addRealmAccess(RealmModel realm, UserModel user, Map<String, Set<String>> realmAdminAccess) {
        RealmManager realmManager = new RealmManager(session);
        ApplicationModel realmAdminApp = realm.getApplicationByName(realmManager.getRealmAdminApplicationName(realm));
        Set<RoleModel> roles = realmAdminApp.getRoles();
        for (RoleModel role : roles) {
            if (!user.hasRole(role)) continue;
            if (!realmAdminAccess.containsKey(realm.getName())) {
                realmAdminAccess.put(realm.getName(), new HashSet<String>());
            }
            realmAdminAccess.get(realm.getName()).add(role.getName());
        }

    }

    private void addMasterRealmAccess(RealmModel masterRealm, UserModel user, Map<String, Set<String>> realmAdminAccess) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            ApplicationModel realmAdminApp = realm.getMasterAdminApp();
            Set<RoleModel> roles = realmAdminApp.getRoles();
            for (RoleModel role : roles) {
                if (!user.hasRole(role)) continue;
                if (!realmAdminAccess.containsKey(realm.getName())) {
                    realmAdminAccess.put(realm.getName(), new HashSet<String>());
                }
                realmAdminAccess.get(realm.getName()).add(role.getName());
            }
        }
    }

    /**
     * Logout from the admin console
     *
     * @return
     */
    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        URI redirect = AdminRoot.adminConsoleUrl(uriInfo).build(realm.getName());

        return Response.status(302).location(
                OIDCLoginProtocolService.logoutUrl(uriInfo).queryParam("redirect_uri", redirect.toString()).build(realm.getName())
        ).build();
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }

    /**
     * Main page of this realm's admin console
     *
     * @return
     * @throws URISyntaxException
     */
    @GET
    @NoCache
    public Response getMainPage() throws URISyntaxException, IOException, FreeMarkerException {
        if (!uriInfo.getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(uriInfo.getRequestUriBuilder().path("/").build()).build();
        } else {
            String adminTheme = realm.getAdminTheme();
            if (adminTheme == null) {
                adminTheme = "keycloak";
            }

            Map<String, String> map = new HashMap<String, String>();

            URI baseUri = uriInfo.getBaseUri();

            String authUrl = baseUri.toString();
            authUrl = authUrl.substring(0, authUrl.length() - 1);

            map.put("authUrl", authUrl);
            map.put("resourceUrl", Urls.themeRoot(baseUri) + "/admin/" + adminTheme);
            map.put("resourceVersion", Version.RESOURCES_VERSION);

            ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
            Theme theme = themeProvider.getTheme(realm.getAdminTheme(), Theme.Type.ADMIN);

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            return builder.build();
        }
    }

    @GET
    @Path("{indexhtml: index.html}") // this expression is a hack to get around jaxdoclet generation bug.  Doesn't like index.html
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(uriInfo.getRequestUriBuilder().path("../").build()).build();
    }

}
