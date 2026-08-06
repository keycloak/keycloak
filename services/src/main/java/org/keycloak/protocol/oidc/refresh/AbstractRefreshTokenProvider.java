package org.keycloak.protocol.oidc.refresh;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

import static org.keycloak.models.Constants.AUTHORIZATION_DETAILS_RESPONSE;

public abstract class AbstractRefreshTokenProvider implements RefreshTokenProvider {

    private static final Logger logger = Logger.getLogger(AbstractRefreshTokenProvider.class);

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
            createTemporaryExclusiveLockForTokenRefreshOperation(session, oldRefreshToken, tokenManager);
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

        final Collection<String> requestedAud = (Collection<String>) oldRefreshToken.getOtherClaims().get(Constants.REQUESTED_AUDIENCE);
        if (requestedAud != null) {
            validation.clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS,
                    requestedAud.stream()
                            .map(clientId -> session.clients().getClientByClientId(realm, clientId))
                            .filter(Objects::nonNull)
                            .toArray(ClientModel[]::new));
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
            responseBuilder.generateRefreshToken(oldRefreshToken, clientSession);
        }

        if (newToken.getAuthorization() != null
                && clientConfig.isUseRefreshToken()) {
            responseBuilder.getRefreshToken().setAuthorization(newToken.getAuthorization());
        }

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        storeRefreshTimingInformation(event, oldRefreshToken, newToken);

        responseBuilder.requestRefreshToken(oldRefreshToken);

        afterRefreshTokenGenerated(ctx, responseBuilder);

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

    private void createTemporaryExclusiveLockForTokenRefreshOperation(KeycloakSession session, RefreshToken refreshToken, TokenManager tokenManager) {
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
    private void storeRefreshTimingInformation(EventBuilder event, RefreshToken refreshToken, AccessToken newToken) {
        long expirationAccessToken = newToken.getExp() - newToken.getIat();
        long ageOfRefreshToken = newToken.getIat() - refreshToken.getIat();
        event.detail(Details.ACCESS_TOKEN_EXPIRATION_TIME, Long.toString(expirationAccessToken));
        event.detail(Details.AGE_OF_REFRESH_TOKEN, Long.toString(ageOfRefreshToken));
    }

    private void validateTokenReuseForRefresh(KeycloakSession session, RealmModel realm, RefreshToken refreshToken,
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
                // Detach must persist even when the error response rolls back the main tx.
                KeycloakModelUtils.enlistAfterRollback(session, ctx -> {
                    AuthenticatedClientSessionModel cs = ctx.findClientSession(clientSession);
                    if (cs != null) cs.detachFromUserSession();
                });
                throw oee;
            }
        }
    }
}
