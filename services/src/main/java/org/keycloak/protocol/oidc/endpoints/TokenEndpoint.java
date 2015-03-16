package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.Cors;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenEndpoint {

    private static final Logger logger = Logger.getLogger(TokenEndpoint.class);
    private MultivaluedMap<String, String> formParams;
    private ClientModel client;

    private enum Action {
        AUTHORIZATION_CODE, REFRESH_TOKEN, PASSWORD
    }

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final AuthenticationManager authManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private Action action;

    private String grantType;

    private String legacyGrantType;

    public TokenEndpoint(TokenManager tokenManager, AuthenticationManager authManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.authManager = authManager;
        this.realm = realm;
        this.event = event;
    }

    @POST
    public Response build() {
        switch (action) {
            case AUTHORIZATION_CODE:
                return buildAuthorizationCodeAccessTokenResponse();
            case REFRESH_TOKEN:
                return buildRefreshToken();
            case PASSWORD:
                return buildResourceOwnerPasswordCredentialsGrant();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    @OPTIONS
    public Response preflight() {
        if (logger.isDebugEnabled()) {
            logger.debugv("CORS preflight from: {0}", headers.getRequestHeaders().getFirst("Origin"));
        }
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    /**
     * @deprecated
     */
    public TokenEndpoint legacy(String legacyGrantType) {
        logger.warnv("Invoking deprecated endpoint {0}", uriInfo.getRequestUri());
        this.legacyGrantType = legacyGrantType;
        return this;
    }

    public TokenEndpoint init() {
        formParams = request.getDecodedFormParameters();
        grantType = formParams.getFirst(OIDCLoginProtocol.GRANT_TYPE_PARAM);

        checkSsl();
        checkRealm();
        checkGrantType();
        checkClient();

        return this;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        String authorizationHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        client = AuthorizeClientUtil.authorizeClient(authorizationHeader, formParams, event, realm);

        if ((client instanceof ApplicationModel) && ((ApplicationModel) client).isBearerOnly()) {
            throw new ErrorResponseException("invalid_client", "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }

    private void checkGrantType() {
        if (grantType == null) {
            if (legacyGrantType != null) {
                grantType = legacyGrantType;
            } else {
                throw new ErrorResponseException("invalid_request", "Missing query parameter: " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
            }
        }

        if (grantType.equals(OAuth2Constants.AUTHORIZATION_CODE)) {
            event.event(EventType.CODE_TO_TOKEN);
            action = Action.AUTHORIZATION_CODE;
        } else if (grantType.equals(OAuth2Constants.REFRESH_TOKEN)) {
            event.event(EventType.REFRESH_TOKEN);
            action = Action.REFRESH_TOKEN;
        } else if (grantType.equals(OAuth2Constants.PASSWORD)) {
            event.event(EventType.LOGIN);
            action = Action.PASSWORD;
        } else {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Invalid " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }
    }

    public Response buildAuthorizationCodeAccessTokenResponse() {
        String code = formParams.getFirst(OAuth2Constants.CODE);
        if (code == null) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException("invalid_request", "Missing parameter: " + OAuth2Constants.CODE, Response.Status.BAD_REQUEST);
        }

        ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
        if (accessCode == null) {
            String[] parts = code.split("\\.");
            if (parts.length == 2) {
                try {
                    event.detail(Details.CODE_ID, new String(parts[1]));
                } catch (Throwable t) {
                }
            }
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException("invalid_grant", "Code not found", Response.Status.BAD_REQUEST);
        }

        ClientSessionModel clientSession = accessCode.getClientSession();
        event.detail(Details.CODE_ID, clientSession.getId());
        if (!accessCode.isValid(ClientSessionModel.Action.CODE_TO_TOKEN)) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException("invalid_grant", "Code is expired", Response.Status.BAD_REQUEST);
        }

        accessCode.setAction(null);
        UserSessionModel userSession = clientSession.getUserSession();
        event.user(userSession.getUser());
        event.session(userSession.getId());

        String redirectUri = clientSession.getNote(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        if (redirectUri != null && !redirectUri.equals(formParams.getFirst(OAuth2Constants.REDIRECT_URI))) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException("invalid_grant", "Incorrect redirect_uri", Response.Status.BAD_REQUEST);
        }

        if (!client.getClientId().equals(clientSession.getClient().getClientId())) {
            event.error(Errors.INVALID_CODE);
            throw new ErrorResponseException("invalid_grant", "Auth error", Response.Status.BAD_REQUEST);
        }

        UserModel user = session.users().getUserById(userSession.getUser().getId(), realm);
        if (user == null) {
            event.error(Errors.USER_NOT_FOUND);
            throw new ErrorResponseException("invalid_grant", "User not found", Response.Status.BAD_REQUEST);
        }

        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new ErrorResponseException("invalid_grant", "User disabled", Response.Status.BAD_REQUEST);
        }

        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            event.error(Errors.USER_SESSION_NOT_FOUND);
            throw new ErrorResponseException("invalid_grant", "Session not active", Response.Status.BAD_REQUEST);
        }

        String adapterSessionId = formParams.getFirst(AdapterConstants.APPLICATION_SESSION_STATE);
        if (adapterSessionId != null) {
            String adapterSessionHost = formParams.getFirst(AdapterConstants.APPLICATION_SESSION_HOST);
            logger.debugf("Adapter Session '%s' saved in ClientSession for client '%s'. Host is '%s'", adapterSessionId, client.getClientId(), adapterSessionHost);

            event.detail(AdapterConstants.APPLICATION_SESSION_STATE, adapterSessionId);
            clientSession.setNote(AdapterConstants.APPLICATION_SESSION_STATE, adapterSessionId);
            event.detail(AdapterConstants.APPLICATION_SESSION_HOST, adapterSessionHost);
            clientSession.setNote(AdapterConstants.APPLICATION_SESSION_HOST, adapterSessionHost);
        }

        AccessToken token = tokenManager.createClientAccessToken(session, accessCode.getRequestedRoles(), realm, client, user, userSession, clientSession);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .accessToken(token)
                .generateIDToken()
                .generateRefreshToken().build();

        event.success();

        return Cors.add(request, Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildRefreshToken() {
        String refreshToken = formParams.getFirst(OAuth2Constants.REFRESH_TOKEN);
        if (refreshToken == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "No refresh token", Response.Status.BAD_REQUEST);
        }

        AccessTokenResponse res;
        try {
            res = tokenManager.refreshAccessToken(session, uriInfo, clientConnection, realm, client, refreshToken, event, headers);
        } catch (OAuthErrorException e) {
            event.error(Errors.INVALID_TOKEN);
            throw new ErrorResponseException(e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
        }

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

    public Response buildResourceOwnerPasswordCredentialsGrant() {
        if (!realm.isPasswordCredentialGrantAllowed()) {
            throw new ErrorResponseException("not_enabled", "Direct Grant REST API not enabled", Response.Status.FORBIDDEN);
        }

        event.detail(Details.AUTH_METHOD, "oauth_credentials").detail(Details.RESPONSE_TYPE, "token");

        String username = formParams.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            event.error(Errors.USERNAME_MISSING);
            throw new ErrorResponseException("invalid_request", "Missing parameter: username", Response.Status.UNAUTHORIZED);
        }
        event.detail(Details.USERNAME, username);

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
        if (user != null) event.user(user);

        AuthenticationManager.AuthenticationStatus authenticationStatus = authManager.authenticateForm(session, clientConnection, realm, formParams);
        Map<String, String> err;

        switch (authenticationStatus) {
            case SUCCESS:
                break;
            case ACCOUNT_TEMPORARILY_DISABLED:
            case ACTIONS_REQUIRED:
                event.error(Errors.USER_TEMPORARILY_DISABLED);
                throw new ErrorResponseException("invalid_grant", "Account temporarily disabled", Response.Status.BAD_REQUEST);
            case ACCOUNT_DISABLED:
                event.error(Errors.USER_DISABLED);
                throw new ErrorResponseException("invalid_grant", "Account disabled", Response.Status.BAD_REQUEST);
            default:
                event.error(Errors.INVALID_USER_CREDENTIALS);
                throw new ErrorResponseException("invalid_grant", "Invalid user credentials", Response.Status.UNAUTHORIZED);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        UserSessionProvider sessions = session.sessions();

        UserSessionModel userSession = sessions.createUserSession(realm, user, username, clientConnection.getRemoteAddr(), "oauth_credentials", false);
        event.session(userSession);

        ClientSessionModel clientSession = sessions.createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);

        TokenManager.attachClientSession(userSession, clientSession);

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .generateAccessToken(session, scope, client, user, userSession, clientSession)
                .generateRefreshToken()
                .generateIDToken()
                .build();

        event.success();

        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

}