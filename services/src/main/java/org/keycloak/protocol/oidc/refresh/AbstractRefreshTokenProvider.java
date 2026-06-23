package org.keycloak.protocol.oidc.refresh;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Retry;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;

import org.jboss.logging.Logger;

public abstract class AbstractRefreshTokenProvider implements RefreshTokenProvider {

    private static final Logger logger = Logger.getLogger(AbstractRefreshTokenProvider.class);

    protected Function<String, String> transformScopes(KeycloakSession session, Set<String> requestedScopes) {
        return scope -> {
            if (requestedScopes.contains(scope)) {
                return scope;
            }

            if (Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
                OrganizationScope oldScope = OrganizationScope.valueOfScope(session, scope);
                return oldScope == null ? null : oldScope.resolveName(session, requestedScopes, scope);
            }

            return null;
        };
    }

    protected void createTemporaryExclusiveLockForTokenRefreshOperation(KeycloakSession session, RefreshToken refreshToken, TokenManager tokenManager) {
        String lockId = "refreshLock:" + refreshToken.getSessionId() + ":" + tokenManager.getReuseIdKey(refreshToken);
        Retry.executeWithBackoff((int iteration) -> {
            // This assumes that 60 seconds is the maximum time this operation will take
            if (!session.singleUseObjects().putIfAbsent(lockId, 60)) {
                throw new RuntimeException("Unable to acquire serialization lock for token refresh");
            }

            // Trigger the session provider, to ensure that it enlists first for enlistAfterCompletion
            session.sessions();

            KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    KeycloakModelUtils.runJobInTransaction(factory, s -> s.singleUseObjects().remove(lockId));
                }

                @Override
                protected void rollbackImpl() {
                    KeycloakModelUtils.runJobInTransaction(factory, s -> s.singleUseObjects().remove(lockId));
                }
            });
        }, Duration.of(10, ChronoUnit.SECONDS), 10);
    }

    /**
     * Store information to identify early token refreshes of clients which stress the IAM system.
     */
    protected void storeRefreshTimingInformation(EventBuilder event, RefreshToken refreshToken, AccessToken newToken) {
        long expirationAccessToken = newToken.getExp() - newToken.getIat();
        long ageOfRefreshToken = newToken.getIat() - refreshToken.getIat();
        event.detail(Details.ACCESS_TOKEN_EXPIRATION_TIME, Long.toString(expirationAccessToken));
        event.detail(Details.AGE_OF_REFRESH_TOKEN, Long.toString(ageOfRefreshToken));
    }

    protected void validateTokenReuseForRefresh(KeycloakSession session, RealmModel realm, RefreshToken refreshToken,
                                              TokenManager.TokenValidation validation, TokenManager tokenManager) throws OAuthErrorException {
        if (realm.isRevokeRefreshToken()) {
            AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
            try {
                tokenManager.validateTokenReuse(session, realm, refreshToken, clientSession, true);
                String key = tokenManager.getReuseIdKey(refreshToken);
                int currentCount = clientSession.getRefreshTokenUseCount(key);
                clientSession.setRefreshTokenUseCount(key, currentCount + 1);
            } catch (OAuthErrorException oee) {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Failed validation of refresh token %s due it was used before. Realm: %s, client: %s, user: %s, user session: %s. Will detach client session from user session",
                            refreshToken.getId(), realm.getName(), clientSession.getClient().getClientId(), clientSession.getUserSession().getUser().getUsername(), clientSession.getUserSession().getId());
                }
                clientSession.detachFromUserSession();
                throw oee;
            }
        }
    }
}
