package org.keycloak.protocol.oidc.refresh;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SessionLookup;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.DefaultKeycloakTransactionManager;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;

public abstract class AbstractRefreshTokenProvider implements RefreshTokenProvider {

    private static final Logger logger = Logger.getLogger(AbstractRefreshTokenProvider.class);
    private static final long REFRESH_LOCK_LIFESPAN_SECONDS = 15;
    private static final long COMPENSATION_LOCK_LIFESPAN_SECONDS = 30;
    private static final long DIRECT_COMPENSATION_MAX_LOCK_AGE_SECONDS = 4;
    private static final String TOKEN_REUSE_ROLLBACK_ATTRIBUTE =
            AbstractRefreshTokenProvider.class.getName() + ".tokenReuseRollback";

    protected final KeycloakSession session;

    protected AbstractRefreshTokenProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public TokenManager.AccessTokenResponseBuilder refreshAccessToken(RefreshTokenContext ctx) throws OAuthErrorException {
        RealmModel realm = ctx.grantContext().getRealm();
        TokenManager tokenManager = ctx.tokenManager();
        RefreshToken oldRefreshToken = ctx.oldRefreshToken();
        EventBuilder event = ctx.grantContext().getEvent();
        ClientModel authorizedClient = ctx.grantContext().getClient();
        String scopeParameter = ctx.scopeParameter();

        if (realm.isRevokeRefreshToken()) {
            // If refresh tokens are revoked, we need to serialize all requests to avoid wrong conclusions.
            // This needs to be called before we load the user session from the database or the cache
            createTemporaryExclusiveLockForTokenRefreshOperation(session, realm, oldRefreshToken, tokenManager);
        }

        event.session(oldRefreshToken.getSessionState())
                .detail(Details.REFRESH_TOKEN_ID, oldRefreshToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, oldRefreshToken.getType());

        String providerIdDetail = oldRefreshToken.getProvider() != null ? oldRefreshToken.getProvider() : getProviderId();
        event.detail(Details.REFRESH_TOKEN_PROVIDER_ID, providerIdDetail);

        if (oldRefreshToken.getSubject() != null) {
            event.detail(Details.REFRESH_TOKEN_SUB, oldRefreshToken.getSubject());
        }

        // Setup clientScopes from refresh token to the context
        String oldTokenScope = oldRefreshToken.getScope();
        //The requested scope MUST NOT include any scope not originally granted by the resource owner
        //if scope parameter is not null, remove every scope that is not part of scope parameter
        if (scopeParameter != null && ! scopeParameter.isEmpty()) {
            Set<String> scopeParamScopes = Arrays.stream(scopeParameter.split(" ")).collect(Collectors.toSet());
            oldTokenScope = Arrays.stream(oldTokenScope.split(" "))
                    .map(transformScopes(session, scopeParamScopes))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
        }

        TokenManager.TokenValidation validation = validateToken(session, session.getContext().getUri(), ctx.grantContext().getClientConnection(), realm,
                                                                oldRefreshToken, ctx.grantContext().getHeaders(), oldTokenScope, authorizedClient, tokenManager, event);
        UserModel user = validation.user;
        ClientSessionContext clientSessionCtx = validation.clientSessionCtx;
        UserSessionModel userSession = validation.userSession;

        event.session(userSession);
        tokenManager.validateSelectedOrganization(session, oldRefreshToken, user);

        try {
            TokenVerifier.createWithoutSignature(oldRefreshToken)
                    .withChecks(TokenManager.NotBeforeCheck.forModel(realm), TokenManager.NotBeforeCheck.forModel(authorizedClient), TokenManager.NotBeforeCheck.forModel(session, realm, user))
                    .verify();
        } catch (VerificationException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
        }

        // Check user didn't revoke granted consent
        if (!TokenManager.verifyConsentStillAvailable(session, user, authorizedClient, clientSessionCtx.getClientSession(), oldTokenScope)) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user");
        }

        if (oldRefreshToken.getNonce() != null) {
            clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, oldRefreshToken.getNonce());
        }
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        clientSessionCtx.setAttribute(DefaultClientSessionContext.ORIGINAL_REFRESH_TOKEN_SCOPE, oldRefreshToken.getScope());

        final Collection<String> requestedAud = (Collection<String>) oldRefreshToken.getOtherClaims().get(Constants.REQUESTED_AUDIENCE);
        if (requestedAud != null) {
            clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS,
                    requestedAud.stream()
                            .map(clientId -> session.clients().getClientByClientId(realm, clientId))
                            .filter(Objects::nonNull)
                            .filter(ClientModel::isEnabled)
                            .toArray(ClientModel[]::new));
        }

        // recreate token.
        AccessToken newToken = tokenManager.createClientAccessToken(session, realm, authorizedClient, user, userSession, clientSessionCtx, userSession.isOffline());

        session.getContext().setUserSession(validation.userSession);
        AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(authorizedClient);

        // validate authorizedClient is same as validated client
        if (!clientSession.getClient().getId().equals(authorizedClient.getId())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token. Token client and authorized client don't match");
        }

        validateTokenReuseForRefresh(session, realm, oldRefreshToken, validation, tokenManager);

        event.user(validation.userSession.getUser());

        if (oldRefreshToken.getAuthorization() != null) {
            newToken.setAuthorization(oldRefreshToken.getAuthorization());
        }

        validation.clientSessionCtx.setAttribute(OAuth2Constants.RESOURCE, ctx.resourceParameter());

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, authorizedClient, event, session,
                validation.userSession, validation.clientSessionCtx).offlineToken( TokenUtil.TOKEN_TYPE_OFFLINE.equals(oldRefreshToken.getType())).accessToken(newToken);

        // Copy authorization_details from refresh token to new access token and to accessTokenResponse (if present)
        List<AuthorizationDetailsJSONRepresentation> authorizationDetails = oldRefreshToken.getAuthorizationDetails();
        if (authorizationDetails != null) {
            newToken.setAuthorizationDetails(authorizationDetails);
            validation.clientSessionCtx.setAttribute(AUTHORIZATION_DETAILS_RESPONSE, authorizationDetails);
        }

        if (clientConfig.isUseRefreshToken()) {
            //refresh token must have same scope as old refresh token (type, scope, expiration)
            try {
                responseBuilder.generateRefreshToken(oldRefreshToken, clientSession);
            } finally {
                if (realm.isRevokeRefreshToken()) {
                    recordTokenReuseState(session, clientSession, tokenManager.getReuseIdKey(oldRefreshToken));
                }
            }
        }

        if (newToken.getAuthorization() != null
                && clientConfig.isUseRefreshToken()) {
            responseBuilder.getRefreshToken().setAuthorization(newToken.getAuthorization());
        }

        if (TokenUtil.isOIDCRequest(oldRefreshToken.getScope())) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        storeRefreshTimingInformation(event, oldRefreshToken, newToken);

        responseBuilder.requestRefreshToken(oldRefreshToken);

        afterRefreshTokenGenerated(ctx, responseBuilder);

        if (realm.isRevokeRefreshToken()) {
            recordTokenReuseState(session, clientSession, tokenManager.getReuseIdKey(oldRefreshToken));
        }

        return responseBuilder;
    }

    /**
     * Validation specific to the particular refreshToken provider type. For example this could be validation if related user session still exists and is not expired etc.
     *
     * @return token validation with successful context information
     * @throws OAuthErrorException In case that some validation failed
     */
    protected abstract TokenManager.TokenValidation validateToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm,
                                                                  RefreshToken oldToken, HttpHeaders headers, String scope, ClientModel client,
                                                                  TokenManager tokenManager, EventBuilder event) throws OAuthErrorException;


    /**
     * Callback method invoked after refresh token is generated
     *
     * @param ctx context
     * @param responseBuilder response builder with already filled refresh token and client session context
     */
    protected abstract void afterRefreshTokenGenerated(RefreshTokenContext ctx, TokenManager.AccessTokenResponseBuilder responseBuilder);

    protected RefreshToken createRefreshToken(AccessToken accessToken, AccessToken.Confirmation confirmation, String provider) {
        RefreshToken refreshToken = new RefreshToken(accessToken, confirmation, provider);
        refreshToken.id(SecretGenerator.getInstance().generateSecureID());
        refreshToken.issuedNow();
        return refreshToken;
    }

    private Function<String, String> transformScopes(KeycloakSession session, Set<String> requestedScopes) {
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

    /**
     * Id of the lock which serializes the refreshes contending on the same rotation state. It has to be derived from
     * the same identity that the rotation state itself is keyed by, otherwise the read-modify-write of that state is
     * not atomic.
     * <p>
     * By default that is the user session plus the reuse id, as every token of a refresh token family shares the user
     * session which holds the rotation state.
     */
    protected String getRefreshTokenLockId(RealmModel realm, RefreshToken refreshToken, TokenManager tokenManager) {
        return "refreshLock:" + refreshToken.getSessionId() + ":" + tokenManager.getReuseIdKey(refreshToken);
    }

    private void createTemporaryExclusiveLockForTokenRefreshOperation(KeycloakSession session, RealmModel realm, RefreshToken refreshToken, TokenManager tokenManager) {
        String lockId = getRefreshTokenLockId(realm, refreshToken, tokenManager);
        Retry.executeWithBackoff((int iteration) -> {
            long lockAttemptStarted = System.nanoTime();
            // This assumes that 15 seconds is the maximum time this operation will take.
            // When this operation fails with a ISPN000427, it might leave a stale entry in the cache.
            // Until we have additional APIs to identify it is our own entry that is there, we wait until it expires
            // to then retry. Lifespan (15s) < Retry timeout (30s)
            if (!session.singleUseObjects().putIfAbsent(lockId, REFRESH_LOCK_LIFESPAN_SECONDS)) {
                throw new RuntimeException("Unable to acquire serialization lock for token refresh " + lockId);
            }

            // Trigger the session provider, to ensure that it enlists first for enlistAfterCompletion
            session.sessions();

            // Restore the token rotation state before the lock-release callback runs.
            getOrEnlistTokenReuseRollback(session).markLock(lockId, lockAttemptStarted);

            KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    try {
                        releaseLockIfUnexpired();
                    } catch (Exception e) {
                        // If it fails, the entry will need to expire.
                        // The call was still successful overall, so do not propagate the error.
                        logger.warn("Unable to clear entry for " + lockId, e);
                    }
                }

                @Override
                protected void rollbackImpl() {
                    try {
                        releaseLockIfUnexpired();
                    } catch (Exception e) {
                        // If it fails, the entry will need to expire.
                        // The original error will be propagated as usual.
                        logger.warn("Unable to clear entry for " + lockId, e);
                    }
                }

                private void releaseLockIfUnexpired() {
                    // After the 15-second lease, the key may belong to another refresh.
                    // Measure from before acquisition so a slow putIfAbsent is safe too.
                    if (System.nanoTime() - lockAttemptStarted < TimeUnit.SECONDS.toNanos(REFRESH_LOCK_LIFESPAN_SECONDS)) {
                        KeycloakModelUtils.runJobInTransaction(factory, s -> s.singleUseObjects().remove(lockId));
                    }
                }
            });
        // 30s allow for waiting after a ISPN000427 failure for any existing stale entry to expire after 15 seconds.
        // The remote timeout for any ISPN000427 failure is 5 seconds, so that is also within the limit.
        }, Duration.of(30, ChronoUnit.SECONDS), 10);
    }

    /**
     * Store information to identify early token refreshes of clients which stress the IAM system.
     */
    private void storeRefreshTimingInformation(EventBuilder event, RefreshToken refreshToken, AccessToken newToken) {
        long expirationAccessToken = newToken.getExp() - newToken.getIat();
        long ageOfRefreshToken = newToken.getIat() - refreshToken.getIat();
        event.detail(Details.ACCESS_TOKEN_EXPIRATION_TIME, Long.toString(expirationAccessToken));
        event.detail(Details.AGE_OF_REFRESH_TOKEN, Long.toString(ageOfRefreshToken));
    }

    protected void validateTokenReuseForRefresh(KeycloakSession session, RealmModel realm, RefreshToken refreshToken,
                                                TokenManager.TokenValidation validation, TokenManager tokenManager) throws OAuthErrorException {
        if (realm.isRevokeRefreshToken()) {
            AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
            String key = tokenManager.getReuseIdKey(refreshToken);
            try {
                captureTokenReuseState(session, clientSession, key);
                tokenManager.validateTokenReuse(session, realm, refreshToken, clientSession, true);
                incrementTokenReuseCount(session, clientSession, key);
            } catch (OAuthErrorException oee) {
                if (logger.isDebugEnabled()) {
                    logger.debugf("Failed validation of refresh token %s due it was used before. Realm: %s, client: %s, user: %s, user session: %s. Will detach client session from user session",
                            refreshToken.getId(), realm.getName(), clientSession.getClient().getClientId(), clientSession.getUserSession().getUser().getUsername(), clientSession.getUserSession().getId());
                }
                // Detach must persist even when the error response rolls back the main tx.
                KeycloakModelUtils.enlistAfterRollback(session, ctx -> {
                    AuthenticatedClientSessionModel cs = ctx.findClientSession(clientSession);
                    if (cs != null) cs.detachFromUserSession();
                });
                throw oee;
            } finally {
                recordTokenReuseState(session, clientSession, key);
            }
        }
    }

    private static TokenReuseRollback getOrEnlistTokenReuseRollback(KeycloakSession session) {
        TokenReuseRollback rollback = session.getAttribute(TOKEN_REUSE_ROLLBACK_ATTRIBUTE, TokenReuseRollback.class);
        if (rollback == null) {
            rollback = new TokenReuseRollback();
            TokenReuseRollback captured = rollback;
            KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
            var context = session.getContext();
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    if (session.getAttribute(DefaultKeycloakTransactionManager.AFTER_COMPLETION_FAILURE_ATTRIBUTE) != null) {
                        restore();
                    }
                }

                @Override
                protected void rollbackImpl() {
                    restore();
                }

                private void restore() {
                    captured.restoreWithinLocks(factory, context);
                }
            });
            session.setAttribute(TOKEN_REUSE_ROLLBACK_ATTRIBUTE, rollback);
        }
        return rollback;
    }

    static void captureTokenReuseState(KeycloakSession session,
                                       AuthenticatedClientSessionModel clientSession, String key) {
        TokenReuseRollback rollback = getOrEnlistTokenReuseRollback(session);
        rollback.capture(clientSession, key);
    }

    static void incrementTokenReuseCount(KeycloakSession session,
                                         AuthenticatedClientSessionModel clientSession, String key) {
        captureTokenReuseState(session, clientSession, key);
        int currentCount = clientSession.getRefreshTokenUseCount(key);
        clientSession.setRefreshTokenUseCount(key, currentCount + 1);
        recordTokenReuseState(session, clientSession, key);
    }

    static void recordTokenReuseState(KeycloakSession session,
                                      AuthenticatedClientSessionModel clientSession, String key) {
        TokenReuseRollback rollback = session.getAttribute(TOKEN_REUSE_ROLLBACK_ATTRIBUTE, TokenReuseRollback.class);
        if (rollback != null) {
            rollback.recordAttemptedState(clientSession, key);
        }
    }

    private static class TokenReuseRollback {

        private final Map<TokenReuseStateKey, TokenReuseState> originalStates = new LinkedHashMap<>();
        private final Map<String, Long> lockStartTimes = new LinkedHashMap<>();

        private void markLock(String lockId, long started) {
            lockStartTimes.put(lockId, started);
        }

        private void capture(AuthenticatedClientSessionModel clientSession, String key) {
            TokenReuseStateKey stateKey = new TokenReuseStateKey(clientSession.getId(), key);
            originalStates.computeIfAbsent(stateKey, ignored -> {
                Map<String, String> notes = snapshot(clientSession, key);
                return new TokenReuseState(clientSession, notes, new LinkedHashMap<>(notes));
            });
        }

        private void recordAttemptedState(AuthenticatedClientSessionModel clientSession, String key) {
            TokenReuseState state = originalStates.get(new TokenReuseStateKey(clientSession.getId(), key));
            if (state != null) {
                state.attemptedNotes().putAll(snapshot(clientSession, key));
            }
        }

        private static Map<String, String> snapshot(AuthenticatedClientSessionModel clientSession, String key) {
            Map<String, String> notes = new LinkedHashMap<>();
            for (String prefix : List.of(AuthenticatedClientSessionModel.REFRESH_TOKEN_PREFIX,
                    AuthenticatedClientSessionModel.REFRESH_TOKEN_USE_PREFIX,
                    AuthenticatedClientSessionModel.REFRESH_TOKEN_LAST_REFRESH_PREFIX,
                    AuthenticatedClientSessionModel.REFRESH_TOKEN_LATEST_GENERATED_PREFIX)) {
                String name = prefix + key;
                notes.put(name, clientSession.getNote(name));
            }
            return notes;
        }

        private void restoreWithinLocks(KeycloakSessionFactory factory, KeycloakContext context) {
            for (Map.Entry<TokenReuseStateKey, TokenReuseState> entry : originalStates.entrySet()) {
                TokenReuseState state = entry.getValue();
                String lockId = "refreshLock:" + state.clientSession().getUserSession().getId()
                        + ":" + entry.getKey().reuseKey();
                Long originalStart = lockStartTimes.get(lockId);
                long[] acquiredAt = {0L};
                if (originalStart != null
                        && System.nanoTime() - originalStart
                        >= TimeUnit.SECONDS.toNanos(DIRECT_COMPENSATION_MAX_LOCK_AGE_SECONDS)) {
                    // A database update can retry for ten seconds. If the original
                    // lease has less than eleven seconds left, wait for its expiry
                    // and reacquire before reading or restoring the four-note state.
                    Retry.executeWithBackoff((int iteration) -> {
                        long started = System.nanoTime();
                        boolean[] acquired = {false};
                        KeycloakModelUtils.runJobInTransaction(factory,
                                sub -> acquired[0] = sub.singleUseObjects().putIfAbsent(lockId,
                                        COMPENSATION_LOCK_LIFESPAN_SECONDS));
                        if (!acquired[0]) {
                            throw new IllegalStateException("Unable to reacquire refresh lock before compensation");
                        }
                        acquiredAt[0] = started;
                    }, Duration.of(30, ChronoUnit.SECONDS), 10);
                }
                try {
                    KeycloakModelUtils.runJobInTransaction(factory, context,
                            sub -> restoreState(new SessionLookup(sub), state));
                } finally {
                    if (acquiredAt[0] != 0
                            && System.nanoTime() - acquiredAt[0]
                            < TimeUnit.SECONDS.toNanos(COMPENSATION_LOCK_LIFESPAN_SECONDS)) {
                        try {
                            KeycloakModelUtils.runJobInTransaction(factory,
                                    sub -> sub.singleUseObjects().remove(lockId));
                        } catch (Exception e) {
                            logger.warn("Unable to clear compensation lock for " + lockId, e);
                        }
                    }
                }
            }
        }

        private void restoreState(SessionLookup ctx, TokenReuseState state) {
            AuthenticatedClientSessionModel rollbackClientSession = ctx.findClientSession(state.clientSession());
            if (rollbackClientSession == null) {
                return;
            }
            // A later successful refresh has a new rotation state. Compare the
            // whole group so an old callback never restores only some notes.
            boolean superseded = state.originalNotes().entrySet().stream().anyMatch(note -> {
                String current = rollbackClientSession.getNote(note.getKey());
                return !Objects.equals(current, note.getValue())
                        && !Objects.equals(current, state.attemptedNotes().get(note.getKey()));
            });
            if (superseded) {
                logger.warn("Skipping token reuse rollback because rotation state changed after capture");
                return;
            }
            for (Map.Entry<String, String> note : state.originalNotes().entrySet()) {
                if (note.getValue() == null) {
                    rollbackClientSession.removeNote(note.getKey());
                } else {
                    rollbackClientSession.setNote(note.getKey(), note.getValue());
                }
            }
        }
    }

    private record TokenReuseStateKey(String clientSessionId, String reuseKey) {}

    private record TokenReuseState(AuthenticatedClientSessionModel clientSession, Map<String, String> originalNotes,
                                   Map<String, String> attemptedNotes) {}

    @Override
    public void lockForRevocation(RefreshToken refreshToken) {
        RealmModel realm = session.getContext().getRealm();
        if (!realm.isRevokeRefreshToken()) {
            return;
        }
        createTemporaryExclusiveLockForTokenRefreshOperation(session, realm, refreshToken, new TokenManager());
    }
}
