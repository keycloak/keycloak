package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotAcceptableException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.audit.Audit;
import org.keycloak.audit.Details;
import org.keycloak.audit.Errors;
import org.keycloak.audit.EventType;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.authentication.AuthenticationProviderManager;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ClientConnection;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource class for the oauth/openid connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenService {

    protected static final Logger logger = Logger.getLogger(TokenService.class);

    protected RealmModel realm;
    protected TokenManager tokenManager;
    private Audit audit;
    protected AuthenticationManager authManager;

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
    protected ClientConnection clientConnection;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    private ResourceAdminManager resourceAdminManager = new ResourceAdminManager();

    public TokenService(RealmModel realm, TokenManager tokenManager, Audit audit, AuthenticationManager authManager) {
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.audit = audit;
        this.authManager = authManager;
    }

    public static UriBuilder tokenServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return tokenServiceBaseUrl(baseUriBuilder);
    }

    public static UriBuilder tokenServiceBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getTokenService");
    }

    public static UriBuilder accessCodeToTokenUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return accessCodeToTokenUrl(baseUriBuilder);

    }

    public static UriBuilder accessCodeToTokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "accessCodeToToken");
    }

    public static UriBuilder grantAccessTokenUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return grantAccessTokenUrl(baseUriBuilder);

    }

    public static UriBuilder grantAccessTokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "grantAccessToken");
    }

    public static UriBuilder loginPageUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return loginPageUrl(baseUriBuilder);
    }

    public static UriBuilder loginPageUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "loginPage");
    }

    public static UriBuilder logoutUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return logoutUrl(baseUriBuilder);
    }

    public static UriBuilder logoutUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "logout");
    }

    public static UriBuilder processLoginUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return processLoginUrl(baseUriBuilder);
    }

    public static UriBuilder processLoginUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "processLogin");
    }

    public static UriBuilder processOAuthUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return processOAuthUrl(baseUriBuilder);
    }

    public static UriBuilder processOAuthUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "processOAuth");
    }

    public static UriBuilder refreshUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "refreshAccessToken");
    }


    /**
     * Direct grant REST invocation.  One stop call to obtain an access token.
     *
     * If the client is a confidential client
     * you must include the client-id (application name or oauth client name) and secret in an Basic Auth Authorization header.
     *
     * If the client is a public client, then you must include a "client_id" form parameter with the app's or oauth client's name.
     *
     * The realm must be configured to allow these types of auth requests.  (Direct Grant API in admin console Settings page)
     *
     *
     * @See  <a href="http://tools.ietf.org/html/rfc6749#section-4.3">http://tools.ietf.org/html/rfc6749#section-4.3</a>
     *
     * @param authorizationHeader
     * @param form
     * @return @see org.keycloak.representations.AccessTokenResponse
     */
    @Path("grants/access")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response grantAccessToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                     final MultivaluedMap<String, String> form) {
        if (!checkSsl()) {
            return createError("https_required", "HTTPS required", Response.Status.FORBIDDEN);
        }

        if (!realm.isPasswordCredentialGrantAllowed()) {
            return createError("not_enabled", "Resource Owner Password Credentials Grant not enabled", Response.Status.FORBIDDEN);
        }

        audit.event(EventType.LOGIN).detail(Details.AUTH_METHOD, "oauth_credentials").detail(Details.RESPONSE_TYPE, "token");

        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            audit.error(Errors.USERNAME_MISSING);
            throw new UnauthorizedException("No username");
        }
        audit.detail(Details.USERNAME, username);

        UserModel user = realm.getUser(username);
        if (user != null) audit.user(user);

        ClientModel client = authorizeClient(authorizationHeader, form, audit);

        if ( (client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            audit.error(Errors.NOT_ALLOWED);
            return createError("not_allowed", "Bearer-only applications are not allowed to invoke grants/access", Response.Status.FORBIDDEN);
        }

        if (!realm.isEnabled()) {
            audit.error(Errors.REALM_DISABLED);
            return createError("realm_disabled", "Realm is disabled", Response.Status.UNAUTHORIZED);
        }

        AuthenticationStatus authenticationStatus = authManager.authenticateForm(clientConnection, realm, form);
        Map<String, String> err;

        switch (authenticationStatus) {
            case SUCCESS:
                break;
            case ACCOUNT_TEMPORARILY_DISABLED:
            case ACTIONS_REQUIRED:
                err = new HashMap<String, String>();
                err.put(OAuth2Constants.ERROR, "invalid_grant");
                err.put(OAuth2Constants.ERROR_DESCRIPTION, "AccountProvider temporarily disabled");
                audit.error(Errors.USER_TEMPORARILY_DISABLED);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                        .build();
            case ACCOUNT_DISABLED:
                err = new HashMap<String, String>();
                err.put(OAuth2Constants.ERROR, "invalid_grant");
                err.put(OAuth2Constants.ERROR_DESCRIPTION, "AccountProvider disabled");
                audit.error(Errors.USER_DISABLED);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                        .build();
            default:
                err = new HashMap<String, String>();
                err.put(OAuth2Constants.ERROR, "invalid_grant");
                err.put(OAuth2Constants.ERROR_DESCRIPTION, "Invalid user credentials");
                audit.error(Errors.INVALID_USER_CREDENTIALS);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                        .build();
        }

        String scope = form.getFirst(OAuth2Constants.SCOPE);

        UserSessionModel session = realm.createUserSession(user, clientConnection.getRemoteAddr());
        session.associateClient(client);
        audit.session(session);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, audit)
                .generateAccessToken(scope, client, user, session)
                .generateRefreshToken()
                .generateIDToken()
                .build();

        audit.success();

        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * URL for making refresh token requests.
     *
     * @See <a href="http://tools.ietf.org/html/rfc6749#section-6">http://tools.ietf.org/html/rfc6749#section-6</a>
     *
     * If the client is a confidential client
     * you must include the client-id (application name or oauth client name) and secret in an Basic Auth Authorization header.
     *
     * If the client is a public client, then you must include a "client_id" form parameter with the app's or oauth client's name.
     *
     * @param authorizationHeader
     * @param form
     * @return
     */
    @Path("refresh")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshAccessToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                       final MultivaluedMap<String, String> form) {
        logger.info("--> refreshAccessToken");
        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        audit.event(EventType.REFRESH_TOKEN);

        ClientModel client = authorizeClient(authorizationHeader, form, audit);
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        AccessToken accessToken;
        try {
            accessToken = tokenManager.refreshAccessToken(uriInfo, realm, client, refreshToken, audit);
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, e.getError());
            if (e.getDescription() != null) error.put(OAuth2Constants.ERROR_DESCRIPTION, e.getDescription());
            audit.error(Errors.INVALID_TOKEN);
            logger.error("OAuth Error", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, audit)
                .accessToken(accessToken)
                .generateIDToken()
                .generateRefreshToken().build();

        audit.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    /**
     * URL called after login page.  YOU SHOULD NEVER INVOKE THIS DIRECTLY!
     *
     * @param clientId
     * @param scopeParam
     * @param state
     * @param redirect
     * @param formData
     * @return
     */
    @Path("auth/request/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processLogin(@QueryParam("client_id") final String clientId, @QueryParam("scope") final String scopeParam,
                                 @QueryParam("state") final String state, @QueryParam("redirect_uri") String redirect,
                                 final MultivaluedMap<String, String> formData) {
        logger.debug("TokenService.processLogin");

        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);

        String rememberMe = formData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
        logger.debug("*** Remember me: " + remember);

        audit.event(EventType.LOGIN).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.USERNAME, username);

        if (remember) {
            audit.detail(Details.REMEMBER_ME, "true");
        }

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            audit.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled.");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            audit.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }
        if (!client.isEnabled()) {
            audit.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            audit.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (formData.containsKey("cancel")) {
            audit.error(Errors.REJECTED_BY_USER);
            return oauth.redirectError(client, "access_denied", state, redirect);
        }

        AuthenticationStatus status = authManager.authenticateForm(clientConnection, realm, formData);

        if (remember) {
            authManager.createRememberMeCookie(realm, uriInfo);
        } else {
            authManager.expireRememberMeCookie(realm, uriInfo);
        }

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(realm, username);
        if (user != null) {
            audit.user(user);
        }

        switch (status) {
            case SUCCESS:
            case ACTIONS_REQUIRED:
                UserSessionModel session = realm.createUserSession(user, clientConnection.getRemoteAddr());
		        audit.session(session);

                return oauth.processAccessCode(scopeParam, state, redirect, client, user, session, username, remember, "form", audit);
            case ACCOUNT_TEMPORARILY_DISABLED:
                audit.error(Errors.USER_TEMPORARILY_DISABLED);
                return Flows.forms(this.session, realm, uriInfo).setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).setFormData(formData).createLogin();
            case ACCOUNT_DISABLED:
                audit.error(Errors.USER_DISABLED);
                return Flows.forms(this.session, realm, uriInfo).setError(Messages.ACCOUNT_DISABLED).setFormData(formData).createLogin();
            case MISSING_TOTP:
                return Flows.forms(this.session, realm, uriInfo).setFormData(formData).createLoginTotp();
            case INVALID_USER:
                audit.error(Errors.USER_NOT_FOUND);
                return Flows.forms(this.session, realm, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
            default:
                audit.error(Errors.INVALID_USER_CREDENTIALS);
                return Flows.forms(this.session, realm, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
        }
    }

    @Path("auth/request/login-actions")
    public RequiredActionsService getRequiredActionsService() {
        RequiredActionsService service = new RequiredActionsService(realm, tokenManager, audit);
        ResteasyProviderFactory.getInstance().injectProperties(service);

        //resourceContext.initResource(service);
        return service;
    }

    /**
     * Registration
     *
     * @param clientId
     * @param scopeParam
     * @param state
     * @param redirect
     * @param formData
     * @return
     */
    @Path("registrations")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processRegister(@QueryParam("client_id") final String clientId,
                                    @QueryParam("scope") final String scopeParam, @QueryParam("state") final String state,
                                    @QueryParam("redirect_uri") String redirect, final MultivaluedMap<String, String> formData) {

        String username = formData.getFirst("username");
        String email = formData.getFirst("email");

        audit.event(EventType.REGISTER).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, authManager, tokenManager);

        if (!realm.isEnabled()) {
            logger.warn("Realm not enabled");
            audit.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            logger.warn("Unknown login requester.");
            audit.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            audit.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            audit.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (!realm.isRegistrationAllowed()) {
            logger.warn("Registration not allowed");
            audit.error(Errors.REGISTRATION_DISABLED);
            return oauth.forwardToSecurityFailure("Registration not allowed");
        }

        List<String> requiredCredentialTypes = new LinkedList<String>();
        for (RequiredCredentialModel m : realm.getRequiredCredentials()) {
            requiredCredentialTypes.add(m.getType());
        }

        // Validate here, so user is not created if password doesn't validate to passwordPolicy of current realm
        String error = Validation.validateRegistrationForm(formData, requiredCredentialTypes);
        if (error == null) {
            error = Validation.validatePassword(formData, realm.getPasswordPolicy());
        }

        if (error != null) {
            audit.error(Errors.INVALID_REGISTRATION);
            return Flows.forms(session, realm, uriInfo).setError(error).setFormData(formData).createRegistration();
        }

        AuthenticationProviderManager authenticationProviderManager = AuthenticationProviderManager.getManager(realm, session);

        // Validate that user with this username doesn't exist in realm or any authentication provider
        if (realm.getUser(username) != null || authenticationProviderManager.getUser(username) != null) {
            audit.error(Errors.USERNAME_IN_USE);
            return Flows.forms(session, realm, uriInfo).setError(Messages.USERNAME_EXISTS).setFormData(formData).createRegistration();
        }

        UserModel user = realm.addUser(username);
        user.setEnabled(true);
        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        user.setEmail(email);

        if (requiredCredentialTypes.contains(CredentialRepresentation.PASSWORD)) {
            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(formData.getFirst("password"));

            boolean passwordUpdateSuccessful;
            String passwordUpdateError;
            try {
                passwordUpdateSuccessful = AuthenticationProviderManager.getManager(realm, session).updatePassword(user, formData.getFirst("password"));
                passwordUpdateError = "Password update failed";
            } catch (AuthenticationProviderException ape) {
                passwordUpdateSuccessful = false;
                passwordUpdateError = ape.getMessage();
            }

            // User already registered, but force him to update password
            if (!passwordUpdateSuccessful) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                return Flows.forms(session, realm, uriInfo).setError(passwordUpdateError).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            }
        }

        audit.user(user).success();
        audit.reset();

        return processLogin(clientId, scopeParam, state, redirect, formData);
    }

    /**
     * CORS preflight path for access code to token
     *
     * @return
     */
    @Path("access/codes")
    @OPTIONS
    @Produces("application/json")
    public Response accessCodeToTokenPreflight() {
        logger.debugv("cors request from: {0}" , request.getHttpHeaders().getRequestHeaders().getFirst("Origin"));
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    /**
     * URL invoked by adapter to turn an access code to access token
     *
     * @See <a href="http://tools.ietf.org/html/rfc6749#section-4.1">http://tools.ietf.org/html/rfc6749#section-4.1</a>
     *
     * @param authorizationHeader
     * @param formData
     * @return
     */
    @Path("access/codes")
    @POST
    @Produces("application/json")
    public Response accessCodeToToken(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader, final MultivaluedMap<String, String> formData) {
        logger.debug("accessRequest <---");

        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        audit.event(EventType.CODE_TO_TOKEN);

        if (!realm.isEnabled()) {
            audit.error(Errors.REALM_DISABLED);
            throw new UnauthorizedException("Realm not enabled");
        }

        String code = formData.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_request");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "code not specified");
            audit.error(Errors.INVALID_CODE);
            throw new BadRequestException("Code not specified", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }



        AccessCodeEntry accessCode = tokenManager.parseCode(code, realm);
        if (accessCode == null) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Code not found");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        audit.detail(Details.CODE_ID, accessCode.getCodeId());
        if (accessCode.isExpired()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Code is expired");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        if (!accessCode.getToken().isActive()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Token expired");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        audit.user(accessCode.getUser());
        audit.session(accessCode.getSessionState());

        ClientModel client = authorizeClient(authorizationHeader, formData, audit);

        if (!client.getClientId().equals(accessCode.getClient().getClientId())) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Auth error");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        UserModel user = realm.getUserById(accessCode.getUser().getId());
        if (user == null) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "User not found");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        if (!user.isEnabled()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "User disabled");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        UserSessionModel session = realm.getUserSession(accessCode.getSessionState());
        if (!AuthenticationManager.isSessionValid(realm, session)) {
            AuthenticationManager.logout(realm, session, uriInfo);
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Session not active");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        logger.debug("accessRequest SUCCESS");

        session.associateClient(client);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, audit)
                .accessToken(accessCode.getToken())
                .generateIDToken()
                .generateRefreshToken().build();

        audit.success();

        return Cors.add(request, Response.ok(res)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    protected ClientModel authorizeClient(String authorizationHeader, MultivaluedMap<String, String> formData, Audit audit) {
        String client_id;
        String clientSecret;
        if (authorizationHeader != null) {
            String[] usernameSecret = BasicAuthHelper.parseHeader(authorizationHeader);
            if (usernameSecret == null) {
                throw new UnauthorizedException("Bad Authorization header", Response.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + realm.getName() + "\"").build());
            }
            client_id = usernameSecret[0];
            clientSecret = usernameSecret[1];
        } else {
            logger.info("no authorization header");
            client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
            clientSecret = formData.getFirst("client_secret");
        }

        if (client_id == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Could not find client");
            throw new BadRequestException("Could not find client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        audit.client(client_id);

        ClientModel client = realm.findClient(client_id);
        if (client == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Could not find client");
            audit.error(Errors.CLIENT_NOT_FOUND);
            throw new BadRequestException("Could not find client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if (!client.isEnabled()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Client is not enabled");
            audit.error(Errors.CLIENT_DISABLED);
            throw new BadRequestException("Client is not enabled", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if (!client.isPublicClient()) {
            if (!client.validateSecret(clientSecret)) {
                Map<String, String> error = new HashMap<String, String>();
                error.put(OAuth2Constants.ERROR, "unauthorized_client");
                audit.error(Errors.INVALID_CLIENT_CREDENTIALS);
                throw new BadRequestException("Unauthorized Client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
            }
        }
        return client;
    }

    /**
     * Login page.  Must be redirected to from the application or oauth client.
     *
     * @See <a href="http://tools.ietf.org/html/rfc6749#section-4.1">http://tools.ietf.org/html/rfc6749#section-4.1</a>
     *
     *
     * @param responseType
     * @param redirect
     * @param clientId
     * @param scopeParam
     * @param state
     * @param prompt
     * @return
     */
    @Path("login")
    @GET
    public Response loginPage(final @QueryParam("response_type") String responseType,
                              @QueryParam("redirect_uri") String redirect, final @QueryParam("client_id") String clientId,
                              final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state, final @QueryParam("prompt") String prompt) {
        logger.info("TokenService.loginPage");

        audit.event(EventType.LOGIN).client(clientId).detail(Details.REDIRECT_URI, redirect).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            logger.warn("Realm not enabled");
            audit.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            logger.warn("Unknown login requester: " + clientId);
            audit.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            audit.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }
        if ( (client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            audit.error(Errors.NOT_ALLOWED);
            return oauth.forwardToSecurityFailure("Bearer-only applications are not allowed to initiate browser login");
        }
        if (client.isDirectGrantsOnly()) {
            audit.error(Errors.NOT_ALLOWED);
            return oauth.forwardToSecurityFailure("direct-grants-only clients are not allowed to initiate browser login");
        }
        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            audit.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        logger.info("Checking cookie...");
        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
        if (authResult != null) {
            UserModel user = authResult.getUser();
            UserSessionModel session = authResult.getSession();

            logger.debug(user.getLoginName() + " already logged in.");
            audit.user(user).session(session).detail(Details.AUTH_METHOD, "sso");
            return oauth.processAccessCode(scopeParam, state, redirect, client, user, session, null, false, "sso", audit);
        }

        if (prompt != null && prompt.equals("none")) {
            return oauth.redirectError(client, "access_denied", state, redirect);
        }
        logger.info("createLogin() now...");
        return Flows.forms(session, realm, uriInfo).createLogin();
    }

    /**
     * Registration page.  Must be redirected to from login page.
     *
     * @param responseType
     * @param redirect
     * @param clientId
     * @param scopeParam
     * @param state
     * @return
     */
    @Path("registrations")
    @GET
    public Response registerPage(final @QueryParam("response_type") String responseType,
                                 @QueryParam("redirect_uri") String redirect, final @QueryParam("client_id") String clientId,
                                 final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state) {
        logger.info("**********registerPage()");

        audit.event(EventType.REGISTER).client(clientId).detail(Details.REDIRECT_URI, redirect).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            logger.warn("Realm not enabled");
            audit.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            logger.warn("Unknown login requester.");
            audit.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            audit.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            audit.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (!realm.isRegistrationAllowed()) {
            logger.warn("Registration not allowed");
            audit.error(Errors.REGISTRATION_DISABLED);
            return oauth.forwardToSecurityFailure("Registration not allowed");
        }

        authManager.expireIdentityCookie(realm, uriInfo);

        return Flows.forms(session, realm, uriInfo).createRegistration();
    }

    /**
     * Logout user session.
     *
     * @param sessionState
     * @param redirectUri
     * @return
     */
    @Path("logout")
    @GET
    @NoCache
    public Response logout(final @QueryParam("session_state") String sessionState, final @QueryParam("redirect_uri") String redirectUri) {
        // todo do we care if anybody can trigger this?

        audit.event(EventType.LOGOUT);
        if (redirectUri != null) {
            audit.detail(Details.REDIRECT_URI, redirectUri);
        }
        if (sessionState != null) {
            audit.session(sessionState);
        }

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(realm, uriInfo, headers, false);
        if (authResult != null) {
            logout(authResult.getSession());
        } else if (sessionState != null) {
            UserSessionModel userSession = realm.getUserSession(sessionState);
            if (userSession != null) {
                logout(userSession);
            } else {
                audit.error(Errors.USER_SESSION_NOT_FOUND);
            }
        } else {
            audit.error(Errors.USER_NOT_LOGGED_IN);
        }

        if (redirectUri != null) {
            // todo manage legal redirects
            return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
        } else {
            return Response.ok().build();
        }
    }

    private void logout(UserSessionModel session) {
        UserModel user = session.getUser();
        authManager.logout(realm, session, uriInfo);
        audit.user(user).session(session).success();
    }

    /**
     * OAuth grant page.  You should not invoked this directly!
     *
     * @param formData
     * @return
     */
    @Path("oauth/grant")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processOAuth(final MultivaluedMap<String, String> formData) {
        audit.event(EventType.LOGIN).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        String code = formData.getFirst(OAuth2Constants.CODE);

        AccessCodeEntry accessCodeEntry = tokenManager.parseCode(code, realm);
        if (accessCodeEntry == null) {
            audit.error(Errors.INVALID_CODE);
            return oauth.forwardToSecurityFailure("Unknown access code.");
        }
        audit.detail(Details.CODE_ID, accessCodeEntry.getCodeId());

        String redirect = accessCodeEntry.getRedirectUri();
        String state = accessCodeEntry.getState();

        audit.client(accessCodeEntry.getClient())
                .user(accessCodeEntry.getUser())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, accessCodeEntry.getAuthMethod())
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.USERNAME, accessCodeEntry.getUsernameUsed());

        if (accessCodeEntry.isRememberMe()) {
            audit.detail(Details.REMEMBER_ME, "true");
        }

        UserSessionModel session = realm.getUserSession(accessCodeEntry.getSessionState());
        if (!AuthenticationManager.isSessionValid(realm, session)) {
            AuthenticationManager.logout(realm, session, uriInfo);
            audit.error(Errors.INVALID_CODE);
            return oauth.forwardToSecurityFailure("Session not active");
        }
        audit.session(session);

        if (formData.containsKey("cancel")) {
            audit.error(Errors.REJECTED_BY_USER);
            return redirectAccessDenied(redirect, state);
        }

        audit.success();

        accessCodeEntry.resetExpiration();
        return oauth.redirectAccessCode(accessCodeEntry, session, state, redirect);
    }

    @Path("oauth/oob")
    @GET
    public Response installedAppUrnCallback(final @QueryParam("code") String code, final @QueryParam("error") String error, final @QueryParam("error_description") String errorDescription) {
        LoginFormsProvider forms = Flows.forms(session, realm, uriInfo);
        if (code != null) {
            return forms.setAccessCode(code).createCode();
        } else {
            return forms.setError(error).createCode();
        }
    }

    protected Response redirectAccessDenied(String redirect, String state) {
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam(OAuth2Constants.ERROR, "access_denied");
        if (state != null)
            redirectUri.queryParam(OAuth2Constants.STATE, state);
        Response.ResponseBuilder location = Response.status(302).location(redirectUri.build());
        return location.build();
    }

    public static boolean matchesRedirects(Set<String> validRedirects, String redirect) {
        for (String validRedirect : validRedirects) {
            if (validRedirect.endsWith("*")) {
                // strip off *
                int length = validRedirect.length() - 1;
                validRedirect = validRedirect.substring(0, length);
                if (redirect.startsWith(validRedirect)) return true;
                // strip off trailing '/'
                if (length - 1 > 0 && validRedirect.charAt(length - 1) == '/') length--;
                validRedirect = validRedirect.substring(0, length);
                if (validRedirect.equals(redirect)) return true;
            } else if (validRedirect.equals(redirect)) return true;
        }
        return false;
    }

    public static String verifyRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm, ClientModel client) {
        Set<String> validRedirects = client.getRedirectUris();
        if (redirectUri == null) {
            if (validRedirects.size() != 1) return null;
            String validRedirect = validRedirects.iterator().next();
            int idx = validRedirect.indexOf("/*");
            if (idx > -1) {
                validRedirect = validRedirect.substring(0, idx);
            }
            redirectUri = validRedirect;
        } else if (validRedirects.isEmpty()) {
            logger.error("Redirect URI is required for client: " + client.getClientId());
            redirectUri = null;
        } else {
            String r = redirectUri.indexOf('?') != -1 ? redirectUri.substring(0, redirectUri.indexOf('?')) : redirectUri;
            Set<String> resolveValidRedirects = resolveValidRedirects(uriInfo, validRedirects);


            boolean valid = matchesRedirects(resolveValidRedirects, r);

            if (!valid && r.startsWith(Constants.INSTALLED_APP_URL) && r.indexOf(':', Constants.INSTALLED_APP_URL.length()) >= 0) {
                int i = r.indexOf(':', Constants.INSTALLED_APP_URL.length());

                StringBuilder sb = new StringBuilder();
                sb.append(r.substring(0, i));

                i = r.indexOf('/', i);
                if (i >= 0) {
                    sb.append(r.substring(i));
                }

                r = sb.toString();

                valid = matchesRedirects(resolveValidRedirects, r);
            }
            redirectUri = valid ? redirectUri : null;
        }

        if (Constants.INSTALLED_APP_URN.equals(redirectUri)) {
            return Urls.realmInstalledAppUrnCallback(uriInfo.getBaseUri(), realm.getName()).toString();
        } else {
            return redirectUri;
        }
    }

    public static Set<String> resolveValidRedirects(UriInfo uriInfo, Set<String> validRedirects) {
        // If the valid redirect URI is relative (no scheme, host, port) then use the request's scheme, host, and port
        Set<String> resolveValidRedirects = new HashSet<String>();
        for (String validRedirect : validRedirects) {
            if (validRedirect.startsWith("/")) {
                URI baseUri = uriInfo.getBaseUri();
                String uri = baseUri.getScheme() + "://" + baseUri.getHost();
                if (baseUri.getPort() != -1) {
                    uri += ":" + baseUri.getPort();
                }
                validRedirect = uri + validRedirect;
                logger.debugv("replacing relative valid redirect with: {0}", validRedirect);
            }
            resolveValidRedirects.add(validRedirect);
        }
        return resolveValidRedirects;
    }

    private boolean checkSsl() {
        return realm.isSslNotRequired() || uriInfo.getBaseUri().getScheme().equals("https");
    }

    private Response createError(String error, String errorDescription, Response.Status status) {
        Map<String, String> e = new HashMap<String, String>();
        e.put(OAuth2Constants.ERROR, error);
        if (errorDescription != null) {
            e.put(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
        }
        return Response.status(status).entity(e).type("application/json").build();
    }

}
