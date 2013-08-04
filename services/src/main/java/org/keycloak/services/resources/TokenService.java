package org.keycloak.services.resources;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.jose.jws.JWSBuilder;
import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenService extends AbstractLoginService {


    protected static final Logger logger = Logger.getLogger(TokenService.class);

    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;

    private ResourceAdminManager resourceAdminManager = new ResourceAdminManager();

    public TokenService(RealmModel realm, TokenManager tokenManager) {
        super(realm, tokenManager);
    }

    public static UriBuilder tokenServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder()
                .path(RealmsResource.class).path(RealmsResource.class, "getTokenService");
        return base;
    }

    public static UriBuilder accessCodeToTokenUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "accessCodeToToken");

    }

    public static UriBuilder grantAccessTokenUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "grantAccessToken");

    }

    public static UriBuilder grantIdentityTokenUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "grantIdentityToken");

    }

    public static UriBuilder loginPageUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "loginPage");
    }

    public static UriBuilder processLoginUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "processLogin");
    }

    public static UriBuilder processOAuthUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "processOAuth");
    }


    @Path("grants/identity-token")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response grantIdentityToken(final MultivaluedMap<String, String> form) {
        return new Transaction() {
            protected Response callImpl() {
                String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
                if (username == null) {
                    throw new NotAuthorizedException("No user");
                }
                if (!realm.isEnabled()) {
                    throw new NotAuthorizedException("Disabled realm");
                }
                UserModel user = realm.getUser(username);
                if (user == null) {
                    throw new NotAuthorizedException("No user");
                }
                if (!user.isEnabled()) {
                    throw new NotAuthorizedException("Disabled user.");
                }
                if (!authManager.authenticateForm(realm, user, form)) {
                    throw new NotAuthorizedException("FORM");
                }
                tokenManager = new TokenManager();
                SkeletonKeyToken token = authManager.createIdentityToken(realm, username);
                String encoded = tokenManager.encodeToken(realm, token);
                AccessTokenResponse res = accessTokenResponse(token, encoded);
                return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
            }
        }.call();
    }

    @Path("grants/access")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response grantAccessToken(final MultivaluedMap<String, String> form) {
        return new Transaction() {
            protected Response callImpl() {
                String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
                if (username == null) {
                    throw new NotAuthorizedException("No user");
                }
                if (!realm.isEnabled()) {
                    throw new NotAuthorizedException("Disabled realm");
                }
                UserModel user = realm.getUser(username);
                if (user == null) {
                    throw new NotAuthorizedException("No user");
                }
                if (!user.isEnabled()) {
                    throw new NotAuthorizedException("Disabled user.");
                }
                if (authManager.authenticateForm(realm, user, form)) {
                    throw new NotAuthorizedException("Auth failed");
                }
                SkeletonKeyToken token = tokenManager.createAccessToken(realm, user);
                String encoded = tokenManager.encodeToken(realm, token);
                AccessTokenResponse res = accessTokenResponse(token, encoded);
                return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
            }
        }.call();

    }

    @Path("auth/request/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processLogin(final MultivaluedMap<String, String> formData) {
        return new Transaction() {
            protected Response callImpl() {
                String clientId = formData.getFirst("client_id");
                String scopeParam = formData.getFirst("scope");
                String state = formData.getFirst("state");
                String redirect = formData.getFirst("redirect_uri");

                if (!realm.isEnabled()) {
                    securityFailureForward("Realm not enabled.");
                    return null;
                }
                UserModel client = realm.getUser(clientId);
                if (client == null) {
                    securityFailureForward("Unknown login requester.");
                    return null;
                }
                if (!client.isEnabled()) {
                    securityFailureForward("Login requester not enabled.");
                    return null;
                }
                String username = formData.getFirst("username");
                UserModel user = realm.getUser(username);
                if (user == null) {
                    logger.error("Incorrect user name.");
                    request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Incorrect user name.");
                    forwardToLoginForm(redirect, clientId, scopeParam, state);
                    return null;
                }
                if (!user.isEnabled()) {
                    securityFailureForward("Your account is not enabled.");
                    return null;
                }
                boolean authenticated = authManager.authenticateForm(realm, user, formData);
                if (!authenticated) {
                    logger.error("Authentication failed");
                    request.setAttribute("username", username);
                    request.setAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE", "Invalid credentials.");
                    forwardToLoginForm(redirect, clientId, scopeParam, state);
                    return null;
                }

                return processAccessCode(scopeParam, state, redirect, client, user);
            }
        }.call();
    }

    @Path("access/codes")
    @POST
    @Produces("application/json")
    public Response accessCodeToToken(final MultivaluedMap<String, String> formData) {
        return new Transaction() {
            protected Response callImpl() {
                logger.info("accessRequest <---");
                if (!realm.isEnabled()) {
                    throw new NotAuthorizedException("Realm not enabled");
                }

                String code = formData.getFirst("code");
                if (code == null) {
                    logger.debug("code not specified");
                    Map<String, String> error = new HashMap<String, String>();
                    error.put("error", "invalid_request");
                    error.put("error_description", "code not specified");
                    return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();

                }
                String client_id = formData.getFirst("client_id");
                if (client_id == null) {
                    logger.debug("client_id not specified");
                    Map<String, String> error = new HashMap<String, String>();
                    error.put("error", "invalid_request");
                    error.put("error_description", "client_id not specified");
                    return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
                }
                UserModel client = realm.getUser(client_id);
                if (client == null) {
                    logger.debug("Could not find user");
                    Map<String, String> error = new HashMap<String, String>();
                    error.put("error", "invalid_client");
                    error.put("error_description", "Could not find user");
                    return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
                }

                if (!client.isEnabled()) {
                    logger.debug("user is not enabled");
                    Map<String, String> error = new HashMap<String, String>();
                    error.put("error", "invalid_client");
                    error.put("error_description", "User is not enabled");
                    return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
                }

                boolean authenticated = authManager.authenticateForm(realm, client, formData);
                if (!authenticated) {
                    Map<String, String> error = new HashMap<String, String>();
                    error.put("error", "unauthorized_client");
                    return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
                }


                JWSInput input = new JWSInput(code, providers);
                boolean verifiedCode = false;
                try {
                    verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
                } catch (Exception ignored) {
                    logger.debug("Failed to verify signature", ignored);
                }
                if (!verifiedCode) {
                    Map<String, String> res = new HashMap<String, String>();
                    res.put("error", "invalid_grant");
                    res.put("error_description", "Unable to verify code signature");
                    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
                }
                String key = input.readContent(String.class);
                AccessCodeEntry accessCode = tokenManager.pullAccessCode(key);
                if (accessCode == null) {
                    Map<String, String> res = new HashMap<String, String>();
                    res.put("error", "invalid_grant");
                    res.put("error_description", "Code not found");
                    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
                }
                if (accessCode.isExpired()) {
                    Map<String, String> res = new HashMap<String, String>();
                    res.put("error", "invalid_grant");
                    res.put("error_description", "Code is expired");
                    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
                }
                if (!accessCode.getToken().isActive()) {
                    Map<String, String> res = new HashMap<String, String>();
                    res.put("error", "invalid_grant");
                    res.put("error_description", "Token expired");
                    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
                }
                if (!client.getLoginName().equals(accessCode.getClient().getLoginName())) {
                    Map<String, String> res = new HashMap<String, String>();
                    res.put("error", "invalid_grant");
                    res.put("error_description", "Auth error");
                    return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
                }
                logger.info("accessRequest SUCCESS");
                AccessTokenResponse res = accessTokenResponse(realm.getPrivateKey(), accessCode.getToken());
                return Response.ok(res).build();
            }
        }.call();

    }

    protected AccessTokenResponse accessTokenResponse(PrivateKey privateKey, SkeletonKeyToken token) {
        byte[] tokenBytes = null;
        try {
            tokenBytes = JsonSerialization.toByteArray(token, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String encodedToken = new JWSBuilder()
                .content(tokenBytes)
                .rsa256(privateKey);

        return accessTokenResponse(token, encodedToken);
    }

    protected AccessTokenResponse accessTokenResponse(SkeletonKeyToken token, String encodedToken) {
        AccessTokenResponse res = new AccessTokenResponse();
        res.setToken(encodedToken);
        res.setTokenType("bearer");
        if (token.getExpiration() != 0) {
            long time = token.getExpiration() - (System.currentTimeMillis() / 1000);
            res.setExpiresIn(time);
        }
        return res;
    }

    @Path("login")
    @GET
    public Response loginPage(final @QueryParam("response_type") String responseType,
                              final @QueryParam("redirect_uri") String redirect,
                              final @QueryParam("client_id") String clientId,
                              final @QueryParam("scope") String scopeParam,
                              final @QueryParam("state") String state) {
        return new Transaction() {
            protected Response callImpl() {
                if (!realm.isEnabled()) {
                    securityFailureForward("Realm not enabled");
                    return null;
                }
                UserModel client = realm.getUser(clientId);
                if (client == null) {
                    securityFailureForward("Unknown login requester.");
                    transaction.rollback();
                    return null;
                }

                if (!client.isEnabled()) {
                    securityFailureForward("Login requester not enabled.");
                    transaction.rollback();
                    session.close();
                    return null;
                }

                RoleModel resourceRole = realm.getRole(RealmManager.RESOURCE_ROLE);
                RoleModel identityRequestRole = realm.getRole(RealmManager.IDENTITY_REQUESTER_ROLE);
                boolean isResource = realm.hasRole(client, resourceRole);
                if (!isResource && !realm.hasRole(client, identityRequestRole)) {
                    securityFailureForward("Login requester not allowed to request login.");
                    transaction.rollback();
                    session.close();
                    return null;
                }

                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    logger.info(user.getLoginName() + " already logged in.");
                    return processAccessCode(scopeParam, state, redirect, client, user);
                }

                forwardToLoginForm(redirect, clientId, scopeParam, state);
                return null;
            }
        }.call();
    }

    @Path("logout")
    @GET
    @NoCache
    public Response logout(final @QueryParam("redirect_uri") String redirectUri) {
        return new Transaction() {
            protected Response callImpl() {
                // todo do we care if anybody can trigger this?

                UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
                if (user != null) {
                    logger.info("Logging out: " + user.getLoginName());
                    authManager.expireIdentityCookie(realm, uriInfo);
                    resourceAdminManager.singleLogOut(realm, user.getLoginName());
                }
                // todo manage legal redirects
                return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
            }
        }.call();
    }

    @Path("oauth/grant")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processOAuth(final MultivaluedMap<String, String> formData) {
        return new Transaction() {
            protected Response callImpl() {
                String code = formData.getFirst("code");
                JWSInput input = new JWSInput(code, providers);
                boolean verifiedCode = false;
                try {
                    verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
                } catch (Exception ignored) {
                    logger.debug("Failed to verify signature", ignored);
                }
                if (!verifiedCode) {
                    securityFailureForward("Illegal access code.");
                    session.close();
                    return null;
                }
                String key = input.readContent(String.class);
                AccessCodeEntry accessCodeEntry = tokenManager.getAccessCode(key);
                if (accessCodeEntry == null) {
                    securityFailureForward("Unknown access code.");
                    session.close();
                    return null;
                }

                String redirect = accessCodeEntry.getRedirectUri();
                String state = accessCodeEntry.getState();

                if (formData.containsKey("cancel")) {
                    return redirectAccessDenied(redirect, state);
                }

                return redirectAccessCode(accessCodeEntry, state, redirect);
            }
        }.call();
    }

    protected Response redirectAccessDenied(String redirect, String state) {
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("error", "access_denied");
        if (state != null) redirectUri.queryParam("state", state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        return location.build();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
