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
import org.keycloak.audit.Events;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.authentication.AuthenticationProviderManager;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ClientConnection;
import org.keycloak.services.ForbiddenException;
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
import org.keycloak.util.Time;

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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
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
    @Context
    protected ProviderSession providerSession;

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

        audit.event(Events.LOGIN).detail(Details.AUTH_METHOD, "oauth_credentials").detail(Details.RESPONSE_TYPE, "token");

        ClientModel client = authorizeClient(authorizationHeader, form, audit);

        if (client.isPublicClient()) {
            // we don't allow public clients to invoke grants/access to prevent phishing attacks
            audit.error(Errors.NOT_ALLOWED);
            throw new ForbiddenException("Public clients are not allowed to invoke grants/access");
        }

        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            audit.error(Errors.USERNAME_MISSING);
            throw new UnauthorizedException("No username");
        }
        audit.detail(Details.USERNAME, username);
        if (!realm.isEnabled()) {
            audit.error(Errors.REALM_DISABLED);
            throw new UnauthorizedException("Disabled realm");
        }

        AuthenticationStatus authenticationStatus = authManager.authenticateForm(clientConnection, realm, form);

        switch (authenticationStatus) {
            case SUCCESS:
                break;
            case ACCOUNT_TEMPORARILY_DISABLED:
            case ACTIONS_REQUIRED:
                audit.error(Errors.USER_TEMPORARILY_DISABLED);
                return Response.status(503).type(MediaType.TEXT_PLAIN).entity("Account temporarily disabled").build();
            case ACCOUNT_DISABLED:
                return Response.status(403).type(MediaType.TEXT_PLAIN).entity("Account disabled").build();
            default:
                audit.error(Errors.INVALID_USER_CREDENTIALS);
                throw new UnauthorizedException("Auth failed");
        }

        UserModel user = realm.getUser(form.getFirst(AuthenticationManager.FORM_USERNAME));
        String scope = form.getFirst(OAuth2Constants.SCOPE);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, audit)
                .generateAccessToken(scope, client, user)
                .generateIDToken()
                .build();

        audit.success();

        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

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

        audit.event(Events.REFRESH_TOKEN);

        ClientModel client = authorizeClient(authorizationHeader, form, audit);
        String refreshToken = form.getFirst(OAuth2Constants.REFRESH_TOKEN);
        AccessToken accessToken = null;
        try {
            accessToken = tokenManager.refreshAccessToken(realm, client, refreshToken, audit);
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, e.getError());
            if (e.getDescription() != null) error.put(OAuth2Constants.ERROR_DESCRIPTION, e.getDescription());
            audit.error(Errors.INVALID_TOKEN);
            throw new BadRequestException("OAuth Error", e, Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build());
        }

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, audit)
                .accessToken(accessToken)
                .generateIDToken()
                .generateRefreshToken().build();

        audit.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").build();
    }

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

        audit.event(Events.LOGIN).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "form")
                .detail(Details.USERNAME, username);

        if (remember) {
            audit.detail(Details.REMEMBER_ME, "true");
        }

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

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

        redirect = verifyRedirectUri(redirect, client);
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
            NewCookie cookie = authManager.createRememberMeCookie(realm, uriInfo);
            response.addNewCookie(cookie);
        } else {
            authManager.expireRememberMeCookie(realm, uriInfo);
        }

        switch (status) {
            case SUCCESS:
            case ACTIONS_REQUIRED:
                UserModel user = KeycloakModelUtils.findUserByNameOrEmail(realm, username);
		        audit.user(user);
                return oauth.processAccessCode(scopeParam, state, redirect, client, user, username, remember, "form", audit);
            case ACCOUNT_TEMPORARILY_DISABLED:
                audit.error(Errors.USER_TEMPORARILY_DISABLED);
                return Flows.forms(realm, uriInfo).setError(Messages.ACCOUNT_TEMPORARILY_DISABLED).setFormData(formData).createLogin();
            case ACCOUNT_DISABLED:
                audit.error(Errors.USER_DISABLED);
                return Flows.forms(realm, uriInfo).setError(Messages.ACCOUNT_DISABLED).setFormData(formData).createLogin();
            case MISSING_TOTP:
                return Flows.forms(realm, uriInfo).setFormData(formData).createLoginTotp();
            case INVALID_USER:
                audit.error(Errors.USER_NOT_FOUND);
                return Flows.forms(realm, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
            default:
                audit.error(Errors.INVALID_USER_CREDENTIALS);
                return Flows.forms(realm, uriInfo).setError(Messages.INVALID_USER).setFormData(formData).createLogin();
        }
    }

    @Path("auth/request/login-actions")
    public RequiredActionsService getRequiredActionsService() {
        RequiredActionsService service = new RequiredActionsService(realm, tokenManager, audit);
        ResteasyProviderFactory.getInstance().injectProperties(service);

        //resourceContext.initResource(service);
        return service;
    }

    @Path("registrations")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processRegister(@QueryParam("client_id") final String clientId,
                                    @QueryParam("scope") final String scopeParam, @QueryParam("state") final String state,
                                    @QueryParam("redirect_uri") String redirect, final MultivaluedMap<String, String> formData) {

        String username = formData.getFirst("username");
        String email = formData.getFirst("email");

        audit.event(Events.REGISTER).client(clientId)
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.USERNAME, username)
                .detail(Details.EMAIL, email)
                .detail(Details.REGISTER_METHOD, "form");

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

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

        redirect = verifyRedirectUri(redirect, client);
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
            return Flows.forms(realm, uriInfo).setError(error).setFormData(formData).createRegistration();
        }

        AuthenticationProviderManager authenticationProviderManager = AuthenticationProviderManager.getManager(realm, providerSession);

        // Validate that user with this username doesn't exist in realm or any authentication provider
        if (realm.getUser(username) != null || authenticationProviderManager.getUser(username) != null) {
            audit.error(Errors.USERNAME_IN_USE);
            return Flows.forms(realm, uriInfo).setError(Messages.USERNAME_EXISTS).setFormData(formData).createRegistration();
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
            String passwordUpdateError = null;
            try {
                passwordUpdateSuccessful = AuthenticationProviderManager.getManager(realm, providerSession).updatePassword(user, formData.getFirst("password"));
                passwordUpdateError = "Password update failed";
            } catch (AuthenticationProviderException ape) {
                passwordUpdateSuccessful = false;
                passwordUpdateError = ape.getMessage();
            }

            // User already registered, but force him to update password
            if (!passwordUpdateSuccessful) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                return Flows.forms(realm, uriInfo).setError(passwordUpdateError).createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
            }
        }

        audit.user(user).success();
        audit.reset();

        return processLogin(clientId, scopeParam, state, redirect, formData);
    }

    @Path("access/codes")
    @OPTIONS
    @Produces("application/json")
    public Response accessCodeToTokenPreflight() {
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    @Path("access/codes")
    @POST
    @Produces("application/json")
    public Response accessCodeToToken(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorizationHeader, final MultivaluedMap<String, String> formData) {
        logger.debug("accessRequest <---");

        if (!checkSsl()) {
            throw new NotAcceptableException("HTTPS required");
        }

        audit.event(Events.CODE_TO_TOKEN);

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

        JWSInput input = new JWSInput(code);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            logger.debug("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Unable to verify code signature");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }
        String key = input.readContentAsString();

        audit.detail(Details.CODE_ID, key);

        AccessCodeEntry accessCode = tokenManager.pullAccessCode(key);
        if (accessCode == null) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Code not found");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        audit.user(accessCode.getUser());

        ClientModel client = authorizeClient(authorizationHeader, formData, audit);

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
        if (!client.getClientId().equals(accessCode.getClient().getClientId())) {
            Map<String, String> res = new HashMap<String, String>();
            res.put(OAuth2Constants.ERROR, "invalid_grant");
            res.put(OAuth2Constants.ERROR_DESCRIPTION, "Auth error");
            audit.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res)
                    .build();
        }

        logger.debug("accessRequest SUCCESS");

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, audit)
                .accessToken(accessCode.getToken())
                .generateIDToken()
                .generateRefreshToken().build();

        audit.success();

        return Cors.add(request, Response.ok(res)).auth().allowedOrigins(client).allowedMethods("POST").build();
    }

    protected ClientModel authorizeClient(String authorizationHeader, MultivaluedMap<String, String> formData, Audit audit) {
        String client_id = null;
        String clientSecret = null;
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

    @Path("login")
    @GET
    public Response loginPage(final @QueryParam("response_type") String responseType,
                              @QueryParam("redirect_uri") String redirect, final @QueryParam("client_id") String clientId,
                              final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state, final @QueryParam("prompt") String prompt) {
        logger.info("TokenService.loginPage");

        audit.event(Events.LOGIN).client(clientId).detail(Details.REDIRECT_URI, redirect).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

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
        redirect = verifyRedirectUri(redirect, client);
        if (redirect == null) {
            audit.error(Errors.INVALID_REDIRECT_URI);
            return oauth.forwardToSecurityFailure("Invalid redirect_uri.");
        }

        logger.info("Checking cookie...");
        UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers);
        if (user != null) {
            logger.debug(user.getLoginName() + " already logged in.");
            audit.user(user).detail(Details.AUTH_METHOD, "sso");
            return oauth.processAccessCode(scopeParam, state, redirect, client, user, null, false, "sso", audit);
        }

        if (prompt != null && prompt.equals("none")) {
            return oauth.redirectError(client, "access_denied", state, redirect);
        }
        logger.info("createLogin() now...");
        return Flows.forms(realm, uriInfo).createLogin();
    }

    @Path("registrations")
    @GET
    public Response registerPage(final @QueryParam("response_type") String responseType,
                                 @QueryParam("redirect_uri") String redirect, final @QueryParam("client_id") String clientId,
                                 final @QueryParam("scope") String scopeParam, final @QueryParam("state") String state) {
        logger.info("**********registerPage()");

        audit.event(Events.REGISTER).client(clientId).detail(Details.REDIRECT_URI, redirect).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

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

        redirect = verifyRedirectUri(redirect, client);
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

        return Flows.forms(realm, uriInfo).createRegistration();
    }

    @Path("logout")
    @GET
    @NoCache
    public Response logout(final @QueryParam("redirect_uri") String redirectUri) {
        // todo do we care if anybody can trigger this?

        audit.event(Events.LOGOUT).detail(Details.REDIRECT_URI, redirectUri);

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        UserModel user = authManager.authenticateIdentityCookie(realm, uriInfo, headers, false);
        if (user != null) {
            logger.infov("Logging out: {0}", user.getLoginName());
            authManager.expireIdentityCookie(realm, uriInfo);
            authManager.expireRememberMeCookie(realm, uriInfo);
            resourceAdminManager.logoutUser(realm, user);

            audit.user(user).success();
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
        audit.event(Events.LOGIN).detail(Details.RESPONSE_TYPE, "code");

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!checkSsl()) {
            return oauth.forwardToSecurityFailure("HTTPS required");
        }

        String code = formData.getFirst(OAuth2Constants.CODE);
        JWSInput input = new JWSInput(code);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            logger.debug("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            audit.error(Errors.INVALID_CODE);
            return oauth.forwardToSecurityFailure("Illegal access code.");
        }
        String key = input.readContentAsString();
        audit.detail(Details.CODE_ID, key);

        AccessCodeEntry accessCodeEntry = tokenManager.getAccessCode(key);
        if (accessCodeEntry == null) {
            audit.error(Errors.INVALID_CODE);
            return oauth.forwardToSecurityFailure("Unknown access code.");
        }

        String redirect = accessCodeEntry.getRedirectUri();
        String state = accessCodeEntry.getState();

        audit.client(accessCodeEntry.getClient())
                .user(accessCodeEntry.getUser())
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, accessCodeEntry.getAuthMethod())
                .detail(Details.REDIRECT_URI, redirect)
                .detail(Details.USERNAME, accessCodeEntry.getUsername());

        if (accessCodeEntry.isRememberMe()) {
            audit.detail(Details.REMEMBER_ME, "true");
        }

        if (formData.containsKey("cancel")) {
            audit.error(Errors.REJECTED_BY_USER);
            return redirectAccessDenied(redirect, state);
        }

        audit.success();

        accessCodeEntry.setExpiration(Time.currentTime() + realm.getAccessCodeLifespan());
        return oauth.redirectAccessCode(accessCodeEntry, state, redirect);
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

    public static String verifyRedirectUri(String redirectUri, ClientModel client) {
        if (redirectUri == null) {
            return client.getRedirectUris().size() == 1 ? client.getRedirectUris().iterator().next() : null;
        } else if (client.getRedirectUris().isEmpty()) {
            if (client.isPublicClient()) {
                logger.error("Client redirect uri must be registered for public client");
                return null;
            }
            return redirectUri;
        } else {
            String r = redirectUri.indexOf('?') != -1 ? redirectUri.substring(0, redirectUri.indexOf('?')) : redirectUri;

            boolean valid = matchesRedirects(client.getRedirectUris(), r);

            if (!valid && r.startsWith(Constants.INSTALLED_APP_URL) && r.indexOf(':', Constants.INSTALLED_APP_URL.length()) >= 0) {
                int i = r.indexOf(':', Constants.INSTALLED_APP_URL.length());

                StringBuilder sb = new StringBuilder();
                sb.append(r.substring(0, i));

                i = r.indexOf('/', i);
                if (i >= 0) {
                    sb.append(r.substring(i));
                }

                r = sb.toString();

                valid = matchesRedirects(client.getRedirectUris(), r);
            }

            return valid ? redirectUri : null;
        }
    }

    private boolean checkSsl() {
        if (realm.isSslNotRequired()) {
            return true;
        }

        return uriInfo.getBaseUri().getScheme().equals("https");
    }

}
