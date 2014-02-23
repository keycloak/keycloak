package org.keycloak.services.resources;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenService {

    protected static final Logger logger = Logger.getLogger(TokenService.class);

    protected RealmModel realm;
    protected TokenManager tokenManager;
    protected AuthenticationManager authManager = new AuthenticationManager();

    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest request;
    @Context
    protected HttpResponse response;
    @Context
    protected KeycloakSession session;
    @Context
    protected KeycloakTransaction transaction;

    @Context
    protected ResourceContext resourceContext;

    private ResourceAdminManager resourceAdminManager = new ResourceAdminManager();

    public TokenService(RealmModel realm, TokenManager tokenManager) {
        this.realm = realm;
        this.tokenManager = tokenManager;
    }

    public static UriBuilder tokenServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getTokenService");
        return base;
    }

    public static UriBuilder accessCodeToTokenUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "accessCodeToToken");

    }

    public static UriBuilder grantAccessTokenUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "grantAccessToken");

    }

    public static UriBuilder loginPageUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "loginPage");
    }

    public static UriBuilder logoutUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "logout");
    }

    public static UriBuilder processLoginUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "processLogin");
    }

    public static UriBuilder processOAuthUrl(UriInfo uriInfo) {
        return tokenServiceBaseUrl(uriInfo).path(TokenService.class, "processOAuth");
    }

    @Path("grants/access")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response grantAccessToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                     final MultivaluedMap<String, String> form) {
        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        UserModel client = authorizeClient(authorizationHeader);


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
        if (authManager.authenticateForm(realm, user, form) != AuthenticationStatus.SUCCESS) {
            throw new NotAuthorizedException("Auth failed");
        }
        String scope = form.getFirst("scope");
        AccessTokenResponse res = tokenManager.responseBuilder(realm)
                .generateAccessToken(scope, client, user).build();
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Path("refresh")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshAccessToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                     final MultivaluedMap<String, String> form) {
        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        UserModel client = authorizeClient(authorizationHeader);
        String refreshToken = form.getFirst("refresh_token");
        AccessToken accessToken = null;
        try {
            accessToken = tokenManager.refreshAccessToken(realm, client, refreshToken);
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getError());
            if (e.getDescription() != null) error.put("error_description", e.getDescription());
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build(), e);
        }

        AccessTokenResponse res = tokenManager.responseBuilder(realm)
                                              .accessToken(accessToken)
                                              .generateRefreshToken().build();
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Path("auth/request/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processLogin(@QueryParam("client_id") final String clientId, @QueryParam("scope") final String scopeParam,
            @QueryParam("state") final String state, @QueryParam("redirect_uri") String redirect,
            final MultivaluedMap<String, String> formData) {
        logger.debug("TokenService.processLogin");
        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            return oauth.forwardToSecurityFailure("Realm not enabled.");
        }
        UserModel client = realm.getUser(clientId);
        if (client == null) {
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }
        if (!client.isEnabled()) {
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(redirect, client);
        if (redirect == null) {
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (formData.containsKey("cancel")) {
            return oauth.redirectError(client, "access_denied", state, redirect);
        }

        String username = formData.getFirst("username");
        UserModel user = realm.getUser(username);
        if (user == null && username.contains("@")) {
            user = realm.getUserByEmail(username);
        }

        if (user == null){
            return Flows.forms(realm, request, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
        }

        AuthenticationStatus status = authManager.authenticateForm(realm, user, formData);

        switch (status) {
            case SUCCESS:
            case ACTIONS_REQUIRED:
                return oauth.processAccessCode(scopeParam, state, redirect, client, user);
            case ACCOUNT_DISABLED:
                return Flows.forms(realm, request, uriInfo).setError(Messages.ACCOUNT_DISABLED).setFormData(formData).createLogin();
            case MISSING_TOTP:
                return Flows.forms(realm, request, uriInfo).setFormData(formData).createLoginTotp();
            default:
                return Flows.forms(realm, request, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
        }
    }

    @Path("auth/request/login-actions")
    public RequiredActionsService getRequiredActionsService() {
        RequiredActionsService service = new RequiredActionsService(realm, tokenManager);
        resourceContext.initResource(service);
        return service;
    }

    @Path("registrations")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processRegister(@QueryParam("client_id") final String clientId,
            @QueryParam("scope") final String scopeParam, @QueryParam("state") final String state,
            @QueryParam("redirect_uri") String redirect, final MultivaluedMap<String, String> formData) {
        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!realm.isEnabled()) {
            logger.warn("Realm not enabled");
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        UserModel client = realm.getUser(clientId);
        if (client == null) {
            logger.warn("Unknown login requester.");
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(redirect, client);
        if (redirect == null) {
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (!realm.isRegistrationAllowed()) {
            logger.warn("Registration not allowed");
            return oauth.forwardToSecurityFailure("Registration not allowed");
        }

        List<String> requiredCredentialTypes = new LinkedList<String>();
        for (RequiredCredentialModel m : realm.getRequiredCredentials()) {
            requiredCredentialTypes.add(m.getType());
        }

        String error = Validation.validateRegistrationForm(formData, requiredCredentialTypes);
        if (error == null) {
            error = Validation.validatePassword(formData, realm.getPasswordPolicy());
        }

        if (error != null) {
            return Flows.forms(realm, request, uriInfo).setError(error).setFormData(formData).createRegistration();
        }

        String username = formData.getFirst("username");

        UserModel user = realm.getUser(username);
        if (user != null) {
            return Flows.forms(realm, request, uriInfo).setError(Messages.USERNAME_EXISTS).setFormData(formData).createRegistration();
        }

        user = realm.addUser(username);
        user.setEnabled(true);
        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        user.setEmail(formData.getFirst("email"));

        if (requiredCredentialTypes.contains(CredentialRepresentation.PASSWORD)) {
            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(formData.getFirst("password"));
            realm.updateCredential(user, credentials);
        }

        return processLogin(clientId, scopeParam, state, redirect, formData);
    }

    @Path("access/codes")
    @POST
    @Produces("application/json")
    public Response accessCodeToToken(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader, final MultivaluedMap<String, String> formData) {
        logger.debug("accessRequest <---");

        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Realm not enabled");
        }

        UserModel client = authorizeClient(authorizationHeader);

        String code = formData.getFirst("code");
        if (code == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_request");
            error.put("error_description", "code not specified");
            throw new BadRequestException("Code not specified", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());

        }

        JWSInput input = new JWSInput(code);
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
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        String key = input.readContentAsString();
        AccessCodeEntry accessCode = tokenManager.pullAccessCode(key);
        if (accessCode == null) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Code not found");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        if (accessCode.isExpired()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Code is expired");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        if (!accessCode.getToken().isActive()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Token expired");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        if (!client.getLoginName().equals(accessCode.getClient().getLoginName())) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        logger.debug("accessRequest SUCCESS");
        AccessTokenResponse res = tokenManager.responseBuilder(realm)
                                              .accessToken(accessCode.getToken())
                                              .generateRefreshToken().build();

        return Cors.add(request, Response.ok(res)).allowedOrigins(client).allowedMethods("POST").build();
    }

    protected UserModel authorizeClient(String authorizationHeader) {
        if (authorizationHeader == null) {
            throw new NotAuthorizedException("No Authorization header to authenticate client", "Basic realm=\"" + realm.getName() + "\"");
        }

        String[] usernameSecret = BasicAuthHelper.parseHeader(authorizationHeader);
        if (usernameSecret == null) {
            throw new NotAuthorizedException("No Authorization header to authenticate client", "Basic realm=\"" + realm.getName() + "\"");
        }

        String client_id = usernameSecret[0];
        String clientSecret = usernameSecret[1];
        UserModel client = realm.getUser(client_id);
        if (client == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_client");
            error.put("error_description", "Could not find client");
            throw new BadRequestException("Could not find client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if (!client.isEnabled()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_client");
            error.put("error_description", "Client is not enabled");
            throw new BadRequestException("Client is not enabled", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if (!realm.validateSecret(client, clientSecret)) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "unauthorized_client");
            throw new BadRequestException("Unauthorized Client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }
        return client;
    }

    @Path("login")
    @GET
    public Response loginPage(final @QueryParam("response_type") String responseType,
            @QueryParam("redirect_uri") String redirect, final @QueryParam("client_id") String clientId,
            final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state, final @QueryParam("prompt") String prompt) {
        logger.info("TokenService.loginPage");
        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            logger.warn("Realm not enabled");
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        UserModel client = realm.getUser(clientId);
        if (client == null) {
            logger.warn("Unknown login requester: " + clientId);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }
        redirect = verifyRedirectUri(redirect, client);
        if (redirect == null) {
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        logger.info("Checking roles...");
        RoleModel resourceRole = realm.getRole(Constants.APPLICATION_ROLE);
        RoleModel identityRequestRole = realm.getRole(Constants.IDENTITY_REQUESTER_ROLE);
        boolean isResource = realm.hasRole(client, resourceRole);
        if (!isResource && !realm.hasRole(client, identityRequestRole)) {
            logger.warn("Login requester not allowed to request login.");
            return oauth.forwardToSecurityFailure("Login requester not allowed to request login.");
        }
        logger.info("Checking cookie...");
        UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
        if (user != null) {
            logger.debug(user.getLoginName() + " already logged in.");
            return oauth.processAccessCode(scopeParam, state, redirect, client, user);
        }

        if (prompt != null && prompt.equals("none")) {
            return oauth.redirectError(client, "access_denied", state, redirect);
        }
        logger.info("createLogin() now...");
        return Flows.forms(realm, request, uriInfo).createLogin();
    }

    @Path("registrations")
    @GET
    public Response registerPage(final @QueryParam("response_type") String responseType,
            @QueryParam("redirect_uri") String redirect, final @QueryParam("client_id") String clientId,
            final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state) {
        logger.info("**********registerPage()");
        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            logger.warn("Realm not enabled");
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        UserModel client = realm.getUser(clientId);
        if (client == null) {
            logger.warn("Unknown login requester.");
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(redirect, client);
        if (redirect == null) {
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (!realm.isRegistrationAllowed()) {
            logger.warn("Registration not allowed");
            return oauth.forwardToSecurityFailure("Registration not allowed");
        }

        authManager.expireIdentityCookie(realm, uriInfo);

        return Flows.forms(realm, request, uriInfo).createRegistration();
    }

    @Path("logout")
    @GET
    @NoCache
    public Response logout(final @QueryParam("redirect_uri") String redirectUri) {
        // todo do we care if anybody can trigger this?

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers, false);
        if (user != null) {
            logger.info("Logging out: {0}", user.getLoginName());
            authManager.expireIdentityCookie(realm, uriInfo);
            resourceAdminManager.singleLogOut(realm, user.getId());
        } else {
            logger.info("No user logged in for logout");
        }
        // todo manage legal redirects
        return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
    }

    @Path("oauth/grant")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processOAuth(final MultivaluedMap<String, String> formData) {
        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        String code = formData.getFirst("code");
        JWSInput input = new JWSInput(code);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            logger.debug("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            return oauth.forwardToSecurityFailure("Illegal access code.");
        }
        String key = input.readContentAsString();
        AccessCodeEntry accessCodeEntry = tokenManager.getAccessCode(key);
        if (accessCodeEntry == null) {
            return oauth.forwardToSecurityFailure("Unknown access code.");
        }

        String redirect = accessCodeEntry.getRedirectUri();
        String state = accessCodeEntry.getState();

        if (formData.containsKey("cancel")) {
            return redirectAccessDenied(redirect, state);
        }

        accessCodeEntry.setExpiration((System.currentTimeMillis() / 1000) + realm.getAccessCodeLifespan());
        return oauth.redirectAccessCode(accessCodeEntry, state, redirect);
    }

    protected Response redirectAccessDenied(String redirect, String state) {
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("error", "access_denied");
        if (state != null)
            redirectUri.queryParam("state", state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        return location.build();
    }

    public static String verifyRedirectUri(String redirectUri, UserModel client) {
        if (redirectUri == null) {
            return client.getRedirectUris().size() == 1 ? client.getRedirectUris().iterator().next() : null;
        } else if (client.getRedirectUris().isEmpty()) {
            return redirectUri;
        } else {
            String r = redirectUri.indexOf('?') != -1 ? redirectUri.substring(0, redirectUri.indexOf('?')) : redirectUri;
            return client.getRedirectUris().contains(r) ? redirectUri : null;
        }
    }

    private boolean checkSsl() {
        if (realm.isSslNotRequired()) {
            return true;
        }

        return uriInfo.getBaseUri().getScheme().equals("https");
    }

}
