package org.keycloak.services.resources;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserCredentialModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/saas")
public class SaasService {
    protected static final Logger logger = Logger.getLogger(SaasService.class);
    public static final String REALM_CREATOR_ROLE = "realm-creator";
    public static final String SAAS_IDENTITY_COOKIE = "KEYCLOAK_SAAS_IDENTITY";

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpRequest request;

    @Context
    HttpResponse response;

    protected String saasLoginPath = "/saas/saas-login.jsp";
    protected String saasRegisterPath = "/saas/saas-register.jsp";
    protected String adminPath = "/saas/admin/index.html";
    protected AuthenticationManager authManager = new AuthenticationManager();

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

    @Path("whoami")
    @GET
    @Produces("application/json")
    public Response whoAmI(final @Context HttpHeaders headers) {
        return new Transaction() {
            @Override
            public Response callImpl()
            {
                logger.info("WHOAMI start.");
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                if (realm == null) throw new NotFoundException();
                UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
                if (user == null) {
                    return Response.status(404).build();
                }
                logger.info("WHOAMI: " + user.getLoginName());
                return Response.ok(new WhoAmI(user.getLoginName(), user.getLoginName())).build();
            }
        }.call();
    }

    @Path("isLoggedIn.js")
    @GET
    @Produces("application/javascript")
    public String isLoggedIn(final @Context HttpHeaders headers) {
        return new Transaction() {
            @Override
            public String callImpl()
            {
                logger.info("WHOAMI Javascript start.");
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                if (realm == null) {
                    return "var keycloakCookieLoggedIn = false;";

                }
                UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
                if (user == null) {
                    return "var keycloakCookieLoggedIn = false;";
                }
                logger.info("WHOAMI: " + user.getLoginName());
                return "var keycloakCookieLoggedIn = true;";
            }
        }.call();
    }


    public static UriBuilder contextRoot(UriInfo uriInfo) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("/auth-server");
    }

    public static UriBuilder saasCookiePath(UriInfo uriInfo) {
        return contextRoot(uriInfo).path("rest").path(SaasService.class);
    }



    @Path("logout")
    @GET
    public void logout() {
        new Transaction() {
            @Override
            protected void runImpl() {
                authManager.expireSaasIdentityCookie(uriInfo);
                request.forward(saasLoginPath);
            }
        }.run();
    }

    @Path("logout-cookie")
    @GET
    public void logoutCookie() {
        logger.info("*** logoutCookie");
        new Transaction() {
            @Override
            protected void runImpl() {
                authManager.expireSaasIdentityCookie(uriInfo);
            }
        }.run();
    }


    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void processLogin(final MultivaluedMap<String, String> formData) {
        logger.info("processLogin start");
        new Transaction() {
            @Override
            protected void runImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                if (realm == null) throw new NotFoundException();

                if (!realm.isEnabled()) {
                    throw new NotImplementedYetException();
                }
                String username = formData.getFirst("username");
                UserModel user = realm.getUser(username);
                if (user == null) {
                    logger.info("Not Authenticated! Incorrect user name");
                    request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Incorrect user name.");
                    request.forward(saasLoginPath);
                    return;
                }
                if (!user.isEnabled()) {
                    logger.info("NAccount is disabled, contact admin.");
                    request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Account is disabled, contact admin.");
                    request.forward(saasLoginPath);
                    return;
                }

                boolean authenticated = authManager.authenticateForm(realm, user, formData);
                if (!authenticated) {
                    logger.info("Not Authenticated! Invalid credentials");
                    request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Invalid credentials.");
                    request.forward(saasLoginPath);
                    return;
                }

                NewCookie cookie = authManager.createSaasIdentityCookie(realm, user, uriInfo);
                response.addNewCookie(cookie);
                request.forward(adminPath);
            }
        }.run();
    }

    @Path("registrations")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(final UserRepresentation newUser) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel defaultRealm = realmManager.defaultRealm();
                UserModel user = registerMe(defaultRealm, newUser);
                if (user == null) {
                    return Response.status(400).type("text/plain").entity("Already exists").build();
                }
                URI uri = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(user.getLoginName()).build();
                return Response.created(uri).build();
            }
        }.call();
    }

    @Path("registrations")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processRegister(final @FormParam("name") String name,
                                    final @FormParam("email") String email,
                                    final @FormParam("username") String username,
                                    final @FormParam("password") String password,
                                    final @FormParam("password-confirm") String confirm) {
        if (!password.equals(confirm)) {
            request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Password confirmation doesn't match.");
            request.forward(saasRegisterPath);
            return null;
        }
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel defaultRealm = realmManager.defaultRealm();
                UserRepresentation newUser = new UserRepresentation();
                newUser.setUsername(username);
                newUser.credential(RequiredCredentialRepresentation.PASSWORD, password, false);
                UserModel user = registerMe(defaultRealm, newUser);
                if (user == null) {
                    request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Username already exists.");
                    request.forward(saasRegisterPath);
                    return null;

                }
                NewCookie cookie = authManager.createSaasIdentityCookie(defaultRealm, user, uriInfo);
                return Response.status(302).location(contextRoot(uriInfo).path(adminPath).build()).cookie(cookie).build();
            }
        }.call();
    }


    protected UserModel registerMe(RealmModel defaultRealm, UserRepresentation newUser) {
        if (!defaultRealm.isEnabled()) {
            throw new ForbiddenException();
        }
        if (!defaultRealm.isRegistrationAllowed()) {
            throw new ForbiddenException();
        }
        UserModel user = defaultRealm.getUser(newUser.getUsername());
        if (user != null) {
            return null;
        }

        user = defaultRealm.addUser(newUser.getUsername());
        for (CredentialRepresentation cred : newUser.getCredentials()) {
            UserCredentialModel credModel = new UserCredentialModel();
            credModel.setType(cred.getType());
            credModel.setValue(cred.getValue());
            defaultRealm.updateCredential(user, credModel);
        }
        RoleModel realmCreator = defaultRealm.getRole(REALM_CREATOR_ROLE);
        defaultRealm.grantRole(user, realmCreator);
        return user;
    }


}
