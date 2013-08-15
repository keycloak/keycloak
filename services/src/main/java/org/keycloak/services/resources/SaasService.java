package org.keycloak.services.resources;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.net.URI;
import java.util.StringTokenizer;

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

    @Path("keepalive")
    @GET
    @NoCache
    public Response keepalive(final @Context HttpHeaders headers) {
        logger.info("keepalive");
        return new Transaction() {
            @Override
            public Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                if (realm == null)
                    throw new NotFoundException();
                UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
                if (user == null) {
                    return Response.status(401).build();
                }
                NewCookie refreshCookie = authManager.createSaasIdentityCookie(realm, user, uriInfo);
                return Response.noContent().cookie(refreshCookie).build();
            }
        }.call();
    }

    @Path("whoami")
    @GET
    @Produces("application/json")
    @NoCache
    public Response whoAmI(final @Context HttpHeaders headers) {
        return new Transaction() {
            @Override
            public Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                if (realm == null)
                    throw new NotFoundException();
                UserModel user = authManager.authenticateSaasIdentityCookie(realm, uriInfo, headers);
                if (user == null) {
                    return Response.status(401).build();
                }
                return Response.ok(new WhoAmI(user.getLoginName(), user.getFirstName() + " " + user.getLastName())).build();
            }
        }.call();
    }

    @Path("isLoggedIn.js")
    @GET
    @Produces("application/javascript")
    @NoCache
    public String isLoggedIn(final @Context HttpHeaders headers) {
        return new Transaction() {
            @Override
            public String callImpl() {
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

    @Path("admin/realms")
    public RealmsAdminResource getRealmsAdmin(@Context final HttpHeaders headers) {
        return new Transaction(false) {
            @Override
            protected RealmsAdminResource callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel saasRealm = realmManager.defaultRealm();
                if (saasRealm == null)
                    throw new NotFoundException();
                UserModel admin = authManager.authenticateSaasIdentity(saasRealm, uriInfo, headers);
                if (admin == null) {
                    throw new NotAuthorizedException("Bearer");
                }
                RoleModel creatorRole = saasRealm.getRole(SaasService.REALM_CREATOR_ROLE);
                if (!saasRealm.hasRole(admin, creatorRole)) {
                    logger.warn("not a Realm creator");
                    throw new NotAuthorizedException("Bearer");
                }
                return new RealmsAdminResource(admin);
            }
        }.call();
    }

    @Path("login")
    @GET
    @NoCache
    public void loginPage() {
        new Transaction() {
            @Override
            protected void runImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                authManager.expireSaasIdentityCookie(uriInfo);

                Flows.forms(realm, request).forwardToLogin();
            }
        }.run();
    }

    @Path("registrations")
    @GET
    @NoCache
    public void registerPage() {
        new Transaction() {
            @Override
            protected void runImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                authManager.expireSaasIdentityCookie(uriInfo);

                Flows.forms(realm, request).forwardToRegistration();
            }
        }.run();
    }

    @Path("logout")
    @GET
    @NoCache
    public void logout() {
        new Transaction() {
            @Override
            protected void runImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                authManager.expireSaasIdentityCookie(uriInfo);

                Flows.forms(realm, request).forwardToLogin();
            }
        }.run();
    }

    @Path("logout-cookie")
    @GET
    @NoCache
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
    public Response processLogin(final MultivaluedMap<String, String> formData) {
        logger.info("processLogin start");
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.defaultRealm();
                if (realm == null)
                    throw new NotFoundException();

                if (!realm.isEnabled()) {
                    throw new NotImplementedYetException();
                }
                String username = formData.getFirst("username");
                UserModel user = realm.getUser(username);
                if (user == null) {
                    logger.info("Not Authenticated! Incorrect user name");

                    return Flows.forms(realm, request).setError("Invalid username or password").setFormData(formData)
                            .forwardToLogin();
                }
                if (!user.isEnabled()) {
                    logger.info("NAccount is disabled, contact admin.");

                    return Flows.forms(realm, request).setError("Invalid username or password")
                            .setFormData(formData).forwardToLogin();
                }

                boolean authenticated = authManager.authenticateForm(realm, user, formData);
                if (!authenticated) {
                    logger.info("Not Authenticated! Invalid credentials");

                    return Flows.forms(realm, request).setError("Invalid username or password").setFormData(formData)
                            .forwardToLogin();
                }

                NewCookie cookie = authManager.createSaasIdentityCookie(realm, user, uriInfo);
                return Response.status(302).cookie(cookie).location(contextRoot(uriInfo).path(adminPath).build()).build();
            }
        }.call();
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
    public Response processRegister(final MultivaluedMap<String, String> formData) {
        return new Transaction() {
            @Override
            protected Response callImpl() {
                RealmManager realmManager = new RealmManager(session);
                RealmModel defaultRealm = realmManager.defaultRealm();

                String error = validateRegistrationForm(formData);
                if (error != null) {
                    return Flows.forms(defaultRealm, request).setError(error).setFormData(formData)
                            .forwardToRegistration();
                }

                UserRepresentation newUser = new UserRepresentation();
                newUser.setUsername(formData.getFirst("username"));
                newUser.setEmail(formData.getFirst("email"));

                String fullname = formData.getFirst("name");
                if (fullname != null) {
                    StringTokenizer tokenizer = new StringTokenizer(fullname, " ");
                    StringBuffer first = null;
                    String last = "";
                    while (tokenizer.hasMoreTokens()) {
                        String token = tokenizer.nextToken();
                        if (tokenizer.hasMoreTokens()) {
                            if (first == null) {
                                first = new StringBuffer();
                            } else {
                                first.append(" ");
                            }
                            first.append(token);
                        } else {
                            last = token;
                        }
                    }
                    if (first == null)
                        first = new StringBuffer();
                    newUser.setFirstName(first.toString());
                    newUser.setLastName(last);
                }
                newUser.credential(CredentialRepresentation.PASSWORD, formData.getFirst("password"));
                UserModel user = registerMe(defaultRealm, newUser);
                if (user == null) {
                    return Flows.forms(defaultRealm, request).setError("Username already exists.")
                            .setFormData(formData).forwardToRegistration();

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
        user.setFirstName(newUser.getFirstName());
        user.setLastName(newUser.getLastName());
        user.setEmail(newUser.getEmail());
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

    private String validateRegistrationForm(MultivaluedMap<String, String> formData) {
        if (isEmpty(formData.getFirst("name"))) {
            return "Please specify full name";
        }

        if (isEmpty(formData.getFirst("email"))) {
            return "Please specify email";
        }

        if (isEmpty(formData.getFirst("username"))) {
            return "Please specify username";
        }

        if (isEmpty(formData.getFirst("password"))) {
            return "Please specify password";
        }

        if (!formData.getFirst("password").equals(formData.getFirst("password-confirm"))) {
            return "Password confirmation doesn't match.";
        }

        return null;
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}
