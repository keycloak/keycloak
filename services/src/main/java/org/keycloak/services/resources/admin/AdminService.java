package org.keycloak.services.resources.admin;

import org.codehaus.jackson.annotate.JsonProperty;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.jaxrs.JaxrsOAuthClient;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.TokenService;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/admin")
public class AdminService {
    protected static final Logger logger = Logger.getLogger(AdminService.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpRequest request;

    @Context
    protected HttpResponse response;

    @Context
    protected KeycloakSession session;

    @Context
    protected ResourceContext resourceContext;

    @Context
    protected Providers providers;

    protected String adminPath = "/admin/index.html";
    protected AppAuthManager authManager;
    protected TokenManager tokenManager;

    public AdminService(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.authManager = new AppAuthManager("KEYCLOAK_ADMIN_CONSOLE_IDENTITY", tokenManager);
    }

    public static UriBuilder adminApiUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "getRealmsAdmin").path(RealmsAdminResource.class, "getRealmAdmin");
        return base;
    }


    public static class WhoAmI {
        protected String userId;
        protected String displayName;

        @JsonProperty("createRealm")
        protected boolean createRealm;
        @JsonProperty("realm_access")
        protected Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();

        public WhoAmI() {
        }

        public WhoAmI(String userId, String displayName, boolean createRealm, Map<String, Set<String>> realmAccess) {
            this.userId = userId;
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

    @Path("keepalive")
    @GET
    @NoCache
    public Response keepalive(final @Context HttpHeaders headers) {
        logger.debug("keepalive");
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        if (realm == null)
            throw new NotFoundException();
        Auth auth = authManager.authenticateCookie(realm, headers);
        if (auth == null) {
            return Response.status(401).build();
        }
        NewCookie refreshCookie = authManager.createRefreshCookie(realm, auth.getUser(), auth.getClient(), AdminService.saasCookiePath(uriInfo).build());
        return Response.noContent().cookie(refreshCookie).build();
    }

    @Path("whoami")
    @GET
    @Produces("application/json")
    @NoCache
    public Response whoAmI(final @Context HttpHeaders headers) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        if (realm == null)
            throw new NotFoundException();
        Auth auth = authManager.authenticateCookie(realm, headers);
        if (auth == null) {
            logger.debug("No auth cookie");
            return Response.status(401).build();
        }
        UserModel user = auth.getUser();
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

        boolean createRealm = realm.hasRole(user, realm.getRole(AdminRoles.CREATE_REALM));

        Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();
        addRealmAdminAccess(realmAccess, auth.getRealm().getRoleMappings(auth.getUser()));

        return Response.ok(new WhoAmI(user.getId(), displayName, createRealm, realmAccess)).build();
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

        @Path("isLoggedIn.js")
    @GET
    @Produces("application/javascript")
    @NoCache
    public String isLoggedIn(final @Context HttpHeaders headers) {
        logger.debug("WHOAMI Javascript start.");
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        if (realm == null) {
            return "var keycloakCookieLoggedIn = false;";

        }
        UserModel user = authManager.authenticateCookie(realm, headers).getUser();
        if (user == null) {
            return "var keycloakCookieLoggedIn = false;";
        }
        logger.debug("WHOAMI: " + user.getLoginName());
        return "var keycloakCookieLoggedIn = true;";
    }

    public static UriBuilder contextRoot(UriInfo uriInfo) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("/auth");
    }

    public static UriBuilder saasCookiePath(UriInfo uriInfo) {
        return contextRoot(uriInfo).path("rest").path(AdminService.class);
    }

    @Path("realms")
    public RealmsAdminResource getRealmsAdmin(@Context final HttpHeaders headers) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel adminRealm = getAdminstrationRealm(realmManager);
        if (adminRealm == null)
            throw new NotFoundException();
        Auth auth = authManager.authenticate(adminRealm, headers);
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        }

        RealmsAdminResource adminResource = new RealmsAdminResource(auth, tokenManager);
        resourceContext.initResource(adminResource);
        return adminResource;
    }

    @Path("serverinfo")
    public ServerInfoAdminResource getServerInfo(@Context final HttpHeaders headers) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel adminRealm = getAdminstrationRealm(realmManager);
        if (adminRealm == null)
            throw new NotFoundException();
        Auth auth = authManager.authenticate(adminRealm, headers);
        UserModel admin = auth.getUser();
        if (admin == null) {
            throw new NotAuthorizedException("Bearer");
        }
        ApplicationModel adminConsole = adminRealm.getApplicationNameMap().get(Constants.ADMIN_CONSOLE_APPLICATION);
        if (adminConsole == null) {
            throw new NotFoundException();
        }
        ServerInfoAdminResource adminResource = new ServerInfoAdminResource();
        resourceContext.initResource(adminResource);
        return adminResource;
    }

    private void expireCookie() {
        authManager.expireCookie(AdminService.saasCookiePath(uriInfo).build());
    }

    @Path("login")
    @GET
    @NoCache
    public Response loginPage(@QueryParam("path") String path) {
        logger.debug("loginPage ********************** <---");
        expireCookie();

        JaxrsOAuthClient oauth = new JaxrsOAuthClient();
        String authUrl = TokenService.loginPageUrl(uriInfo).build(Constants.ADMIN_REALM).toString();
        logger.debug("authUrl: {0}", authUrl);
        oauth.setAuthUrl(authUrl);
        oauth.setClientId(Constants.ADMIN_CONSOLE_APPLICATION);

        UriBuilder redirectBuilder = uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "loginRedirect");
        if (path != null) {
            redirectBuilder.queryParam("path", path);
        }
        URI redirectUri = redirectBuilder.build();
        logger.debug("redirectUri: {0}", redirectUri.toString());
        oauth.setStateCookiePath(redirectUri.getRawPath());
        return oauth.redirect(uriInfo, redirectUri.toString());
    }

    @Path("login-error")
    @GET
    @NoCache
    public Response errorOnLoginRedirect(@QueryParam ("error") String message) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        return Flows.forms(realm, request, uriInfo).setError(message).createErrorPage();
    }

    protected Response redirectOnLoginError(String message) {
        URI uri = uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "errorOnLoginRedirect").queryParam("error", message).build();
        URI logout = TokenService.logoutUrl(uriInfo).queryParam("redirect_uri", uri.toString()).build(Constants.ADMIN_REALM);
        return Response.status(302).location(logout).build();
    }

    @Path("login-redirect")
    @GET
    @NoCache
    public Response loginRedirect(@QueryParam("code") String code,
                                  @QueryParam("state") String state,
                                  @QueryParam("error") String error,
                                  @QueryParam("path") String path,
                                  @Context HttpHeaders headers

                                  ) {
        try {
            logger.info("loginRedirect ********************** <---");
            if (error != null) {
                logger.debug("error from oauth");
                return redirectOnLoginError(error);
            }
            RealmManager realmManager = new RealmManager(session);
            RealmModel adminRealm = getAdminstrationRealm(realmManager);
            if (!adminRealm.isEnabled()) {
                logger.debug("realm not enabled");
                return redirectOnLoginError("realm not enabled");
            }
            ApplicationModel adminConsole = adminRealm.getApplicationNameMap().get(Constants.ADMIN_CONSOLE_APPLICATION);
            if (!adminConsole.isEnabled()) {
                logger.debug("admin app not enabled");
                return redirectOnLoginError("admin app not enabled");
            }

            if (code == null) {
                logger.debug("code not specified");
                return redirectOnLoginError("invalid login data");
            }
            if (state == null) {
                logger.debug("state not specified");
                return redirectOnLoginError("invalid login data");
            }
            new JaxrsOAuthClient().checkStateCookie(uriInfo, headers);

            logger.debug("loginRedirect SUCCESS");
            NewCookie cookie = authManager.createCookie(adminRealm, adminConsole, code, AdminService.saasCookiePath(uriInfo).build());

            URI redirectUri = contextRoot(uriInfo).path(adminPath).build();
            if (path != null) {
                redirectUri = redirectUri.resolve("#" + UriBuilder.fromPath(path).build().toString());
            }
            return Response.status(302).cookie(cookie).location(redirectUri).build();
        } finally {
            expireCookie();
        }
    }

    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        expireCookie();
        authManager.expireIdentityCookie(realm, uriInfo);

        return Response.status(302).location(uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "loginPage").build()).build();
    }

    @Path("logout-cookie")
    @GET
    @NoCache
    public void logoutCookie() {
        logger.debug("*** logoutCookie");
        expireCookie();
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }
}
