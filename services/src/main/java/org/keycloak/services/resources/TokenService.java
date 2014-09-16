package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.NotAcceptableException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AccessCode;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;
import org.keycloak.representations.PasswordToken;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.Base64Url;
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
import javax.ws.rs.core.Cookie;
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
    private EventBuilder event;
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
    protected ClientConnection clientConnection;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    private ResourceAdminManager resourceAdminManager = new ResourceAdminManager();

    public TokenService(RealmModel realm, TokenManager tokenManager, EventBuilder event, AuthenticationManager authManager) {
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.event = event;
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

    public static UriBuilder validateAccessTokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(TokenService.class, "validateAccessToken");
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
            return createError("not_enabled", "Direct Grant REST API not enabled", Response.Status.FORBIDDEN);
        }

        event.event(EventType.LOGIN).detail(Details.AUTH_METHOD, "oauth_credentials").detail(Details.RESPONSE_TYPE, "token");

        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            event.error(Errors.USERNAME_MISSING);
            throw new UnauthorizedException("No username");
        }
        event.detail(Details.USERNAME, username);

        UserModel user = session.users().getUserByUsername(username, realm);
        if (user != null) event.user(user);

        ClientModel client = authorizeClient(authorizationHeader, form, event);

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return createError("realm_disabled", "Realm is disabled", Response.Status.UNAUTHORIZED);
        }

        AuthenticationStatus authenticationStatus = authManager.authenticateForm(session, clientConnection, realm, form);
        Map<String, String> err;

        switch (authenticationStatus) {
            case SUCCESS:
                break;
            case ACCOUNT_TEMPORARILY_DISABLED:
            case ACTIONS_REQUIRED:
                err = new HashMap<String, String>();
                err.put(OAuth2Constants.ERROR, "invalid_grant");
                err.put(OAuth2Constants.ERROR_DESCRIPTION, "AccountProvider temporarily disabled");
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                        .build();
            case ACCOUNT_DISABLED:
                err = new HashMap<String, String>();
                err.put(OAuth2Constants.ERROR, "invalid_grant");
                err.put(OAuth2Constants.ERROR_DESCRIPTION, "AccountProvider disabled");
                event.error(Errors.USER_DISABLED);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                        .build();
            default:
                err = new HashMap<String, String>();
                err.put(OAuth2Constants.ERROR, "invalid_grant");
                err.put(OAuth2Constants.ERROR_DESCRIPTION, "Invalid user credentials");
                event.error(Errors.INVALID_USER_CREDENTIALS);
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                        .build();
        }

        String scope = form.getFirst(OAuth2Constants.SCOPE);

        UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "oauth_credentials", false);
        event.session(userSession);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event)
                .generateAccessToken(scope, client, user, userSession)
                .generateRefreshToken()
                .generateIDToken()
                .build();

        event.success();

        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Validate encoded access token.
     *
     * @param tokenString
     * @return Unmarshalled token
     */
    @Path("validate")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateAccessToken(@QueryParam("access_token") String tokenString) {
        if (!checkSsl()) {
            return createError("https_required", "HTTPS required", Response.Status.FORBIDDEN);
        }
        event.event(EventType.VALIDATE_ACCESS_TOKEN);
        AccessToken token = null;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName());
        } catch (Exception e) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_GRANT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Token invalid");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }
        event.user(token.getSubject()).session(token.getSessionState()).detail(Details.VALIDATE_ACCESS_TOKEN, token.getId());

        if (token.isExpired()
                || token.getIssuedAt() < realm.getNotBefore()
                ) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_GRANT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Token expired");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }


        UserModel user = session.users().getUserById(token.getSubject(), realm);
        if (user == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_GRANT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "User does not exist");
            event.error(Errors.USER_NOT_FOUND);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }

        if (!user.isEnabled()) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_GRANT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "User disabled");
            event.error(Errors.USER_DISABLED);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }

        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionState());
        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_GRANT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Expired session");
            event.error(Errors.USER_SESSION_NOT_FOUND);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }

        ClientModel client = realm.findClient(token.getIssuedFor());
        if (client == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_CLIENT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Issued for client no longer exists");
            event.error(Errors.CLIENT_NOT_FOUND);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();

        }

        if (token.getIssuedAt() < client.getNotBefore()) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_CLIENT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Issued for client no longer exists");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }

        try {
            tokenManager.verifyAccess(token, realm, client, user);
        } catch (OAuthErrorException e) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_SCOPE);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Role mappings have changed");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();

        }
        return Response.ok(token, MediaType.APPLICATION_JSON_TYPE).build();
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
        if (!checkSsl()) {
            return createError("https_required", "HTTPS required", Response.Status.FORBIDDEN);
        }

        event.event(EventType.REFRESH_TOKEN);

        ClientModel client = authorizeClient(authorizationHeader, form, event);
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_REQUEST);
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "No refresh token");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }
        AccessToken accessToken;
        try {
            accessToken = tokenManager.refreshAccessToken(session, uriInfo, clientConnection, realm, client, refreshToken, event);
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, e.getError());
            if (e.getDescription() != null) error.put(OAuth2Constants.ERROR_DESCRIPTION, e.getDescription());
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event)
                .accessToken(accessToken)
                .generateIDToken()
                .generateRefreshToken().build();

        event.success();

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
        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);

        String rememberMe = formData.getFirst("rememberMe");
        boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");

        event.event(EventType.LOGIN).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.USERNAME, username);

        if (remember) {
            event.detail(Details.REMEMBER_ME, "true");
        }

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled.");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }
        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (formData.containsKey("cancel")) {
            event.error(Errors.REJECTED_BY_USER);
            return oauth.redirectError(client, "access_denied", state, redirect);
        }

        AuthenticationStatus status = authManager.authenticateForm(session, clientConnection, realm, formData);

        if (remember) {
            authManager.createRememberMeCookie(realm, username, uriInfo, clientConnection);
        } else {
            authManager.expireRememberMeCookie(realm, uriInfo, clientConnection);
        }

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
        if (user != null) {
            event.user(user);
        }

        switch (status) {
            case SUCCESS:
            case ACTIONS_REQUIRED:
                UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "form", remember);
		        event.session(userSession);

                return oauth.processAccessCode(scopeParam, state, redirect, client, user, userSession, event);
            case ACCOUNT_TEMPORARILY_DISABLED:
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                return Flows.forms(this.session, realm, client, uriInfo).setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).setFormData(formData).createLogin();
            case ACCOUNT_DISABLED:
                event.error(Errors.USER_DISABLED);
                return Flows.forms(this.session, realm, client, uriInfo).setError(Messages.ACCOUNT_DISABLED).setFormData(formData).createLogin();
            case MISSING_TOTP:
                formData.remove(CredentialRepresentation.PASSWORD);

                String passwordToken = new JWSBuilder().jsonContent(new PasswordToken(realm.getName(), user.getId())).rsa256(realm.getPrivateKey());
                formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

                return Flows.forms(this.session, realm, client, uriInfo).setFormData(formData).createLoginTotp();
            case INVALID_USER:
                event.error(Errors.USER_NOT_FOUND);
                return Flows.forms(this.session, realm, client, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
            default:
                event.error(Errors.INVALID_USER_CREDENTIALS);
                return Flows.forms(this.session, realm, client, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
        }
    }

    @Path("auth/request/login-actions")
    public RequiredActionsService getRequiredActionsService() {
        RequiredActionsService service = new RequiredActionsService(realm, tokenManager, event);
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

        event.event(EventType.REGISTER).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
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
            event.error(Errors.INVALID_REGISTRATION);
            return Flows.forms(session, realm, client, uriInfo).setError(error).setFormData(formData).createRegistration();
        }

        // Validate that user with this username doesn't exist in realm or any federation provider
        if (session.users().getUserByUsername(username, realm) != null) {
            event.error(Errors.USERNAME_IN_USE);
            return Flows.forms(session, realm, client, uriInfo).setError(Messages.USERNAME_EXISTS).setFormData(formData).createRegistration();
        }

        // Validate that user with this email doesn't exist in realm or any federation provider
        if (session.users().getUserByEmail(email, realm) != null) {
            event.error(Errors.EMAIL_IN_USE);
            return Flows.forms(session, realm, client, uriInfo).setError(Messages.EMAIL_EXISTS).setFormData(formData).createRegistration();
        }

        UserModel user = session.users().addUser(realm, username);
        user.setEnabled(true);
        user.setFirstName(formData.getFirst("firstName"));
        user.setLastName(formData.getFirst("lastName"));

        user.setEmail(email);

        if (requiredCredentialTypes.contains(CredentialRepresentation.PASSWORD)) {
            UserCredentialModel credentials = new UserCredentialModel();
            credentials.setType(CredentialRepresentation.PASSWORD);
            credentials.setValue(formData.getFirst("password"));

            boolean passwordUpdateSuccessful;
            String passwordUpdateError = null;
            try {
                session.users().updateCredential(realm, user, UserCredentialModel.password(formData.getFirst("password")));
                passwordUpdateSuccessful = true;
            } catch (Exception ape) {
                passwordUpdateSuccessful = false;
                passwordUpdateError = ape.getMessage();
            }

            // User already registered, but force him to update password
            if (!passwordUpdateSuccessful) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                return Flows.forms(session, realm, client, uriInfo).setError(passwordUpdateError).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            }
        }

        event.user(user).success();
        event.reset();

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
        if (logger.isDebugEnabled()) {
            logger.debugv("cors request from: {0}", request.getHttpHeaders().getRequestHeaders().getFirst("Origin"));
        }
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
        if (!checkSsl()) {
            throw new ForbiddenException("HTTPS required");
        }

        event.event(EventType.CODE_TO_TOKEN);

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new UnauthorizedException("Realm not enabled");
        }

        String code = formData.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_request");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "code not specified");
            event.error(Errors.INVALID_CODE);
            throw new BadRequestException("Code not specified", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        AccessCode accessCode = AccessCode.parse(code, session, realm);
        if (accessCode == null) {
            String[] parts = code.split("\\.");
            if (parts.length == 2) {
                try {
                    event.detail(Details.CODE_ID, new String(Base64Url.decode(parts[1])));
                } catch (Throwable t) {
                }
            }
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Code not found");
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        event.detail(Details.CODE_ID, accessCode.getCodeId());
        if (!accessCode.isValid(ClientSessionModel.Action.CODE_TO_TOKEN)) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Code is expired");
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        accessCode.setAction(null);

        event.user(accessCode.getUser());
        event.session(accessCode.getSessionState());

        ClientModel client = authorizeClient(authorizationHeader, formData, event);

        if (!client.getClientId().equals(accessCode.getClient().getClientId())) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Auth error");
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        UserModel user = session.users().getUserById(accessCode.getUser().getId(), realm);
        if (user == null) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "User not found");
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        if (!user.isEnabled()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "User disabled");
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        UserSessionModel userSession = session.sessions().getUserSession(realm, accessCode.getSessionState());
        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticationManager.logout(session, realm, userSession, uriInfo, clientConnection);
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Session not active");
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        AccessToken token = tokenManager.createClientAccessToken(accessCode.getRequestedRoles(), realm, client, user, userSession);

        try {
            tokenManager.verifyAccess(token, realm, client, user);
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, e.getError());
            if (e.getDescription() != null) error.put(OAuth2Constants.ERROR_DESCRIPTION, e.getDescription());
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event)
                .accessToken(token)
                .generateIDToken()
                .generateRefreshToken().build();

        event.success();

        return Cors.add(request, Response.ok(res)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    protected ClientModel authorizeClient(String authorizationHeader, MultivaluedMap<String, String> formData, EventBuilder event) {
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
            client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
            clientSecret = formData.getFirst("client_secret");
        }

        if (client_id == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Could not find client");
            throw new BadRequestException("Could not find client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        event.client(client_id);

        ClientModel client = realm.findClient(client_id);
        if (client == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Could not find client");
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new BadRequestException("Could not find client", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if (!client.isEnabled()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Client is not enabled");
            event.error(Errors.CLIENT_DISABLED);
            throw new BadRequestException("Client is not enabled", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if ( (client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, "invalid_client");
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "Bearer-only not allowed");
            event.error(Errors.INVALID_CLIENT);
            throw new BadRequestException("Bearer-only not allowed", Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        if (!client.isPublicClient()) {
            if (clientSecret == null || !client.validateSecret(clientSecret)) {
                Map<String, String> error = new HashMap<String, String>();
                error.put(OAuth2Constants.ERROR, "unauthorized_client");
                event.error(Errors.INVALID_CLIENT_CREDENTIALS);
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
                              final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state, final @QueryParam("prompt") String prompt,
                              final @QueryParam("login_hint") String loginHint) {
        event.event(EventType.LOGIN).client(clientId).detail(Details.REDIRECT_URI, redirect).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }
        if ( (client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            return oauth.forwardToSecurityFailure("Bearer-only applications are not allowed to initiate browser login");
        }
        if (client.isDirectGrantsOnly()) {
            event.error(Errors.NOT_ALLOWED);
            return oauth.forwardToSecurityFailure("direct-grants-only clients are not allowed to initiate browser login");
        }
        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers);
        if (authResult != null) {
            UserModel user = authResult.getUser();
            UserSessionModel session = authResult.getSession();

            event.user(user).session(session).detail(Details.AUTH_METHOD, "sso");
            return oauth.processAccessCode(scopeParam, state, redirect, client, user, session, event);
        }

        if (prompt != null && prompt.equals("none")) {
            return oauth.redirectError(client, "access_denied", state, redirect);
        }

        LoginFormsProvider forms = Flows.forms(session, realm, client, uriInfo);

        String rememberMeUsername = null;
        Cookie rememberMeCookie = headers.getCookies().get(AuthenticationManager.KEYCLOAK_REMEMBER_ME);
        if (rememberMeCookie != null && !"".equals(rememberMeCookie.getValue())) {
            rememberMeUsername = rememberMeCookie.getValue();
        }

        if (loginHint != null || rememberMeUsername != null) {
            MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();

            if (loginHint != null) {
                formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
            } else {
                formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                formData.add("rememberMe", "on");
            }

            forms.setFormData(formData);
        }

        return forms.createLogin();
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
        event.event(EventType.REGISTER).client(clientId).detail(Details.REDIRECT_URI, redirect).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled");
        }
        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        redirect = verifyRedirectUri(uriInfo, redirect, realm, client);
        if (redirect == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return oauth.forwardToSecurityFailure("Registration not allowed");
        }

        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        return Flows.forms(session, realm, client, uriInfo).createRegistration();
    }

    /**
     * Logout user session.  User must be logged in via a session cookie.
     *
     * @param redirectUri
     * @return
     */
    @Path("logout")
    @GET
    @NoCache
    public Response logout(final @QueryParam("redirect_uri") String redirectUri) {
        event.event(EventType.LOGOUT);
        if (redirectUri != null) {
            event.detail(Details.REDIRECT_URI, redirectUri);
        }
        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers, false);
        if (authResult != null) {
            logout(authResult.getSession());
        }

        if (redirectUri != null) {
            String validatedRedirect = verifyRealmRedirectUri(uriInfo, redirectUri, realm);
            if (validatedRedirect == null) {
                OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);
                return oauth.forwardToSecurityFailure("Invalid redirect uri.");
            }
            return Response.status(302).location(UriBuilder.fromUri(validatedRedirect).build()).build();
        } else {
            return Response.ok().build();
        }
    }

    /**
     * Logout a session via a non-browser invocation.  Similar signature to refresh token except there is no grant_type.
     * You must pass in the refresh token and
     * authenticate the client if it is not public.
     *
     * If the client is a confidential client
     * you must include the client-id (application name or oauth client name) and secret in an Basic Auth Authorization header.
     *
     * If the client is a public client, then you must include a "client_id" form parameter with the app's or oauth client's name.
     *
     * returns 204 if successful, 400 if not with a json error response.
     *
     * @param authorizationHeader
     * @param form
     * @return
     */
    @Path("logout")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response logoutToken(final @HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                       final MultivaluedMap<String, String> form) {
        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        event.event(EventType.LOGOUT);

        ClientModel client = authorizeClient(authorizationHeader, form, event);
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_REQUEST);
            error.put(OAuth2Constants.ERROR_DESCRIPTION, "No refresh token");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }
        try {
            RefreshToken token = tokenManager.verifyRefreshToken(realm, refreshToken);
            UserSessionModel userSessionModel = session.sessions().getUserSession(realm, token.getSessionState());
            if (userSessionModel != null) {
                logout(userSessionModel);
            }
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, e.getError());
            if (e.getDescription() != null) error.put(OAuth2Constants.ERROR_DESCRIPTION, e.getDescription());
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }
       return Cors.add(request, Response.noContent()).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    private void logout(UserSessionModel userSession) {
        authManager.logout(session, realm, userSession, uriInfo, clientConnection);
        event.user(userSession.getUser()).session(userSession).success();
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
        event.event(EventType.LOGIN).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, clientConnection, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        String code = formData.getFirst(OAuth2Constants.CODE);

        AccessCode accessCode = AccessCode.parse(code, session, realm);
        if (accessCode == null || !accessCode.isValid(ClientSessionModel.Action.OAUTH_GRANT)) {
            event.error(Errors.INVALID_CODE);
            return oauth.forwardToSecurityFailure("Invalid access code.");
        }
        event.detail(Details.CODE_ID, accessCode.getCodeId());

        String redirect = accessCode.getRedirectUri();
        String state = accessCode.getState();

        event.client(accessCode.getClient())
                .user(accessCode.getUser())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.REDIRECT_URI, redirect);

        UserSessionModel userSession = session.sessions().getUserSession(realm, accessCode.getSessionState());
        if (userSession != null) {
            event.detail(Details.AUTH_METHOD, userSession.getAuthMethod());
            event.detail(Details.USERNAME, userSession.getLoginUsername());
            if (userSession.isRememberMe()) {
                event.detail(Details.REMEMBER_ME, "true");
            }
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticationManager.logout(session, realm, userSession, uriInfo, clientConnection);
            event.error(Errors.INVALID_CODE);
            return oauth.forwardToSecurityFailure("Session not active");
        }
        event.session(userSession);

        if (formData.containsKey("cancel")) {
            event.error(Errors.REJECTED_BY_USER);
            return redirectAccessDenied(redirect, state);
        }

        event.success();

        accessCode.setAction(ClientSessionModel.Action.CODE_TO_TOKEN);
        return oauth.redirectAccessCode(accessCode, userSession, state, redirect);
    }

    @Path("oauth/oob")
    @GET
    public Response installedAppUrnCallback(final @QueryParam("code") String code, final @QueryParam("error") String error, final @QueryParam("error_description") String errorDescription) {
        LoginFormsProvider forms = Flows.forms(session, realm, null, uriInfo);
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

    public static Set<String> getValidateRedirectUris(RealmModel realm) {
        Set<String> redirects = new HashSet<String>();
        for (ApplicationModel client : realm.getApplications()) {
            for (String redirect : client.getRedirectUris()) {
                redirects.add(redirect);
            }
        }
        for (OAuthClientModel client : realm.getOAuthClients()) {
            for (String redirect : client.getRedirectUris()) {
                redirects.add(redirect);
            }
        }
        return redirects;
    }

    public static String verifyRealmRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm) {
        Set<String> validRedirects = getValidateRedirectUris(realm);
        return verifyRedirectUri(uriInfo, redirectUri, realm, validRedirects);
    }

    public static String verifyRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm, ClientModel client) {
        Set<String> validRedirects = client.getRedirectUris();
        return verifyRedirectUri(uriInfo, redirectUri, realm, validRedirects);
    }

    public static String verifyRedirectUri(UriInfo uriInfo, String redirectUri, RealmModel realm, Set<String> validRedirects) {
        if (redirectUri == null) {
            if (validRedirects.size() != 1) return null;
            String validRedirect = validRedirects.iterator().next();
            int idx = validRedirect.indexOf("/*");
            if (idx > -1) {
                validRedirect = validRedirect.substring(0, idx);
            }
            redirectUri = validRedirect;
        } else if (validRedirects.isEmpty()) {
            logger.debug("No Redirect URIs supplied");
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
            if (valid && redirectUri.startsWith("/")) {
                redirectUri = relativeToAbsoluteURI(uriInfo, redirectUri);
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
            resolveValidRedirects.add(validRedirect); // add even relative urls.
            if (validRedirect.startsWith("/")) {
                validRedirect = relativeToAbsoluteURI(uriInfo, validRedirect);
                logger.debugv("replacing relative valid redirect with: {0}", validRedirect);
                resolveValidRedirects.add(validRedirect);
            }
        }
        return resolveValidRedirects;
    }

    public static String relativeToAbsoluteURI(UriInfo uriInfo, String relative) {
        URI baseUri = uriInfo.getBaseUri();
        String uri = baseUri.getScheme() + "://" + baseUri.getHost();
        if (baseUri.getPort() != -1) {
            uri += ":" + baseUri.getPort();
        }
        relative = uri + relative;
        return relative;
    }

    private boolean checkSsl() {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
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
