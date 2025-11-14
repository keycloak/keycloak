/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenCategory;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.HashProvider;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.models.utils.SessionExpirationUtils;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationMembershipMapper;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenResponseMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserConsentManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.tracing.TracingAttributes;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.util.TokenUtil;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.ORGANIZATION;
import static org.keycloak.models.light.LightweightUserAdapter.isLightweightUser;
import static org.keycloak.representations.IDToken.NONCE;

/**
 * Stateless object that creates tokens and manages oauth access codes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenManager {
    private static final Logger logger = Logger.getLogger(TokenManager.class);

    public static class TokenValidation {
        public final UserModel user;
        public final UserSessionModel userSession;
        public final ClientSessionContext clientSessionCtx;
        public final AccessToken newToken;

        public TokenValidation(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx, AccessToken newToken) {
            this.user = user;
            this.userSession = userSession;
            this.clientSessionCtx = clientSessionCtx;
            this.newToken = newToken;
        }
    }

    public TokenValidation validateToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm,
                                         RefreshToken oldToken, HttpHeaders headers, String oldTokenScope) throws OAuthErrorException {
        UserSessionModel userSession = null;
        boolean offline = TokenUtil.TOKEN_TYPE_OFFLINE.equals(oldToken.getType());

        if (offline) {

            UserSessionManager sessionManager = new UserSessionManager(session);
            userSession = sessionManager.findOfflineUserSession(realm, oldToken.getSessionState());
            if (userSession != null) {

                // Revoke timeouted offline userSession
                if (!AuthenticationManager.isSessionValid(realm, userSession)) {
                    sessionManager.revokeOfflineUserSession(userSession);
                    throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Offline session not active", "Offline session not active");
                }

            } else {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Offline user session not found", "Offline user session not found");
            }
        } else {
            // Find userSession regularly for online tokens
            userSession = session.sessions().getUserSession(realm, oldToken.getSessionState());
            if (!AuthenticationManager.isSessionValid(realm, userSession)) {
                AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session not active", "Session not active");
            }
        }

        UserModel user = userSession.getUser();
        if (user == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Unknown user");
        }

        if (!user.isEnabled()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "User disabled", "User disabled");
        }

        if (oldToken.isIssuedBeforeSessionStart(userSession.getStarted())) {
            logger.debug("Refresh token issued before the user session started");
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Refresh token issued before the user session started");
        }


        ClientModel client = session.getContext().getClient();
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());

        if (clientSession == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session doesn't have required client", "Session doesn't have required client");
        }

        if (!AuthenticationManager.isClientSessionValid(realm, client, userSession, clientSession)) {
            logger.debug("Client session not active");
            userSession.removeAuthenticatedClientSessions(Collections.singletonList(client.getId()));
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Client session not active");
        }

        if (oldToken.isIssuedBeforeSessionStart(clientSession.getStarted())) {
            logger.debug("refresh token issued before the client session started");
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "refresh token issued before the client session started");
        }

        if (!client.getClientId().equals(oldToken.getIssuedFor())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Unmatching clients", "Unmatching clients");
        }

        try {
            TokenVerifier.createWithoutSignature(oldToken)
                    .withChecks(NotBeforeCheck.forModel(client), NotBeforeCheck.forModel(session, realm, user))
                    .verify();
        } catch (VerificationException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
        }

        if (userSession.isOffline() && !UserSessionUtil.isOfflineAccessGranted(session, clientSession)) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Offline session invalid because offline access not granted anymore");
        }

        // Case when offline token is migrated from previous version
        if (oldTokenScope == null && userSession.isOffline()) {
            logger.debugf("Migrating offline token of user '%s' for client '%s' of realm '%s'", user.getUsername(), client.getClientId(), realm.getName());
            MigrationUtils.migrateOldOfflineToken(session, realm, client, user);
            oldTokenScope = OAuth2Constants.OFFLINE_ACCESS;
        }

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, oldTokenScope, session);

        // Check user didn't revoke granted consent
        if (!verifyConsentStillAvailable(session, user, client, clientSessionCtx.getClientScopesStream())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "Client no longer has requested consent from user");
        }

        if (oldToken.getNonce() != null) {
            clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, oldToken.getNonce());
        }
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);

        // recreate token.
        AccessToken newToken = createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);

        return new TokenValidation(user, userSession, clientSessionCtx, newToken);
    }

    public static boolean isUserValid(KeycloakSession session, RealmModel realm, AccessToken token, UserModel user) {
        if (user == null) {
            logger.debugf("User does not exists");
            return false;
        }
        if (!user.isEnabled()) {
            logger.debugf("User '%s' is disabled", user.getUsername());
            return false;
        }
        try {
            TokenVerifier.createWithoutSignature(token)
                    .withChecks(NotBeforeCheck.forModel(session ,realm, user))
                    .verify();
        } catch (VerificationException e) {
            logger.debugf("JWT check failed: %s", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Lookup user from the "stateless" token. Stateless token is the token without sessionState filled (token doesn't belong to any userSession)
     */
    public static UserModel lookupUserFromStatelessToken(KeycloakSession session, RealmModel realm, AccessToken token) {
        // Try to lookup user based on "sub" claim. It should work for most cases with some rare exceptions (EG. OIDC "pairwise" subjects)
        UserModel user = token.getSubject() == null ? null : session.users().getUserById(realm, token.getSubject());
        if (user != null) {
            return user;
        }

        // Fallback to lookup user based on username (preferred_username claim)
        if (token.getPreferredUsername() != null) {
            return session.users().getUserByUsername(realm, token.getPreferredUsername());
        }

        return null;
    }


    public AccessTokenResponseBuilder refreshAccessToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, ClientModel authorizedClient,
                                            String encodedRefreshToken, EventBuilder event, HttpHeaders headers, HttpRequest request, String scopeParameter) throws OAuthErrorException {
        RefreshToken refreshToken = verifyRefreshToken(session, realm, authorizedClient, request, encodedRefreshToken, true);

        event.session(refreshToken.getSessionState())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, refreshToken.getType());

        if (refreshToken.getSubject() != null) {
            event.detail(Details.REFRESH_TOKEN_SUB, refreshToken.getSubject());
        }

        // Setup clientScopes from refresh token to the context
        String oldTokenScope = refreshToken.getScope();
        //The requested scope MUST NOT include any scope not originally granted by the resource owner
        //if scope parameter is not null, remove every scope that is not part of scope parameter
        if (scopeParameter != null && ! scopeParameter.isEmpty()) {
            Set<String> scopeParamScopes = Arrays.stream(scopeParameter.split(" ")).collect(Collectors.toSet());
            oldTokenScope = Arrays.stream(oldTokenScope.split(" "))
                    .map(transformScopes(session, scopeParamScopes))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
        }


        TokenValidation validation = validateToken(session, uriInfo, connection, realm, refreshToken, headers, oldTokenScope);
        session.getContext().setUserSession(validation.userSession);
        AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(authorizedClient);

        // validate authorizedClient is same as validated client
        if (!clientSession.getClient().getId().equals(authorizedClient.getId())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token. Token client and authorized client don't match");
        }

        validateTokenReuseForRefresh(session, realm, refreshToken, validation);

        event.user(validation.userSession.getUser());

        if (refreshToken.getAuthorization() != null) {
            validation.newToken.setAuthorization(refreshToken.getAuthorization());
        }

        final Collection<String> requestedAud = (Collection<String>) refreshToken.getOtherClaims().get(Constants.REQUESTED_AUDIENCE);
        if (requestedAud != null) {
            validation.clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS,
                    requestedAud.stream()
                            .map(clientId -> session.clients().getClientByClientId(realm, clientId))
                            .filter(Objects::nonNull)
                            .toArray(ClientModel[]::new));
        }

        AccessTokenResponseBuilder responseBuilder = responseBuilder(realm, authorizedClient, event, session,
            validation.userSession, validation.clientSessionCtx).offlineToken( TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType())).accessToken(validation.newToken);
        if (clientConfig.isUseRefreshToken()) {
            //refresh token must have same scope as old refresh token (type, scope, expiration)
            responseBuilder.generateRefreshToken(refreshToken, clientSession);
        }

        if (validation.newToken.getAuthorization() != null
            && clientConfig.isUseRefreshToken()) {
            responseBuilder.getRefreshToken().setAuthorization(validation.newToken.getAuthorization());
        }

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        storeRefreshTimingInformation(event, refreshToken, validation.newToken);

        return responseBuilder;
    }

    private Function<String, String> transformScopes(KeycloakSession session, Set<String> requestedScopes) {
        return scope -> {
            if (requestedScopes.contains(scope)) {
                return scope;
            }

            if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
                OrganizationScope oldScope = OrganizationScope.valueOfScope(session, scope);
                return oldScope == null ? null : oldScope.resolveName(session, requestedScopes, scope);
            }

            return null;
        };
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
        TokenValidation validation) throws OAuthErrorException {
        if (realm.isRevokeRefreshToken()) {
            AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
            try {
                validateTokenReuse(session, realm, refreshToken, clientSession, true);
                String key = getReuseIdKey(refreshToken);
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

    // Will throw OAuthErrorException if validation fails
    public void validateTokenReuse(KeycloakSession session, RealmModel realm, AccessToken refreshToken, AuthenticatedClientSessionModel clientSession, boolean refreshFlag) throws OAuthErrorException {
        int startupTime = session.getProvider(UserSessionProvider.class).getStartupTime(realm);
        String key = getReuseIdKey(refreshToken);
        String refreshTokenId = clientSession.getRefreshToken(key);
        int lastRefresh = clientSession.getRefreshTokenLastRefresh(key);

        //check if a more recent refresh token is already used on this tab, if yes the refresh token is invalid
        if (refreshTokenId != null && !refreshToken.getId().equals(refreshTokenId) && refreshToken.getIat() < lastRefresh && startupTime <= lastRefresh) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
        }

        if (!refreshToken.getId().equals(refreshTokenId)) {
            if (refreshFlag) {
                clientSession.setRefreshToken(key, refreshToken.getId());
                clientSession.setRefreshTokenUseCount(key, 0);
            } else {
                return;
            }
        }

        int currentCount = clientSession.getRefreshTokenUseCount(key);
        if (currentCount > realm.getRefreshTokenMaxReuse()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Maximum allowed refresh token reuse exceeded",
                "Maximum allowed refresh token reuse exceeded");
        }
    }

    public RefreshToken verifyRefreshToken(KeycloakSession session, RealmModel realm, ClientModel client, HttpRequest request, String encodedRefreshToken, boolean checkExpiration) throws OAuthErrorException {
        try {
            RefreshToken refreshToken = toRefreshToken(session, encodedRefreshToken);

            if (!(TokenUtil.TOKEN_TYPE_REFRESH.equals(refreshToken.getType()) || TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType()))) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token");
            }

            TokenVerifier<RefreshToken> tokenVerifier = TokenVerifier.createWithoutSignature(refreshToken)
                    .withChecks(new TokenVerifier.RealmUrlCheck(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName())));

            if (checkExpiration) {
                tokenVerifier.withChecks(NotBeforeCheck.forModel(realm), TokenVerifier.IS_ACTIVE);
            }

            try {
                tokenVerifier.verify();
            } catch (VerificationException e) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, e.getMessage());
            }

            if (!client.getClientId().equals(refreshToken.getIssuedFor())) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token. Token client and authorized client don't match");
            }

            // KEYCLOAK-6771 Certificate Bound Token
            if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseMtlsHokToken()) {
                if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(refreshToken, request, session)) {
                    throw new OAuthErrorException(OAuthErrorException.UNAUTHORIZED_CLIENT, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC);
                }
            }

            if (Profile.isFeatureEnabled(Profile.Feature.DPOP)) {
                if (DPoPUtil.isDPoPToken(refreshToken)) {
                    DPoP dPoP = (DPoP) session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE);
                    if (dPoP == null) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "DPoP proof is missing");
                    }
                    try {
                        DPoPUtil.validateBinding(refreshToken, dPoP);
                    } catch (VerificationException ex) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, ex.getMessage());
                    }
                }
            }

            return refreshToken;

        } catch (JWSInputException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", e);
        }
    }

    public RefreshToken toRefreshToken(KeycloakSession session, String encodedRefreshToken) throws JWSInputException, OAuthErrorException {
        RefreshToken refreshToken = session.tokens().decode(encodedRefreshToken, RefreshToken.class);
        if (refreshToken == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token");
        }
        return refreshToken;
    }

    public IDToken verifyIDToken(KeycloakSession session, RealmModel realm, String encodedIDToken) throws OAuthErrorException {
        IDToken idToken = session.tokens().decode(encodedIDToken, IDToken.class);
        try {
            TokenVerifier.createWithoutSignature(idToken)
                    .withChecks(NotBeforeCheck.forModel(realm), TokenVerifier.IS_ACTIVE)
                    .verify();
        } catch (VerificationException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, e.getMessage());
        }
        return idToken;
    }

    public IDToken verifyIDTokenSignature(KeycloakSession session, String encodedIDToken) throws OAuthErrorException {
        IDToken idToken = session.tokens().decode(encodedIDToken, IDToken.class);
        if (idToken == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken");
        }
        return idToken;
    }

    public AccessToken createClientAccessToken(KeycloakSession session, RealmModel realm, ClientModel client, UserModel user, UserSessionModel userSession,
                                               ClientSessionContext clientSessionCtx) {
        AccessToken token = initToken(session, realm, client, user, userSession, clientSessionCtx, session.getContext().getUri());
        token = transformAccessToken(session, token, userSession, clientSessionCtx);
        return token;
    }

    public static ClientSessionContext attachAuthenticationSession(KeycloakSession session, UserSessionModel userSession, AuthenticationSessionModel authSession) {
        return attachAuthenticationSession(session, userSession, authSession, null, false);
    }

    public static ClientSessionContext attachAuthenticationSession(KeycloakSession session, UserSessionModel userSession,
            AuthenticationSessionModel authSession, boolean createTransientIfMissing) {
        return attachAuthenticationSession(session, userSession, authSession, null, createTransientIfMissing);
    }

    public static ClientSessionContext attachAuthenticationSession(KeycloakSession session, UserSessionModel userSession,
            AuthenticationSessionModel authSession, Set<String> restrictedScopes, boolean createTransientIfMissing) {
        ClientModel client = authSession.getClient();

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        RealmModel realm = userSession.getRealm();
        if (clientSession != null && !AuthenticationManager.isClientSessionValid(realm, client, userSession, clientSession)) {
            // session exists but not active so re-start it
            clientSession.restartClientSession();
        } else if (clientSession == null) {
            if (createTransientIfMissing && userSession.getPersistenceState() != UserSessionModel.SessionPersistenceState.TRANSIENT) {
                // create a transient session for the missing client session
                userSession = UserSessionUtil.createTransientUserSession(session, userSession);
            }
            clientSession = session.sessions().createClientSession(realm, client, userSession);
        }

        clientSession.setRedirectUri(authSession.getRedirectUri());
        clientSession.setProtocol(authSession.getProtocol());

        String scopeParam = authSession.getClientNote(OAuth2Constants.SCOPE);
        Set<ClientScopeModel> clientScopes;

        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            session.getContext().setClient(client);
            clientScopes = AuthorizationContextUtil.getClientScopesStreamFromAuthorizationRequestContextWithClient(session, scopeParam)
                    .collect(Collectors.toSet());
        } else {
            clientScopes = getRequestedClientScopes(session, scopeParam, client, userSession.getUser())
                    .collect(Collectors.toSet());
        }

        Map<String, String> transferredNotes = authSession.getClientNotes();
        for (Map.Entry<String, String> entry : transferredNotes.entrySet()) {
            clientSession.setNote(entry.getKey(), entry.getValue());
        }

        Map<String, String> transferredUserSessionNotes = authSession.getUserSessionNotes();
        for (Map.Entry<String, String> entry : transferredUserSessionNotes.entrySet()) {
            userSession.setNote(entry.getKey(), entry.getValue());
        }

        clientSession.setNote(Constants.LEVEL_OF_AUTHENTICATION, String.valueOf(new AcrStore(session, authSession).getLevelOfAuthenticationFromCurrentAuthentication()));
        clientSession.setTimestamp(userSession.getLastSessionRefresh());
        // Remove authentication session now (just current tab, not whole "rootAuthenticationSession" in case we have more browser tabs with "authentications in progress")
        new AuthenticationSessionManager(session).updateAuthenticationSessionAfterSuccessfulAuthentication(realm, authSession);

        return DefaultClientSessionContext.fromClientSessionAndClientScopes(clientSession, clientScopes, restrictedScopes, session);
    }


    public static void dettachClientSession(AuthenticatedClientSessionModel clientSession) {
        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession == null) {
            return;
        }

        clientSession.detachFromUserSession();
    }


    public static Set<RoleModel> getAccess(UserModel user, ClientModel client, Stream<ClientScopeModel> clientScopes) {
        Set<RoleModel> roleMappings = RoleUtils.getDeepUserRoleMappings(user);

        if (client.isFullScopeAllowed()) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Using full scope for client %s", client.getClientId());
            }
            return roleMappings;
        } else {

            // 1 - Client roles of this client itself
            Stream<RoleModel> scopeMappings = client.getRolesStream();

            // 2 - Role mappings of client itself + default client scopes + optional client scopes requested by scope parameter (if applyScopeParam is true)
            Stream<RoleModel> clientScopesMappings;
            if (!logger.isTraceEnabled()) {
                clientScopesMappings = clientScopes.flatMap(clientScope -> clientScope.getScopeMappingsStream());
            } else {
                clientScopesMappings = clientScopes.flatMap(clientScope -> {
                    logger.tracef("Adding client scope role mappings of client scope '%s' to client '%s'",
                            clientScope.getName(), client.getClientId());
                    return clientScope.getScopeMappingsStream();
                });
            }
            scopeMappings = Stream.concat(scopeMappings, clientScopesMappings);

            // 3 - Expand scope mappings
            scopeMappings = RoleUtils.expandCompositeRolesStream(scopeMappings);

            // Intersection of expanded user roles and expanded scopeMappings
            roleMappings.retainAll(scopeMappings.collect(Collectors.toSet()));

            return roleMappings;
        }
    }


    /** Return client itself + all default client scopes of client + optional client scopes requested by scope parameter **/
    public static Stream<ClientScopeModel> getRequestedClientScopes(KeycloakSession session, String scopeParam, ClientModel client, UserModel user) {
        if (client == null) {
            return Stream.of();
        }

        // Add all default client scopes automatically and client itself
        Stream<ClientScopeModel> clientScopes = Stream.concat(
                client.getClientScopes(true).values().stream(),
                Stream.of(client)).distinct();

        if (scopeParam == null) {
            return clientScopes;
        }

        // skip organization-related scopes that were explicitly requested using the dynamic scope format
        // we don't want dynamic and default client scopes duplicated
        clientScopes = clientScopes.filter(scope -> {
            return scope.equals(client)
                    || !scopeParam.contains(scope.getName() + ClientScopeModel.VALUE_SEPARATOR)
                    || scope.getProtocolMapperByType(OrganizationMembershipMapper.PROVIDER_ID).isEmpty();
        });

        Map<String, ClientScopeModel> allOptionalScopes = client.getClientScopes(false);

        // Add optional client scopes requested by scope parameter
        return Stream.concat(parseScopeParameter(scopeParam)
                        .map(name -> {
                            ClientScopeModel scope = allOptionalScopes.get(name);

                            if (scope != null) {
                                return scope;
                            }

                            return tryResolveDynamicClientScope(session, scopeParam, user, name);
                        })
                        .filter(Objects::nonNull),
                clientScopes).distinct();
    }

    private static ClientScopeModel tryResolveDynamicClientScope(KeycloakSession session, String scopeParam, UserModel user, String name) {
        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            OrganizationScope orgScope = OrganizationScope.valueOfScope(session, scopeParam);

            if (orgScope == null) {
                return null;
            }

            if (user != null && orgScope.resolveOrganizations(user, scopeParam, session).findAny().isEmpty()) {
                return null;
            }

            return orgScope.toClientScope(name, user, session);
        }

        return null;
    }

    /**
     * Check that all the ClientScopes that have been parsed into authorization_resources are actually in the requested scopes
     * otherwise, the scope wasn't parsed correctly
     * @param scopes
     * @param authorizationRequestContext
     * @param client
     * @return
     */
    public static boolean isValidScope(KeycloakSession session, String scopes, AuthorizationRequestContext authorizationRequestContext, ClientModel client, UserModel user) {
        if (scopes == null) {
            return true;
        }

        Collection<String> rawScopes = TokenManager.parseScopeParameter(scopes).collect(Collectors.toSet());

        // detect multiple organization scopes
        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            if (rawScopes.stream().filter(scope -> scope.startsWith(ORGANIZATION)).count() > 1) {
                return false;
            }
        }

        if (TokenUtil.isOIDCRequest(scopes)) {
            rawScopes.remove(OAuth2Constants.SCOPE_OPENID);
        }

        if (rawScopes.isEmpty()) {
            return true;
        }

        Set<String> clientScopes;

        if (authorizationRequestContext == null) {
            // only true when dynamic scopes feature is enabled
            clientScopes = getRequestedClientScopes(session, scopes, client, user)
                    .filter(((Predicate<ClientScopeModel>) ClientModel.class::isInstance).negate())
                    .map(ClientScopeModel::getName)
                    .collect(Collectors.toSet());
        } else {
            List<AuthorizationDetails> details = Optional.ofNullable(authorizationRequestContext.getAuthorizationDetailEntries()).orElse(List.of());

            clientScopes = details
                    .stream()
                    .map(AuthorizationDetails::getAuthorizationDetails)
                    .map(AuthorizationDetailsJSONRepresentation::getScopeNameFromCustomData)
                    .collect(Collectors.toSet());
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Scopes to validate requested scopes against: %1s", String.join(" ", clientScopes));
            logger.tracef("Requested scopes: %1s", String.join(" ", rawScopes));
        }

        if (clientScopes.isEmpty()) {
            return false;
        }

        for (String requestedScope : rawScopes) {
            // we also check dynamic scopes in case the client is from a provider that dynamically provides scopes to their clients
            if (!clientScopes.contains(requestedScope) && client.getDynamicClientScope(requestedScope) == null) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidScope(KeycloakSession session, String scopes, ClientModel client, UserModel user) {
        return isValidScope(session, scopes, null, client, user);
    }

    public static Stream<String> parseScopeParameter(String scopeParam) {
        return Arrays.stream(scopeParam.split(" ")).distinct();
    }

    // Check if user still has granted consents to all requested client scopes
    public static boolean verifyConsentStillAvailable(KeycloakSession session, UserModel user, ClientModel client,
                                                      Stream<ClientScopeModel> requestedClientScopes) {
        if (!client.isConsentRequired()) {
            return true;
        }

        UserConsentModel grantedConsent = UserConsentManager.getConsentByClient(session, client.getRealm(), user, client.getId());

        return requestedClientScopes
                .filter(ClientScopeModel::isDisplayOnConsentScreen)
                .noneMatch(requestedScope -> {
                    if (grantedConsent == null || !grantedConsent.getGrantedClientScopes().contains(requestedScope)) {
                        logger.debugf("Client '%s' no longer has requested consent from user '%s' for client scope '%s'",
                                client.getClientId(), user.getUsername(), requestedScope.getName());
                        return true;
                    }
                    return false;
                });
    }

    public AccessToken transformAccessToken(KeycloakSession session, AccessToken token,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        AccessToken accessToken = ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx, mapper -> mapper.getValue() instanceof OIDCAccessTokenMapper)
                .collect(new TokenCollector<AccessToken>(token) {
                    @Override
                    protected AccessToken applyMapper(AccessToken token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper) {
                        return ((OIDCAccessTokenMapper) mapper.getValue()).transformAccessToken(token, mapper.getKey(), session, userSession, clientSessionCtx);
                    }
                });
        final ClientModel[] requestedAudienceClients = clientSessionCtx.getAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, ClientModel[].class);
        if (requestedAudienceClients != null) {
            restrictRequestedAudience(accessToken, Arrays.stream(requestedAudienceClients)
                    .map(ClientModel::getClientId)
                    .collect(Collectors.toSet()));
        }
        return accessToken;
    }

    public AccessTokenResponse transformAccessTokenResponse(KeycloakSession session, AccessTokenResponse accessTokenResponse,
            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        return ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx, mapper -> mapper.getValue() instanceof OIDCAccessTokenResponseMapper)
                .collect(new TokenCollector<AccessTokenResponse>(accessTokenResponse) {
                    @Override
                    protected AccessTokenResponse applyMapper(AccessTokenResponse token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper) {
                        return ((OIDCAccessTokenResponseMapper) mapper.getValue()).transformAccessTokenResponse(token, mapper.getKey(), session, userSession, clientSessionCtx);
                    }
                });
    }

    public AccessToken transformUserInfoAccessToken(KeycloakSession session, AccessToken token,
                                                    UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx, mapper -> mapper.getValue() instanceof UserInfoTokenMapper)
                .collect(new TokenCollector<AccessToken>(token) {
                    @Override
                    protected AccessToken applyMapper(AccessToken token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper) {
                        return ((UserInfoTokenMapper) mapper.getValue()).transformUserInfoToken(token, mapper.getKey(), session, userSession, clientSessionCtx);
                    }
                });
    }

    public AccessToken transformIntrospectionAccessToken(KeycloakSession session, AccessToken token,
                                                         UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx, mapper -> mapper.getValue() instanceof TokenIntrospectionTokenMapper)
                .collect(new TokenCollector<AccessToken>(token) {
                    @Override
                    protected AccessToken applyMapper(AccessToken token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper) {
                        return ((TokenIntrospectionTokenMapper) mapper.getValue()).transformIntrospectionToken(token, mapper.getKey(), session, userSession, clientSessionCtx);
                    }
                });
    }

    public Map<String, Object> generateUserInfoClaims(AccessToken userInfo, UserModel userModel) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userInfo.getSubject() == null? userModel.getId() : userInfo.getSubject());
        if (userInfo.getIssuer() != null) {
            claims.put("iss", userInfo.getIssuer());
        }
        if (userInfo.getAudience()!= null) {
            claims.put("aud", userInfo.getAudience());
        }
        if (userInfo.getName() != null) {
            claims.put("name", userInfo.getName());
        }
        if (userInfo.getGivenName() != null) {
            claims.put("given_name", userInfo.getGivenName());
        }
        if (userInfo.getFamilyName() != null) {
            claims.put("family_name", userInfo.getFamilyName());
        }
        if (userInfo.getMiddleName() != null) {
            claims.put("middle_name", userInfo.getMiddleName());
        }
        if (userInfo.getNickName() != null) {
            claims.put("nickname", userInfo.getNickName());
        }
        if (userInfo.getPreferredUsername() != null) {
            claims.put("preferred_username", userInfo.getPreferredUsername());
        }
        if (userInfo.getProfile() != null) {
            claims.put("profile", userInfo.getProfile());
        }
        if (userInfo.getPicture() != null) {
            claims.put("picture", userInfo.getPicture());
        }
        if (userInfo.getWebsite() != null) {
            claims.put("website", userInfo.getWebsite());
        }
        if (userInfo.getEmail() != null) {
            claims.put("email", userInfo.getEmail());
        }
        if (userInfo.getEmailVerified() != null) {
            claims.put("email_verified", userInfo.getEmailVerified());
        }
        if (userInfo.getGender() != null) {
            claims.put("gender", userInfo.getGender());
        }
        if (userInfo.getBirthdate() != null) {
            claims.put("birthdate", userInfo.getBirthdate());
        }
        if (userInfo.getZoneinfo() != null) {
            claims.put("zoneinfo", userInfo.getZoneinfo());
        }
        if (userInfo.getLocale() != null) {
            claims.put("locale", userInfo.getLocale());
        }
        if (userInfo.getPhoneNumber() != null) {
            claims.put("phone_number", userInfo.getPhoneNumber());
        }
        if (userInfo.getPhoneNumberVerified() != null) {
            claims.put("phone_number_verified", userInfo.getPhoneNumberVerified());
        }
        if (userInfo.getAddress() != null) {
            claims.put("address", userInfo.getAddress());
        }
        if (userInfo.getUpdatedAt() != null) {
            claims.put("updated_at", userInfo.getUpdatedAt());
        }
        if (userInfo.getClaimsLocales() != null) {
            claims.put("claims_locales", userInfo.getClaimsLocales());
        }
        claims.putAll(userInfo.getOtherClaims());

        if (userInfo.getRealmAccess() != null) {
            Map<String, Set<String>> realmAccess = new HashMap<>();
            realmAccess.put("roles", userInfo.getRealmAccess().getRoles());
            claims.put("realm_access", realmAccess);
        }

        if (userInfo.getResourceAccess() != null && !userInfo.getResourceAccess().isEmpty()) {
            Map<String, Map<String, Set<String>>> resourceAccessMap = new HashMap<>();

            for (Map.Entry<String, AccessToken.Access> resourceAccessMapEntry : userInfo.getResourceAccess()
                    .entrySet()) {
                Map<String, Set<String>> resourceAccess = new HashMap<>();
                resourceAccess.put("roles", resourceAccessMapEntry.getValue().getRoles());
                resourceAccessMap.put(resourceAccessMapEntry.getKey(), resourceAccess);
            }
            claims.put("resource_access", resourceAccessMap);
        }
        return claims;
    }

    private abstract static class TokenCollector<T> implements Collector<Map.Entry<ProtocolMapperModel, ProtocolMapper>, TokenCollector<T>, T> {

        private T token;

        public TokenCollector(T token) {
            this.token = token;
        }

        @Override
        public Supplier<TokenCollector<T>> supplier() {
            return () -> this;
        }

        @Override
        public Function<TokenCollector<T>, T> finisher() {
            return idTokenWrapper -> idTokenWrapper.token;
        }

        @Override
        public Set<Collector.Characteristics> characteristics() {
            return Collections.emptySet();
        }

        @Override
        public BinaryOperator<TokenCollector<T>> combiner() {
            return (tMutableWrapper, tMutableWrapper2) -> { throw new IllegalStateException("can't combine"); };
        }

        @Override
        public BiConsumer<TokenCollector<T>, Map.Entry<ProtocolMapperModel, ProtocolMapper>> accumulator() {
            return (idToken, mapper) -> idToken.token = applyMapper(idToken.token, mapper);
        }

        protected abstract T applyMapper(T token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper);

    }

    public IDToken transformIDToken(KeycloakSession session, IDToken token,
                                    UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx, mapper -> mapper.getValue() instanceof OIDCIDTokenMapper)
                .collect(new TokenCollector<IDToken>(token) {
                    protected IDToken applyMapper(IDToken token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper) {
                        return ((OIDCIDTokenMapper) mapper.getValue()).transformIDToken(token, mapper.getKey(), session, userSession, clientSessionCtx);
                    }
                });
    }

    protected AccessToken initToken(KeycloakSession session, RealmModel realm, ClientModel client, UserModel user, UserSessionModel userSession,
                                    ClientSessionContext clientSessionCtx, UriInfo uriInfo) {
        AccessToken token = new AccessToken();

        TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);
        AccessTokenContext tokenCtx = encoder.getTokenContextFromClientSessionContext(clientSessionCtx, SecretGenerator.getInstance().generateSecureID());
        token.id(encoder.encodeTokenId(tokenCtx));

        token.type(formatTokenType(client, token));
        if (UserSessionModel.SessionPersistenceState.TRANSIENT.equals(userSession.getPersistenceState())) {
            token.subject(user.getId());
        }
        token.issuedNow();
        token.issuedFor(client.getClientId());

        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        token.issuer(clientSession.getNote(OIDCLoginProtocol.ISSUER));
        token.setScope(clientSessionCtx.getScopeString());

        // Backwards compatibility behaviour prior step-up authentication was introduced
        // Protocol mapper is supposed to set this in case "step_up_authentication" feature enabled
        if (!Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION)) {
            String acr = AuthenticationManager.isSSOAuthentication(clientSession) ? "0" : "1";
            token.setAcr(acr);
        }

        token.setSessionId(userSession.getId());
        ClientScopeModel offlineAccessScope = KeycloakModelUtils.getClientScopeByName(realm, OAuth2Constants.OFFLINE_ACCESS);
        boolean offlineTokenRequested = offlineAccessScope == null ? false
                : clientSessionCtx.getClientScopeIds().contains(offlineAccessScope.getId());
        token.exp(getTokenExpiration(realm, client, userSession, clientSession, offlineTokenRequested));

        // Tracing
        var tracing = session.getProvider(TracingProvider.class);
        var span = tracing.getCurrentSpan();
        if (span.isRecording()) {
            span.setAttribute(TracingAttributes.TOKEN_ISSUER, token.getIssuer());
            span.setAttribute(TracingAttributes.TOKEN_SID, token.getSessionId());
            span.setAttribute(TracingAttributes.TOKEN_ID, token.getId());
        }

        return token;
    }

    private Long getTokenExpiration(RealmModel realm, ClientModel client, UserSessionModel userSession,
        AuthenticatedClientSessionModel clientSession, boolean offlineTokenRequested) {
        boolean implicitFlow = false;
        String responseType = clientSession.getNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (responseType != null) {
            implicitFlow = OIDCResponseType.parse(responseType).isImplicitFlow();
        }

        int tokenLifespan;

        if (implicitFlow) {
            tokenLifespan = realm.getAccessTokenLifespanForImplicitFlow();
        } else {
            String clientLifespan = client.getAttribute(OIDCConfigAttributes.ACCESS_TOKEN_LIFESPAN);
            if (clientLifespan != null && !clientLifespan.trim().isEmpty()) {
                tokenLifespan = Integer.parseInt(clientLifespan);
            } else {
                tokenLifespan = realm.getAccessTokenLifespan();
            }
        }

        long expiration;
        if (tokenLifespan == -1) {
            expiration = TimeUnit.SECONDS.toMillis(userSession.getStarted() +
                    (userSession.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0
                            ? realm.getSsoSessionMaxLifespanRememberMe()
                            : realm.getSsoSessionMaxLifespan()));
        } else {
            expiration = Time.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tokenLifespan);
        }

        final boolean offline = userSession.isOffline() || offlineTokenRequested ||
                (userSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT &&
                Constants.CREATED_FROM_PERSISTENT_OFFLINE.equals(userSession.getNote(Constants.CREATED_FROM_PERSISTENT)));
        long sessionExpires = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(
                offline, userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSession.getStarted()), TimeUnit.SECONDS.toMillis(userSession.getStarted()),
                realm, client);
        expiration = sessionExpires > 0? Math.min(expiration, sessionExpires) : expiration;

        return TimeUnit.MILLISECONDS.toSeconds(expiration);
    }


    public AccessTokenResponseBuilder responseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session,
                                                      UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return new AccessTokenResponseBuilder(realm, client, event, session, userSession, clientSessionCtx);
    }

    public class AccessTokenResponseBuilder {
        RealmModel realm;
        ClientModel client;
        EventBuilder event;
        KeycloakSession session;
        UserSessionModel userSession;
        ClientSessionContext clientSessionCtx;

        AccessToken accessToken;
        RefreshToken refreshToken;
        IDToken idToken;
        String responseTokenType;

        boolean generateAccessTokenHash = false;
        String codeHash;

        String stateHash;
        boolean offlineToken = false;

        private AccessTokenResponse response;

        public AccessTokenResponseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session,
                                          UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
            this.realm = realm;
            this.client = client;
            this.event = event;
            this.session = session;
            this.userSession = userSession;
            this.clientSessionCtx = clientSessionCtx;
            this.responseTokenType = formatTokenType(client, null);
        }

        public AccessToken getAccessToken() {
            return accessToken;
        }

        public RefreshToken getRefreshToken() {
            return refreshToken;
        }

        public IDToken getIdToken() {
            return idToken;
        }

        public AccessTokenResponseBuilder accessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
            this.responseTokenType = formatTokenType(client, accessToken);
            return this;
        }

        public AccessTokenResponseBuilder refreshToken(RefreshToken refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AccessTokenResponseBuilder responseTokenType(String responseTokenType) {
            this.responseTokenType = responseTokenType;
            return this;
        }

         public AccessTokenResponseBuilder offlineToken(boolean offlineToken) {
            this.offlineToken = offlineToken;
            return this;
        }

        public AccessTokenResponseBuilder generateAccessToken() {
            UserModel user = userSession.getUser();
            accessToken = createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);
            responseTokenType = formatTokenType(client, accessToken);
            return this;
        }

        public AccessTokenResponseBuilder generateRefreshToken() {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }

            boolean offlineTokenRequested = clientSessionCtx.isOfflineTokenRequested();
            generateRefreshToken(offlineTokenRequested);
            refreshToken.setScope(clientSessionCtx.getScopeString(true));
            if (realm.isRevokeRefreshToken()) {
                refreshToken.getOtherClaims().put(Constants.REUSE_ID, KeycloakModelUtils.generateId());
            }
            return this;
        }

        public AccessTokenResponseBuilder generateRefreshToken(RefreshToken oldRefreshToken, AuthenticatedClientSessionModel clientSession) {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }

            String scope = oldRefreshToken.getScope();
            Object reuseId = oldRefreshToken.getOtherClaims().get(Constants.REUSE_ID);
            boolean offlineTokenRequested = Arrays.asList(scope.split(" ")).contains(OAuth2Constants.OFFLINE_ACCESS) ;
            if (offlineTokenRequested) {
                clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, scope, session);
                if (oldRefreshToken.getNonce() != null) {
                    clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, oldRefreshToken.getNonce());
                }
            }
            generateRefreshToken(offlineTokenRequested);
            if (realm.isRevokeRefreshToken()) {
                refreshToken.getOtherClaims().put(Constants.REUSE_ID, reuseId);
                clientSession.setRefreshTokenLastRefresh(getReuseIdKey(oldRefreshToken), refreshToken.getIat().intValue());
            }
            refreshToken.setScope(scope);
            return this;
        }

        private void generateRefreshToken(boolean offlineTokenRequested) {
            AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
            AccessToken.Confirmation confirmation = getConfirmation(clientSession, accessToken);
            refreshToken = new RefreshToken(accessToken, confirmation);
            refreshToken.id(SecretGenerator.getInstance().generateSecureID());
            refreshToken.issuedNow();
            clientSession.setTimestamp(refreshToken.getIat().intValue());
            UserSessionModel userSession = clientSession.getUserSession();
            userSession.setLastSessionRefresh(refreshToken.getIat().intValue());
            if (offlineTokenRequested) {
                refreshToken.type(TokenUtil.TOKEN_TYPE_OFFLINE);
                if (realm.isOfflineSessionMaxLifespanEnabled()) {
                    refreshToken.exp(getExpiration(true));
                }
                createOrUpdateOfflineSession();
            } else {
                refreshToken.exp(getExpiration(false));
            }
            final ClientModel[] resquestedAudienceClients = clientSessionCtx.getAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, ClientModel[].class);
            if (resquestedAudienceClients != null) {
                refreshToken.getOtherClaims().put(Constants.REQUESTED_AUDIENCE, Arrays.stream(resquestedAudienceClients)
                        .map(ClientModel::getClientId)
                        .collect(Collectors.toSet()));
            }
            Boolean bindOnlyRefreshToken = session.getAttributeOrDefault(DPoPUtil.DPOP_BINDING_ONLY_REFRESH_TOKEN_SESSION_ATTRIBUTE, false);
            if (bindOnlyRefreshToken) {
                DPoP dPoP = session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE, DPoP.class);
                if (dPoP != null) {
                    confirmation = new AccessToken.Confirmation();
                    confirmation.setKeyThumbprint(dPoP.getThumbprint());
                    refreshToken.setConfirmation(confirmation);
                }
            }
        }

        public void createOrUpdateOfflineSession() {
            UserSessionManager sessionManager = new UserSessionManager(session);
            if (!sessionManager.isOfflineTokenAllowed(clientSessionCtx)) {
                event.detail(Details.REASON, "Offline tokens not allowed for the user or client");
                event.error(Errors.NOT_ALLOWED);
                throw new ErrorResponseException(Errors.NOT_ALLOWED, "Offline tokens not allowed for the user or client", Response.Status.BAD_REQUEST);
            }
            sessionManager.createOrUpdateOfflineSession(clientSessionCtx.getClientSession(), userSession);
        }

       /**
        * RFC9449 chapter 5<br/>
        * Refresh tokens issued to confidential clients are not bound to the DPoP proof public key because
        * they are already sender-constrained with a different existing mechanism.<br/>
        * <br/>
        * Based on the definition above the confirmation is only returned for public-clients.
        */
        private AccessToken.Confirmation getConfirmation(AuthenticatedClientSessionModel clientSession,
                                                         AccessToken accessToken) {
            final boolean isPublicClient = clientSession.getClient().isPublicClient();
            return isPublicClient ? accessToken.getConfirmation() : null;
        }

        private Long getExpiration(boolean offline) {
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

        public AccessTokenResponseBuilder generateIDToken() {
            return generateIDToken(false);
        }

        public AccessTokenResponseBuilder generateIDToken(boolean isIdTokenAsDetachedSignature) {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }
            idToken = new IDToken();
            idToken.id(SecretGenerator.getInstance().generateSecureID());
            idToken.type(TokenUtil.TOKEN_TYPE_ID);
            idToken.subject(userSession.getUser().getId());
            idToken.audience(client.getClientId());
            idToken.issuedNow();
            idToken.issuedFor(accessToken.getIssuedFor());
            idToken.issuer(accessToken.getIssuer());
            idToken.setNonce(clientSessionCtx.getAttribute(OIDCLoginProtocol.NONCE_PARAM, String.class));
            idToken.setSessionId(accessToken.getSessionId());
            idToken.exp(accessToken.getExp());

            // Protocol mapper is supposed to set this in case "step_up_authentication" feature enabled
            if (!Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION)) {
                idToken.setAcr(accessToken.getAcr());
            }

            if (isIdTokenAsDetachedSignature == false) {
                idToken = transformIDToken(session, idToken, userSession, clientSessionCtx);
            }
            return this;
        }

        public AccessTokenResponseBuilder generateAccessTokenHash() {
            generateAccessTokenHash = true;
            return this;
        }

        public AccessTokenResponseBuilder generateCodeHash(String code) {
            codeHash = generateOIDCHash(code);
            return this;
        }

        // Financial API - Part 2: Read and Write API Security Profile
        // http://openid.net/specs/openid-financial-api-part-2.html#authorization-server
        public AccessTokenResponseBuilder generateStateHash(String state) {
            stateHash = generateOIDCHash(state);
            return this;
        }

        public boolean isOfflineToken() {
            return offlineToken;
        }

        public AccessTokenResponse build() {
            if (response != null) return response;

            if (accessToken != null) {
                event.detail(Details.TOKEN_ID, accessToken.getId());
            }

            if (refreshToken != null) {
                if (event.getEvent().getDetails().containsKey(Details.REFRESH_TOKEN_ID)) {
                    event.detail(Details.UPDATED_REFRESH_TOKEN_ID, refreshToken.getId());
                } else {
                    event.detail(Details.REFRESH_TOKEN_ID, refreshToken.getId());
                }
                event.detail(Details.REFRESH_TOKEN_TYPE, refreshToken.getType());
            }

            AccessTokenResponse res = new AccessTokenResponse();

            if (accessToken != null) {
                String encodedToken = session.tokens().encode(accessToken);
                res.setToken(encodedToken);
                res.setTokenType(responseTokenType);
                res.setSessionState(accessToken.getSessionState());
                if (accessToken.getExp() != 0) {
                    res.setExpiresIn(accessToken.getExp() - Time.currentTime());
                }
            }

            if (generateAccessTokenHash) {
                String atHash = generateOIDCHash(res.getToken());
                idToken.setAccessTokenHash(atHash);
            }
            if (codeHash != null) {
                idToken.setCodeHash(codeHash);
            }
            // Financial API - Part 2: Read and Write API Security Profile
            // http://openid.net/specs/openid-financial-api-part-2.html#authorization-server
            if (stateHash != null) {
                idToken.setStateHash(stateHash);
            }
            if (idToken != null) {
                String encodedToken = session.tokens().encodeAndEncrypt(idToken);
                res.setIdToken(encodedToken);
            }
            if (refreshToken != null) {
                String encodedToken = session.tokens().encode(refreshToken);
                res.setRefreshToken(encodedToken);
                Long exp = refreshToken.getExp();
                if (exp != null && exp > 0) {
                    res.setRefreshExpiresIn(exp - Time.currentTime());
                }
            }

            int notBefore = realm.getNotBefore();
            if (client.getNotBefore() > notBefore) notBefore = client.getNotBefore();
            final UserModel user = userSession.getUser();
            if (! isLightweightUser(user)) {
                int userNotBefore = session.users().getNotBeforeOfUser(realm, user);
                if (userNotBefore > notBefore) notBefore = userNotBefore;
            }
            res.setNotBeforePolicy(notBefore);

            res = transformAccessTokenResponse(session, res, userSession, clientSessionCtx);

            // OIDC Financial API Read Only Profile : scope MUST be returned in the response from Token Endpoint
            String responseScope = clientSessionCtx.getScopeString();
            res.setScope(responseScope);
            event.detail(Details.SCOPE, responseScope);

            response = res;
            return response;
        }


        private String generateOIDCHash(String input) {
            String signatureAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.ID);
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
            String hashAlgorithm = signatureProvider.signer().getHashAlgorithm();

            HashProvider hashProvider = session.getProvider(HashProvider.class, hashAlgorithm);
            byte[] hash = hashProvider.hash(input);

            return HashUtils.encodeHashToOIDC(hash);
        }

    }

    private String formatTokenType(ClientModel client, AccessToken accessToken) {
        final String tokenType = Optional.ofNullable(accessToken).map(AccessToken::getType)
                                                                 .orElse(TokenUtil.TOKEN_TYPE_BEARER);
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseLowerCaseInTokenResponse()) {
            return tokenType.toLowerCase();
        }
        return tokenType;
    }

    private AccessToken restrictRequestedAudience(AccessToken accessToken, Set<String> audience) {
        if (accessToken.getAudience() != null) {
            final Set<String> audienceToSet = new HashSet<>(audience);
            audienceToSet.retainAll(Set.of(accessToken.getAudience()));
            accessToken.audience(audienceToSet.toArray(String[]::new));
            accessToken.getResourceAccess().keySet().removeIf(clientId -> !audienceToSet.contains(clientId));
        }
        return accessToken;
    }

    public static class NotBeforeCheck implements TokenVerifier.Predicate<JsonWebToken> {

        private final int notBefore;

        public NotBeforeCheck(int notBefore) {
            this.notBefore = notBefore;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (t.getIat() < notBefore) {
                throw new VerificationException("Stale token");
            }

            return true;
        }

        public static NotBeforeCheck forModel(ClientModel clientModel) {
            if (clientModel != null) {

                int notBeforeClient = clientModel.getNotBefore();
                int notBeforeRealm = clientModel.getRealm().getNotBefore();

                int notBefore = (notBeforeClient == 0 ? notBeforeRealm : (notBeforeRealm == 0 ? notBeforeClient :
                        Math.min(notBeforeClient, notBeforeRealm)));

                return new NotBeforeCheck(notBefore);
            }

            return new NotBeforeCheck(0);
        }

        public static NotBeforeCheck forModel(RealmModel realmModel) {
            return new NotBeforeCheck(realmModel == null ? 0 : realmModel.getNotBefore());
        }

        public static NotBeforeCheck forModel(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
            return isLightweightUser(userModel)
              ? new NotBeforeCheck((int) (((LightweightUserAdapter) userModel).getCreatedTimestamp() / 1000L))
              : new NotBeforeCheck(session.users().getNotBeforeOfUser(realmModel, userModel));
        }
    }

    /**
     * Check if access token was revoked with OAuth revocation endpoint
     */
    public static class TokenRevocationCheck implements TokenVerifier.Predicate<JsonWebToken> {

        private final KeycloakSession session;

        public TokenRevocationCheck(KeycloakSession session) {
            this.session = session;
        }

        @Override
        public boolean test(JsonWebToken token) {
            SingleUseObjectProvider singleUseStore = session.singleUseObjects();
            return !singleUseStore.contains(token.getId() + SingleUseObjectProvider.REVOKED_KEY);
        }
    }

    public LogoutTokenValidationContext verifyLogoutToken(KeycloakSession session, String encodedLogoutToken) {
        Optional<LogoutToken> logoutTokenOptional = toLogoutToken(encodedLogoutToken);
        if (logoutTokenOptional.isEmpty()) {
            return LogoutTokenValidationCode.DECODE_TOKEN_FAILED.toCtx();
        }

        LogoutToken logoutToken = logoutTokenOptional.get();
        List<OIDCIdentityProvider> identityProviders = getOIDCIdentityProviders(logoutToken, session).toList();
        if (identityProviders.isEmpty()) {
            return LogoutTokenValidationCode.COULD_NOT_FIND_IDP.toCtx();
        }

        List<OIDCIdentityProvider> validOidcIdentityProviders =
                validateLogoutTokenAgainstIdpProvider(identityProviders.stream(), encodedLogoutToken).toList();
        if (validOidcIdentityProviders.isEmpty()) {
            return LogoutTokenValidationCode.TOKEN_VERIFICATION_WITH_IDP_FAILED.toCtx();
        }

        if (logoutToken.getSubject() == null && logoutToken.getSid() == null) {
            return LogoutTokenValidationCode.MISSING_SID_OR_SUBJECT.toCtx();
        }

        if (!checkLogoutTokenForEvents(logoutToken)) {
            return LogoutTokenValidationCode.BACKCHANNEL_LOGOUT_EVENT_MISSING.toCtx();
        }

        if (logoutToken.getOtherClaims().get(NONCE) != null) {
            return LogoutTokenValidationCode.NONCE_CLAIM_IN_TOKEN.toCtx();
        }

        if (logoutToken.getId() == null) {
            return LogoutTokenValidationCode.LOGOUT_TOKEN_ID_MISSING.toCtx();
        }

        if (logoutToken.getIat() == null) {
            return LogoutTokenValidationCode.MISSING_IAT_CLAIM.toCtx();
        }

        return new LogoutTokenValidationContext(LogoutTokenValidationCode.VALIDATION_SUCCESS, logoutToken, validOidcIdentityProviders);
    }

    public Optional<LogoutToken> toLogoutToken(String encodedLogoutToken) {
        try {
            JWSInput jws = new JWSInput(encodedLogoutToken);
            return Optional.of(jws.readJsonContent(LogoutToken.class));
        } catch (JWSInputException e) {
            return Optional.empty();
        }
    }


    public Stream<OIDCIdentityProvider> validateLogoutTokenAgainstIdpProvider(Stream<OIDCIdentityProvider> oidcIdps, String encodedLogoutToken) {
            return oidcIdps
                    .filter(oidcIdp -> {
                        try {
                            oidcIdp.validateToken(encodedLogoutToken);
                            return true;
                        } catch (IdentityBrokerException e) {
                            logger.debugf("LogoutToken verification with identity provider failed", e.getMessage());
                            return false;
                        }
                    });
    }

    private Stream<OIDCIdentityProvider> getOIDCIdentityProviders(LogoutToken logoutToken, KeycloakSession session) {
        try {
            return session.identityProviders()
                    .getAllStream(IdentityProviderQuery.userAuthentication()
                            .with(OIDCIdentityProviderConfig.ISSUER, logoutToken.getIssuer()
                    ), -1, -1)
                    .map(model -> {
                        var idp = IdentityBrokerService.getIdentityProvider(session, model.getAlias());

                        if (idp instanceof OIDCIdentityProvider oidcIdp) {
                            return oidcIdp;
                        }

                        return null;
                    })
                    .filter(Objects::nonNull);
        } catch (IdentityBrokerException e) {
            logger.warnf("LogoutToken verification with identity provider failed", e.getMessage());
        }
        return Stream.empty();
    }

    private boolean checkLogoutTokenForEvents(LogoutToken logoutToken) {
        for (String eventKey : logoutToken.getEvents().keySet()) {
            if (TokenUtil.TOKEN_BACKCHANNEL_LOGOUT_EVENT.equals(eventKey)) {
                return true;
            }
        }
        return false;
    }

    private String getReuseIdKey(AccessToken refreshToken) {
        return Optional.ofNullable(refreshToken.getOtherClaims().get(Constants.REUSE_ID)).map(String::valueOf).orElse("");
    }

}
