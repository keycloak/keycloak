package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.jaxrs.JaxrsOAuthClient;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.TokenService;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/admin")
public class AdminService {
    protected static final Logger logger = Logger.getLogger(AdminService.class);
    public static final String REALM_CREATOR_ROLE = "realm-creator";
    public static final String SAAS_IDENTITY_COOKIE = "KEYCLOAK_SAAS_IDENTITY";

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
    protected AuthenticationManager authManager = new AuthenticationManager();
    protected TokenManager tokenManager;

    public AdminService(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public static class WhoAmI {
        protected String userId;
        protected String displayName;

        public WhoAmI() {
        }

        public WhoAmI(String userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
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
        UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
        if (user == null) {
            return Response.status(401).build();
        }
        NewCookie refreshCookie = authManager.createSaasIdentityCookie(realm, user, uriInfo);
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
        UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
        if (user == null) {
            return Response.status(401).build();
        }
        // keycloak is bootstrapped with an admin user with no first/last name, so use login name as display name
        return Response.ok(new WhoAmI(user.getLoginName(), user.getLoginName())).build();
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
        UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
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
        RealmModel saasRealm = getAdminstrationRealm(realmManager);
        if (saasRealm == null)
            throw new NotFoundException();
        UserModel admin = authManager.authenticateSaasIdentity(saasRealm, uriInfo, headers);
        if (admin == null) {
            throw new NotAuthorizedException("Bearer");
        }
        ApplicationModel adminConsole = saasRealm.getApplicationNameMap().get(Constants.ADMIN_CONSOLE_APPLICATION);
        if (adminConsole == null) {
            throw new NotFoundException();
        }
        RoleModel adminRole = adminConsole.getRole(Constants.ADMIN_CONSOLE_ADMIN_ROLE);
        if (!adminConsole.hasRole(admin, adminRole)) {
            logger.warn("not a Realm admin");
            throw new NotAuthorizedException("Bearer");
        }
        RealmsAdminResource adminResource = new RealmsAdminResource(admin);
        resourceContext.initResource(adminResource);
        return adminResource;
    }

    @Path("login")
    @GET
    @NoCache
    public Response loginPage(@QueryParam("path") String path) {
        logger.debug("loginPage ********************** <---");
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        authManager.expireSaasIdentityCookie(uriInfo);

        JaxrsOAuthClient oauth = new JaxrsOAuthClient();
        String authUrl = TokenService.loginPageUrl(uriInfo).build(Constants.ADMIN_REALM).toString();
        logger.debug("authUrl: {0}", authUrl);
        oauth.setAuthUrl(authUrl);
        oauth.setClientId(Constants.ADMIN_CONSOLE_APPLICATION);
        URI redirectUri = uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "loginRedirect").build();
        logger.debug("redirectUri: {0}", redirectUri.toString());
        oauth.setStateCookiePath(redirectUri.getRawPath());
        return oauth.redirect(uriInfo, redirectUri.toString(), path);
    }

    @Path("login-redirect")
    @GET
    @NoCache
    public Response loginRedirect(@QueryParam("code") String code,
                                  @QueryParam("state") String state,
                                  @QueryParam("error") String error,
                                  @Context HttpHeaders headers

                                  ) {
        try {
            logger.info("loginRedirect ********************** <---");
            if (error != null) {
                logger.debug("error from oauth");
                throw new ForbiddenException("error");
            }
            RealmManager realmManager = new RealmManager(session);
            RealmModel realm = getAdminstrationRealm(realmManager);
            if (!realm.isEnabled()) {
                logger.debug("realm not enabled");
                throw new ForbiddenException();
            }
            ApplicationModel adminConsole = realm.getApplicationNameMap().get(Constants.ADMIN_CONSOLE_APPLICATION);
            UserModel adminConsoleUser = adminConsole.getApplicationUser();
            if (!adminConsole.isEnabled() || !adminConsoleUser.isEnabled()) {
                logger.debug("admin app not enabled");
                throw new ForbiddenException();
            }

            if (code == null) {
                logger.debug("code not specified");
                throw new BadRequestException();
            }
            if (state == null) {
                logger.debug("state not specified");
                throw new BadRequestException();
            }
            String path = new JaxrsOAuthClient().checkStateCookie(uriInfo, headers);

            JWSInput input = new JWSInput(code);
            boolean verifiedCode = false;
            try {
                verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
            } catch (Exception ignored) {
                logger.debug("Failed to verify signature", ignored);
            }
            if (!verifiedCode) {
                logger.debug("unverified access code");
                throw new BadRequestException();
            }
            String key = input.readContentAsString();
            AccessCodeEntry accessCode = tokenManager.pullAccessCode(key);
            if (accessCode == null) {
                logger.debug("bad access code");
                throw new BadRequestException();
            }
            if (accessCode.isExpired()) {
                logger.debug("access code expired");
                throw new BadRequestException();
            }
            if (!accessCode.getToken().isActive()) {
                logger.debug("access token expired");
                throw new BadRequestException();
            }
            if (!accessCode.getRealm().getId().equals(realm.getId())) {
                logger.debug("bad realm");
                throw new BadRequestException();

            }
            if (!adminConsoleUser.getLoginName().equals(accessCode.getClient().getLoginName())) {
                logger.debug("bad client");
                throw new BadRequestException();
            }
            if (!adminConsole.hasRole(accessCode.getUser(), Constants.ADMIN_CONSOLE_ADMIN_ROLE)) {
                logger.debug("not allowed");
                throw new ForbiddenException();
            }
            logger.debug("loginRedirect SUCCESS");
            NewCookie cookie = authManager.createSaasIdentityCookie(realm, accessCode.getUser(), uriInfo);

            URI redirectUri = contextRoot(uriInfo).path(adminPath).build();
            if (path != null) {
                redirectUri = redirectUri.resolve("#" + UriBuilder.fromPath(path).build().toString());
            }
            return Response.status(302).cookie(cookie).location(redirectUri).build();
        } finally {
            authManager.expireCookie(AbstractOAuthClient.OAUTH_TOKEN_REQUEST_STATE, uriInfo.getAbsolutePath().getPath());
        }
    }

    @Path("logout")
    @GET
    @NoCache
    public Response logout() {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        authManager.expireSaasIdentityCookie(uriInfo);
        authManager.expireIdentityCookie(realm, uriInfo);

        return Response.status(302).location(uriInfo.getBaseUriBuilder().path(AdminService.class).path(AdminService.class, "loginPage").build()).build();
    }

    @Path("logout-cookie")
    @GET
    @NoCache
    public void logoutCookie() {
        logger.debug("*** logoutCookie");
        authManager.expireSaasIdentityCookie(uriInfo);
    }

    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processLogin(final MultivaluedMap<String, String> formData) {
        logger.debug("processLogin start");
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = getAdminstrationRealm(realmManager);
        if (realm == null)
            throw new NotFoundException();
        ApplicationModel adminConsole = realm.getApplicationNameMap().get(Constants.ADMIN_CONSOLE_APPLICATION);
        UserModel adminConsoleUser = adminConsole.getApplicationUser();

        if (!realm.isEnabled()) {
            throw new NotImplementedYetException();
        }
        String username = formData.getFirst("username");
        UserModel user = realm.getUser(username);

        AuthenticationStatus status = authManager.authenticateForm(realm, user, formData);

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        switch (status) {
            case SUCCESS:
                NewCookie cookie = authManager.createSaasIdentityCookie(realm, user, uriInfo);
                return Response.status(302).cookie(cookie).location(contextRoot(uriInfo).path(adminPath).build()).build();
            case ACCOUNT_DISABLED:
                return Flows.forms(realm, request, uriInfo).setError(Messages.ACCOUNT_DISABLED).setFormData(formData)
                        .forwardToLogin();
            case ACTIONS_REQUIRED:
                return oauth.processAccessCode(null, "n", contextRoot(uriInfo).path(adminPath).build().toString(), adminConsoleUser, user);
            default:
                return Flows.forms(realm, request, uriInfo).setError(Messages.INVALID_USER).setFormData(formData)
                        .forwardToLogin();
        }
    }

    protected RealmModel getAdminstrationRealm(RealmManager realmManager) {
        return realmManager.getKeycloakAdminstrationRealm();
    }
}
