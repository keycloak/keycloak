package org.keycloak.services.resources;

import org.jboss.resteasy.jose.jws.JWSBuilder;
import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.TokenIdGenerator;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.SkeletonKeyScope;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.services.JspRequestParameters;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.ResourceModel;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenService {


    protected static final Logger logger = Logger.getLogger(TokenService.class);

    @Context
    protected UriInfo uriInfo;
    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected HttpHeaders headers;
    @Context
    protected IdentitySession identitySession;
    @Context
    HttpRequest request;
    @Context
    HttpResponse response;


    protected String securityFailurePath = "/securityFailure.jsp";
    protected String loginFormPath = "/loginForm.jsp";
    protected String oauthFormPath = "/oauthForm.jsp";

    protected RealmModel realm;
    protected TokenManager tokenManager;
    protected AuthenticationManager authManager = new AuthenticationManager();
    private ResourceAdminManager resourceAdminManager = new ResourceAdminManager();

    public TokenService(RealmModel realm, TokenManager tokenManager) {
        this.realm = realm;
        this.tokenManager = tokenManager;
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
    public Response grantIdentityToken(MultivaluedMap<String, String> form) {
        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            throw new NotAuthorizedException("No user");
        }
        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Disabled realm");
        }
        User user = realm.getIdm().getUser(username);
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
        SkeletonKeyToken token = tokenManager.createIdentityToken(realm, username);
        String encoded = tokenManager.encodeToken(realm, token);
        AccessTokenResponse res = accessTokenResponse(token, encoded);
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Path("grants/access")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response grantAccessToken(MultivaluedMap<String, String> form) {
        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            throw new NotAuthorizedException("No user");
        }
        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Disabled realm");
        }
        User user = realm.getIdm().getUser(username);
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

    @Path("auth/request/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processLogin(MultivaluedMap<String, String> formData) {
        String clientId = formData.getFirst("client_id");
        String scopeParam = formData.getFirst("scope");
        String state = formData.getFirst("state");
        String redirect = formData.getFirst("redirect_uri");

        if (!realm.isEnabled()) {
            securityFailureForward("Realm not enabled.");
            return null;
        }
        User client = realm.getIdm().getUser(clientId);
        if (client == null) {
            securityFailureForward("Unknown login requester.");
            return null;
        }
        if (!client.isEnabled()) {
            securityFailureForward("Login requester not enabled.");
            return null;
        }
        String username = formData.getFirst("username");
        User user = realm.getIdm().getUser(username);
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

    protected Response processAccessCode(String scopeParam, String state, String redirect, User client, User user) {
        Role resourceRole = realm.getIdm().getRole(RealmManager.RESOURCE_ROLE);
        Role oauthClientRole = realm.getIdm().getRole(RealmManager.OAUTH_CLIENT_ROLE);
        boolean isResource = realm.getIdm().hasRole(client, resourceRole);
        if (!isResource && !realm.getIdm().hasRole(client, oauthClientRole)) {
            securityFailureForward("Login requester not allowed to request login.");
            identitySession.close();
            return null;
        }
        AccessCodeEntry accessCode = tokenManager.createAccessCode(scopeParam, realm, client, user);

        if (!isResource && accessCode.getRealmRolesRequested().size() > 0 && accessCode.getResourceRolesRequested().size() > 0) {
            oauthGrantPage(accessCode, client, state, redirect);
            identitySession.close();
            return null;
        }
        return redirectAccessCode(accessCode, state, redirect);
    }

    protected Response redirectAccessCode(AccessCodeEntry accessCode, String state, String redirect) {
        String code = accessCode.getCode();
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("code", code);
        if (state != null) redirectUri.queryParam("state", state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        if (realm.isCookieLoginAllowed()) {
            location.cookie(tokenManager.createLoginCookie(realm, accessCode.getUser(), uriInfo));
        }
        return location.build();
    }

    @Path("access/codes")
    @POST
    @Produces("application/json")
    public Response accessCodeToToken(MultivaluedMap<String, String> formData) {
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
        User client = realm.getIdm().getUser(client_id);
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
        if (!client.getId().equals(accessCode.getClient().getId())) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
        }
        logger.info("accessRequest SUCCESS");
        AccessTokenResponse res = accessTokenResponse(realm.getPrivateKey(), accessCode.getToken());
        return Response.ok(res).build();

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

    protected void securityFailureForward(String message) {
        logger.error(message);
        request.setAttribute(JspRequestParameters.KEYCLOAK_SECURITY_FAILURE_MESSAGE, message);
        request.forward(securityFailurePath);
        identitySession.close();
    }

    protected void forwardToLoginForm(String redirect,
                                      String clientId,
                                      String scopeParam,
                                      String state) {
        request.setAttribute(RealmModel.class.getName(), realm);
        request.setAttribute("KEYCLOAK_LOGIN_ACTION", processLoginUrl(uriInfo).build(realm.getId()));

        // RESTEASY eats the form data, so we send via an attribute
        request.setAttribute("redirect_uri", redirect);
        request.setAttribute("client_id", clientId);
        request.setAttribute("scope", scopeParam);
        request.setAttribute("state", state);
        request.forward(loginFormPath);
        identitySession.close();
    }

    @Path("login")
    @GET
    public Response loginPage(@QueryParam("response_type") String responseType,
                              @QueryParam("redirect_uri") String redirect,
                              @QueryParam("client_id") String clientId,
                              @QueryParam("scope") String scopeParam,
                              @QueryParam("state") String state) {
        if (!realm.isEnabled()) {
            securityFailureForward("Realm not enabled");
            return null;
        }
        User client = realm.getIdm().getUser(clientId);
        if (client == null) {
            securityFailureForward("Unknown login requester.");
            return null;
        }

        if (!client.isEnabled()) {
            securityFailureForward("Login requester not enabled.");
            identitySession.close();
            return null;
        }
        Role resourceRole = realm.getIdm().getRole(RealmManager.RESOURCE_ROLE);
        Role oauthClientRole = realm.getIdm().getRole(RealmManager.OAUTH_CLIENT_ROLE);
        boolean isResource = realm.getIdm().hasRole(client, resourceRole);
        if (!isResource && !realm.getIdm().hasRole(client, oauthClientRole)) {
            securityFailureForward("Login requester not allowed to request login.");
            identitySession.close();
            return null;
        }

        User user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
        if (user != null) {
            return processAccessCode(scopeParam, state, redirect, client, user);
        }

        forwardToLoginForm(redirect, clientId, scopeParam, state);
        return null;
    }

    @Path("logout")
    @GET
    public Response logout(@QueryParam("redirect_uri") String redirectUri) {
        // todo do we care if anybody can trigger this?

        User user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
        if (user != null) {
            logger.info("Logging out: " + user.getLoginName());
            authManager.expireIdentityCookie(realm, uriInfo);
            resourceAdminManager.singleLogOut(realm, user.getLoginName());
        }
        // todo manage legal redirects
        return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
    }

    @Path("oauth/grant")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processOAuth(MultivaluedMap<String, String> formData) {
        String redirect = formData.getFirst("redirect_uri");
        String state = formData.getFirst("state");
        if (formData.containsKey("cancel")) {
            return redirectAccessDenied(redirect, state);
        }
        String code = formData.getFirst("code");

        JWSInput input = new JWSInput(code, providers);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            logger.debug("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            return redirectAccessDenied(redirect, state);
        }
        String key = input.readContent(String.class);
        AccessCodeEntry accessCodeEntry = tokenManager.getAccessCode(key);
        if (accessCodeEntry == null) {
            return redirectAccessDenied(redirect, state);
        }
        return redirectAccessCode(accessCodeEntry, state, redirect);
    }

    protected Response redirectAccessDenied(String redirect, String state) {
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("error", "access_denied");
        if (state != null) redirectUri.queryParam("state", state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        return location.build();
    }

    protected void oauthGrantPage(AccessCodeEntry accessCode, User client, String state, String redirect_uri) {
        request.setAttribute("realmRolesRequested", accessCode.getRealmRolesRequested());
        request.setAttribute("resourceRolesRequested", accessCode.getResourceRolesRequested());
        request.setAttribute("state", state);
        request.setAttribute("redirect_uri", redirect_uri);
        request.setAttribute("client", client);
        request.setAttribute("action", processOAuthUrl(uriInfo));
        request.setAttribute("accessCode", accessCode.getCode());

        request.forward(oauthFormPath);
    }

}
