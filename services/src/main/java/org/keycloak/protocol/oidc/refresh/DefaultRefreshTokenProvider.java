package org.keycloak.protocol.oidc.refresh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.SessionExpirationUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.util.TokenUtil;


/**
 * Default refresh token provider. Requires valid user session, which is referenced in the refresh token, to be present in Keycloak storage
 */
public class DefaultRefreshTokenProvider extends AbstractRefreshTokenProvider implements RefreshTokenProvider {

    public DefaultRefreshTokenProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public boolean supports(InitialRefreshTokenContext initialRefreshTokenCtx) {
        return true;
    }

    @Override
    public RefreshToken generateRefreshToken(InitialRefreshTokenContext initialRefreshTokenCtx) {
        ClientSessionContext clientSessionCtx = initialRefreshTokenCtx.clientSessionCtx();
        TokenManager.AccessTokenResponseBuilder responseBuilder = initialRefreshTokenCtx.responseBuilder();
        AccessToken accessToken = responseBuilder.getAccessToken();
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();

        RefreshToken refreshToken = createRefreshToken(accessToken, initialRefreshTokenCtx.confirmation(), DefaultRefreshTokenProviderFactory.PROVIDER_ID);

        clientSession.setTimestamp(refreshToken.getIat().intValue());
        UserSessionModel userSession = clientSession.getUserSession();
        userSession.setLastSessionRefresh(refreshToken.getIat().intValue());
        if (initialRefreshTokenCtx.offlineTokenRequested()) {
            refreshToken.type(TokenUtil.TOKEN_TYPE_OFFLINE);
            if (userSession.getRealm().isOfflineSessionMaxLifespanEnabled()) {
                refreshToken.exp(getExpiration(clientSessionCtx, userSession,true));
            }
            responseBuilder.createOrUpdateOfflineSession();
        } else {
            refreshToken.exp(getExpiration(clientSessionCtx, userSession, false));
        }
        final ClientModel[] requestedAudienceClients = clientSessionCtx.getAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, ClientModel[].class);
        if (requestedAudienceClients != null) {
            refreshToken.getOtherClaims().put(Constants.REQUESTED_AUDIENCE, Arrays.stream(requestedAudienceClients)
                    .map(ClientModel::getClientId)
                    .collect(Collectors.toSet()));
        }

        return refreshToken;
    }

    @Override
    public boolean supports(RefreshTokenContext ctx) {
        RefreshToken refreshToken = ctx.oldRefreshToken();
        return (TokenUtil.TOKEN_TYPE_REFRESH.equals(refreshToken.getType()) || TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType()));
    }

    @Override
    protected TokenManager.TokenValidation validateToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm,
                                                      RefreshToken oldToken, HttpHeaders headers, String scope, ClientModel client,
                                                      TokenManager tokenManager, EventBuilder event) throws OAuthErrorException {
        return tokenManager.validateToken(session, session.getContext().getUri(), connection, realm, oldToken, headers, scope);
    }

    @Override
    protected void afterRefreshTokenGenerated(RefreshTokenContext ctx, TokenManager.AccessTokenResponseBuilder responseBuilder) {
        AuthenticatedClientSessionModel clientSession = responseBuilder.getClientSessionCtx().getClientSession();
        UserSessionModel userSession = clientSession.getUserSession();
        ctx.grant().updateClientSession(clientSession);
        ctx.grant().updateUserSessionFromClientAuth(userSession);
    }

    @Override
    public void revokeToken(AccessToken token, UserModel user, ClientModel client, EventBuilder event) {
        RealmModel realm = session.getContext().getRealm();

        if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(token.getType())) {
            UserSessionModel userSession = session.sessions().getOfflineUserSession(realm, token.getSessionId());
            if (userSession != null) {
                new UserSessionManager(session).removeClientFromOfflineUserSession(realm, userSession, client, user);
            }
        }
        // Always remove "online" session as well if exists to make sure that issued access-tokens are revoked as well
        UserSessionModel userSession = session.sessions().getUserSession(realm, token.getSessionId());
        if (userSession != null) {
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            if (clientSession != null) {
                revokeTokenExchangeSession(userSession, token, event);

                TokenManager.detachClientSession(clientSession);

                // TODO: Might need optimization to prevent loading client sessions from cache in getAuthenticatedClientSessions()
                if (userSession.getAuthenticatedClientSessions().isEmpty()) {
                    session.sessions().removeUserSession(realm, userSession);
                }
            }
        }
    }

    private void revokeTokenExchangeSession(UserSessionModel userSession, AccessToken token, EventBuilder event) {
        Map<String, AuthenticatedClientSessionModel> clientSessionModelMap = userSession.getAuthenticatedClientSessions();
        List<String> revokedClients = new ArrayList<>();
        clientSessionModelMap.forEach((key, clientSessionModel) -> {
            if (clientSessionModel.getNote(Constants.TOKEN_EXCHANGE_SUBJECT_CLIENT + token.getIssuedFor()) != null) {
                revokedClients.add(clientSessionModel.getClient().getClientId());
                TokenManager.detachClientSession(clientSessionModel);
            }
        });
        if (!revokedClients.isEmpty()) {
            event.detail(Details.TOKEN_EXCHANGE_REVOKED_CLIENTS, String.join(",", revokedClients));
        }
    }

    private Long getExpiration(ClientSessionContext clientSessionCtx, UserSessionModel userSession, boolean offline) {
        ClientModel client = clientSessionCtx.getClientSession().getClient();
        RealmModel realm = client.getRealm();
        long expiration = SessionExpirationUtils.calculateClientSessionIdleTimestamp(
                offline, userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSessionCtx.getClientSession().getTimestamp()),
                realm, client);
        long lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(
                offline, userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSessionCtx.getClientSession().getStarted()),
                TimeUnit.SECONDS.toMillis(userSession.getStarted()),
                realm, client);
        expiration = lifespan > 0? Math.min(expiration, lifespan) : expiration;

        return TimeUnit.MILLISECONDS.toSeconds(expiration);
    }

}
