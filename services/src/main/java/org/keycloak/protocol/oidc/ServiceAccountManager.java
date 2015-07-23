package org.keycloak.protocol.oidc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.Cors;

/**
 * Endpoint for authenticate clients and retrieve service accounts
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServiceAccountManager {

    protected static final Logger logger = Logger.getLogger(ServiceAccountManager.class);

    private TokenManager tokenManager;
    private AuthenticationManager authManager;
    private EventBuilder event;
    private HttpRequest request;
    private MultivaluedMap<String, String> formParams;

    private KeycloakSession session;

    private RealmModel realm;
    private HttpHeaders headers;
    private UriInfo uriInfo;
    private ClientConnection clientConnection;

    private ClientModel client;
    private UserModel clientUser;

    public ServiceAccountManager(TokenManager tokenManager, AuthenticationManager authManager, EventBuilder event, HttpRequest request, MultivaluedMap<String, String> formParams, KeycloakSession session) {
        this.tokenManager = tokenManager;
        this.authManager = authManager;
        this.event = event;
        this.request = request;
        this.formParams = formParams;
        this.session = session;

        this.realm = session.getContext().getRealm();
        this.headers = session.getContext().getRequestHeaders();
        this.uriInfo = session.getContext().getUri();
        this.clientConnection = session.getContext().getConnection();
    }

    public Response buildClientCredentialsGrant() {
        authenticateClient();
        checkClient();
        return finishClientAuthorization();
    }

    protected void authenticateClient() {
        // TODO: This should be externalized into pluggable SPI for client authentication (hopefully Authentication SPI can be reused).
        // Right now, just Client Credentials Grants (as per OAuth2 specs) is supported
        String authorizationHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        client = AuthorizeClientUtil.authorizeClient(authorizationHeader, formParams, event, realm);
        event.detail(Details.CLIENT_AUTH_METHOD, Details.CLIENT_AUTH_METHOD_VALUE_CLIENT_CREDENTIALS);
    }

    protected void checkClient() {
        if (client.isBearerOnly()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException("unauthorized_client", "Bearer-only client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (client.isPublicClient()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException("unauthorized_client", "Public client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (!client.isServiceAccountsEnabled()) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorResponseException("unauthorized_client", "Client not enabled to retrieve service account", Response.Status.UNAUTHORIZED);
        }
    }

    protected Response finishClientAuthorization() {
        event.detail(Details.RESPONSE_TYPE, ServiceAccountConstants.CLIENT_AUTH);

        clientUser = session.users().getUserByServiceAccountClient(client);

        if (clientUser == null || client.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, ServiceAccountConstants.CLIENT_ID_PROTOCOL_MAPPER) == null) {
            // May need to handle bootstrap here as well
            logger.infof("Service account user for client '%s' not found or default protocol mapper for service account not found. Creating now", client.getClientId());
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            clientUser = session.users().getUserByServiceAccountClient(client);
        }

        String clientUsername = clientUser.getUsername();
        event.detail(Details.USERNAME, clientUsername);
        event.user(clientUser);

        if (!clientUser.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            throw new ErrorResponseException("invalid_request", "User '" + clientUsername + "' disabled", Response.Status.UNAUTHORIZED);
        }

        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        UserSessionProvider sessions = session.sessions();

        // TODO: Once more requirements are added, clientSession will be likely created earlier by authentication mechanism
        ClientSessionModel clientSession = sessions.createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));

        // TODO: Should rather obtain authMethod from client session?
        UserSessionModel userSession = sessions.createUserSession(realm, clientUser, clientUsername, clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null);
        event.session(userSession);

        TokenManager.attachClientSession(userSession, clientSession);

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, clientConnection.getRemoteAddr());

        AccessTokenResponse res = tokenManager.responseBuilder(realm, client, event, session, userSession, clientSession)
                .generateAccessToken(session, scope, client, clientUser, userSession, clientSession)
                .generateRefreshToken()
                .generateIDToken()
                .build();

        event.success();

        return Cors.add(request, Response.ok(res, MediaType.APPLICATION_JSON_TYPE)).auth().allowedOrigins(client).allowedMethods("POST").exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS).build();
    }

}
