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

import java.util.Collections;
import java.util.HashMap;
import org.jboss.logging.Logger;
import org.keycloak.http.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.TokenCategory;
import org.keycloak.TokenVerifier;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.HashProvider;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.migration.migrators.MigrationUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.ImpersonationSessionNote;
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
import org.keycloak.models.utils.SessionExpirationUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.TokenIntrospectionTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenResponseMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
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
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import java.util.Arrays;
import java.util.Collection;
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
    private static final String JWT = "JWT";

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
                if (!AuthenticationManager.isOfflineSessionValid(realm, userSession)) {
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

        // Can theoretically happen in cross-dc environment. Try to see if userSession with our client is available in remoteCache
        if (clientSession == null) {
            userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, userSession.getId(), offline, client.getId());
            if (userSession != null) {
                clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            } else {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session doesn't have required client", "Session doesn't have required client");
            }
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

        clientSessionCtx.setAttribute(OIDCLoginProtocol.NONCE_PARAM, oldToken.getNonce());

        // recreate token.
        AccessToken newToken = createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);

        return new TokenValidation(user, userSession, clientSessionCtx, newToken);
    }

    /**
     * Checks if the token is valid. Optionally the session last refresh and client session timestamp
     * are updated if the token was valid. This is used to keep the session alive when long lived tokens are used.
     *
     * @param session
     * @param realm
     * @param token
     * @param updateTimestamps
     * @return
     */
    public boolean checkTokenValidForIntrospection(KeycloakSession session, RealmModel realm, AccessToken token, boolean updateTimestamps) {
        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null || !client.isEnabled()) {
            return false;
        }

        try {
            TokenVerifier.createWithoutSignature(token)
                    .withChecks(NotBeforeCheck.forModel(client), TokenVerifier.IS_ACTIVE, new TokenRevocationCheck(session))
                    .verify();
        } catch (VerificationException e) {
            logger.debugf("JWT check failed: %s", e.getMessage());
            return false;
        }

        boolean valid = false;

        // Tokens without sessions are considered valid. Signature check and revocation check are sufficient checks for them
        if (token.getSessionState() == null) {
            UserModel user = lookupUserFromStatelessToken(session, realm, token);
            valid = isUserValid(session, realm, token, user);
        } else {

            UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, token.getSessionState(), false, client.getId());

            if (userSession == null) {
                // also try to resolve sessions created during token exchange when the user is impersonated
                userSession = session.sessions().getUserSessionWithPredicate(realm,
                        token.getSessionState(), false,
                        model -> client.getId().equals(model.getNote(ImpersonationSessionNote.IMPERSONATOR_CLIENT.toString())));
            }

            if (AuthenticationManager.isSessionValid(realm, userSession)) {
                valid = isUserValid(session, realm, token, userSession.getUser());
            } else {
                userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, token.getSessionState(), true, client.getId());
                if (AuthenticationManager.isOfflineSessionValid(realm, userSession)) {
                    valid = isUserValid(session, realm, token, userSession.getUser());
                }
            }

            if (valid && (token.isIssuedBeforeSessionStart(userSession.getStarted()))) {
                valid = false;
            }

            AuthenticatedClientSessionModel clientSession = userSession == null ? null : userSession.getAuthenticatedClientSessionByClient(client.getId());
            if (clientSession != null) {
                if (valid && (token.isIssuedBeforeSessionStart(clientSession.getStarted()))) {
                    valid = false;
                }
            }

            String tokenType = token.getType();
            if (realm.isRevokeRefreshToken()
                && (tokenType.equals(TokenUtil.TOKEN_TYPE_REFRESH) || tokenType.equals(TokenUtil.TOKEN_TYPE_OFFLINE))
                && !validateTokenReuseForIntrospection(session, realm, token)) {
                return false;
            }

            if (updateTimestamps && valid) {
                int currentTime = Time.currentTime();
                userSession.setLastSessionRefresh(currentTime);
                if (clientSession != null) {
                    clientSession.setTimestamp(currentTime);
                }
            }
        }

        return valid;
    }

    private boolean validateTokenReuseForIntrospection(KeycloakSession session, RealmModel realm, AccessToken token) {
        UserSessionModel userSession = null;
        if (token.getType().equals(TokenUtil.TOKEN_TYPE_REFRESH)) {
            userSession = session.sessions().getUserSession(realm, token.getSessionState());
        } else {
            UserSessionManager sessionManager = new UserSessionManager(session);
            userSession = sessionManager.findOfflineUserSession(realm, token.getSessionState());
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());

        try {
            validateTokenReuse(session, realm, token, clientSession, false);
            return true;
        } catch (OAuthErrorException e) {
            return false;
        }
    }

    private boolean isUserValid(KeycloakSession session, RealmModel realm, AccessToken token, UserModel user) {
        if (user == null) {
            return false;
        }
        if (!user.isEnabled()) {
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
        UserModel user = session.users().getUserById(realm, token.getSubject());
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

        event.user(refreshToken.getSubject()).session(refreshToken.getSessionState())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, refreshToken.getType());
        // Setup clientScopes from refresh token to the context
        String oldTokenScope = refreshToken.getScope();
        //The requested scope MUST NOT include any scope not originally granted by the resource owner
        //if scope parameter is not null, remove every scope that is not part of scope parameter
        if (scopeParameter != null && ! scopeParameter.isEmpty()) {
            Set<String> scopeParamScopes = Arrays.stream(scopeParameter.split(" ")).collect(Collectors.toSet());
            oldTokenScope = Arrays.stream(oldTokenScope.split(" ")).filter(sc -> scopeParamScopes.contains(sc))
                    .collect(Collectors.joining(" "));
        }


        TokenValidation validation = validateToken(session, uriInfo, connection, realm, refreshToken, headers, oldTokenScope);
        AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(authorizedClient);

        // validate authorizedClient is same as validated client
        if (!clientSession.getClient().getId().equals(authorizedClient.getId())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token. Token client and authorized client don't match");
        }

        validateTokenReuseForRefresh(session, realm, refreshToken, validation);

        int currentTime = Time.currentTime();
        clientSession.setTimestamp(currentTime);
        validation.userSession.setLastSessionRefresh(currentTime);

        if (refreshToken.getAuthorization() != null) {
            validation.newToken.setAuthorization(refreshToken.getAuthorization());
        }

        AccessTokenResponseBuilder responseBuilder = responseBuilder(realm, authorizedClient, event, session,
            validation.userSession, validation.clientSessionCtx).accessToken(validation.newToken);
        if (clientConfig.isUseRefreshToken()) {
            //refresh token must have same scope as old refresh token (type, scope, expiration)
            responseBuilder.generateRefreshToken(refreshToken.getScope());
        }

        if (validation.newToken.getAuthorization() != null
            && clientConfig.isUseRefreshToken()) {
            responseBuilder.getRefreshToken().setAuthorization(validation.newToken.getAuthorization());
        }

        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        return responseBuilder;
    }

    private void validateTokenReuseForRefresh(KeycloakSession session, RealmModel realm, RefreshToken refreshToken,
        TokenValidation validation) throws OAuthErrorException {
        if (realm.isRevokeRefreshToken()) {
            AuthenticatedClientSessionModel clientSession = validation.clientSessionCtx.getClientSession();
            try {
                validateTokenReuse(session, realm, refreshToken, clientSession, true);
                int currentCount = clientSession.getCurrentRefreshTokenUseCount();
                clientSession.setCurrentRefreshTokenUseCount(currentCount + 1);
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
    private void validateTokenReuse(KeycloakSession session, RealmModel realm, AccessToken refreshToken,
        AuthenticatedClientSessionModel clientSession, boolean refreshFlag) throws OAuthErrorException {
        int startupTime = session.getProvider(UserSessionProvider.class).getStartupTime(realm);

        if (clientSession.getCurrentRefreshToken() != null
            && !refreshToken.getId().equals(clientSession.getCurrentRefreshToken())
            && refreshToken.getIssuedAt() < clientSession.getTimestamp()
            && startupTime <= clientSession.getTimestamp()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
        }

        if (!refreshToken.getId().equals(clientSession.getCurrentRefreshToken())) {
            if (refreshFlag) {
                clientSession.setCurrentRefreshToken(refreshToken.getId());
                clientSession.setCurrentRefreshTokenUseCount(0);
            } else {
                return;
            }
        }

        int currentCount = clientSession.getCurrentRefreshTokenUseCount();
        if (currentCount > realm.getRefreshTokenMaxReuse()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Maximum allowed refresh token reuse exceeded",
                "Maximum allowed refresh token reuse exceeded");
        }
        return;
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
                DPoP dPoP = (DPoP) session.getAttribute(DPoPUtil.DPOP_SESSION_ATTRIBUTE);
                if (client.isPublicClient() && (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseDPoP() || dPoP != null )) {
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
        AccessToken token = initToken(realm, client, user, userSession, clientSessionCtx, session.getContext().getUri());
        token = transformAccessToken(session, token, userSession, clientSessionCtx);
        return token;
    }


    public static ClientSessionContext attachAuthenticationSession(KeycloakSession session, UserSessionModel userSession, AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        if (clientSession == null) {
            clientSession = session.sessions().createClientSession(userSession.getRealm(), client, userSession);
        }

        clientSession.setRedirectUri(authSession.getRedirectUri());
        clientSession.setProtocol(authSession.getProtocol());

        Set<String> clientScopeIds;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            clientScopeIds = AuthorizationContextUtil.getClientScopesStreamFromAuthorizationRequestContextWithClient(session, authSession.getClientNote(OAuth2Constants.SCOPE))
                    .map(ClientScopeModel::getId)
                    .collect(Collectors.toSet());
        } else {
            clientScopeIds = authSession.getClientScopes();
        }

        Map<String, String> transferredNotes = authSession.getClientNotes();
        for (Map.Entry<String, String> entry : transferredNotes.entrySet()) {
            clientSession.setNote(entry.getKey(), entry.getValue());
        }

        Map<String, String> transferredUserSessionNotes = authSession.getUserSessionNotes();
        for (Map.Entry<String, String> entry : transferredUserSessionNotes.entrySet()) {
            userSession.setNote(entry.getKey(), entry.getValue());
        }

        clientSession.setNote(Constants.LEVEL_OF_AUTHENTICATION, String.valueOf(new AcrStore(authSession).getLevelOfAuthenticationFromCurrentAuthentication()));
        clientSession.setTimestamp(userSession.getLastSessionRefresh());

        // Remove authentication session now (just current tab, not whole "rootAuthenticationSession" in case we have more browser tabs with "authentications in progress")
        new AuthenticationSessionManager(session).updateAuthenticationSessionAfterSuccessfulAuthentication(userSession.getRealm(), authSession);

        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndClientScopeIds(clientSession, clientScopeIds, session);
        return clientSessionCtx;
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
    public static Stream<ClientScopeModel> getRequestedClientScopes(String scopeParam, ClientModel client) {
        // Add all default client scopes automatically and client itself
        Stream<ClientScopeModel> clientScopes = Stream.concat(
                client.getClientScopes(true).values().stream(),
                Stream.of(client)).distinct();

        if (scopeParam == null) {
            return clientScopes;
        }

        Map<String, ClientScopeModel> allOptionalScopes = client.getClientScopes(false);
        // Add optional client scopes requested by scope parameter
        return Stream.concat(parseScopeParameter(scopeParam).map(allOptionalScopes::get).filter(Objects::nonNull),
                clientScopes).distinct();
    }

    /**
     * Check that all the ClientScopes that have been parsed into authorization_resources are actually in the requested scopes
     * otherwise, the scope wasn't parsed correctly
     * @param scopes
     * @param authorizationRequestContext
     * @param client
     * @return
     */
    public static boolean isValidScope(String scopes, AuthorizationRequestContext authorizationRequestContext, ClientModel client) {
        if (scopes == null) {
            return true;
        }
        Collection<String> requestedScopes = TokenManager.parseScopeParameter(scopes).collect(Collectors.toSet());
        Set<String> rarScopes = authorizationRequestContext.getAuthorizationDetailEntries()
                .stream()
                .map(AuthorizationDetails::getAuthorizationDetails)
                .map(AuthorizationDetailsJSONRepresentation::getScopeNameFromCustomData)
                .collect(Collectors.toSet());

        if (TokenUtil.isOIDCRequest(scopes)) {
            requestedScopes.remove(OAuth2Constants.SCOPE_OPENID);
        }

        if ((authorizationRequestContext.getAuthorizationDetailEntries() == null || authorizationRequestContext.getAuthorizationDetailEntries().isEmpty()) && requestedScopes.size()>0) {
            return false;
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Rar scopes to validate requested scopes against: %1s", String.join(" ", rarScopes));
            logger.tracef("Requested scopes: %1s", String.join(" ", requestedScopes));
        }

        for (String requestedScope : requestedScopes) {
            // We keep the check to the getDynamicClientScope for the OpenshiftSAClientAdapter
            if (!rarScopes.contains(requestedScope) && client.getDynamicClientScope(requestedScope) == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidScope(String scopes, ClientModel client) {
        if (scopes == null) {
            return true;
        }

        Set<String> clientScopes = getRequestedClientScopes(scopes, client)
                .filter(((Predicate<ClientScopeModel>) ClientModel.class::isInstance).negate())
                .map(ClientScopeModel::getName)
                .collect(Collectors.toSet());
        Collection<String> requestedScopes = TokenManager.parseScopeParameter(scopes).collect(Collectors.toSet());

        if (TokenUtil.isOIDCRequest(scopes)) {
            requestedScopes.remove(OAuth2Constants.SCOPE_OPENID);
        }

        if (!requestedScopes.isEmpty() && clientScopes.isEmpty()) {
            return false;
        }

        for (String requestedScope : requestedScopes) {
            // we also check dynamic scopes in case the client is from a provider that dynamically provides scopes to their clients
            if (!clientScopes.contains(requestedScope) && client.getDynamicClientScope(requestedScope) == null) {
                return false;
            }
        }
        
        return true;
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
        return ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx, mapper -> mapper.getValue() instanceof OIDCAccessTokenMapper)
                .collect(new TokenCollector<AccessToken>(token) {
                    @Override
                    protected AccessToken applyMapper(AccessToken token, Map.Entry<ProtocolMapperModel, ProtocolMapper> mapper) {
                        return ((OIDCAccessTokenMapper) mapper.getValue()).transformAccessToken(token, mapper.getKey(), session, userSession, clientSessionCtx);
                    }
                });
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

    protected AccessToken initToken(RealmModel realm, ClientModel client, UserModel user, UserSessionModel session,
                                    ClientSessionContext clientSessionCtx, UriInfo uriInfo) {
        AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.type(TokenUtil.TOKEN_TYPE_BEARER);
        token.subject(user.getId());
        token.issuedNow();
        token.issuedFor(client.getClientId());

        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        token.issuer(clientSession.getNote(OIDCLoginProtocol.ISSUER));
        token.setNonce(clientSessionCtx.getAttribute(OIDCLoginProtocol.NONCE_PARAM, String.class));
        token.setScope(clientSessionCtx.getScopeString());

        // Backwards compatibility behaviour prior step-up authentication was introduced
        // Protocol mapper is supposed to set this in case "step_up_authentication" feature enabled
        if (!Profile.isFeatureEnabled(Profile.Feature.STEP_UP_AUTHENTICATION)) {
            String acr = AuthenticationManager.isSSOAuthentication(clientSession) ? "0" : "1";
            token.setAcr(acr);
        }

        String authTime = session.getNote(AuthenticationManager.AUTH_TIME);
        if (authTime != null) {
            token.setAuthTime(Integer.parseInt(authTime));
        }


        token.setSessionState(session.getId());
        ClientScopeModel offlineAccessScope = KeycloakModelUtils.getClientScopeByName(realm, OAuth2Constants.OFFLINE_ACCESS);
        boolean offlineTokenRequested = offlineAccessScope == null ? false
            : clientSessionCtx.getClientScopeIds().contains(offlineAccessScope.getId());
        token.expiration(getTokenExpiration(realm, client, session, clientSession, offlineTokenRequested));

        return token;
    }

    private int getTokenExpiration(RealmModel realm, ClientModel client, UserSessionModel userSession,
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

        long sessionExpires = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(
                userSession.isOffline() || offlineTokenRequested, userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSession.getStarted()), TimeUnit.SECONDS.toMillis(userSession.getStarted()),
                realm, client);
        expiration = sessionExpires > 0? Math.min(expiration, sessionExpires) : expiration;

        return (int) TimeUnit.MILLISECONDS.toSeconds(expiration);
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

        private AccessTokenResponse response;

        public AccessTokenResponseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session,
                                          UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
            this.realm = realm;
            this.client = client;
            this.event = event;
            this.session = session;
            this.userSession = userSession;
            this.clientSessionCtx = clientSessionCtx;
            this.responseTokenType = formatTokenType(client);
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

        public AccessTokenResponseBuilder generateAccessToken() {
            UserModel user = userSession.getUser();
            accessToken = createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);
            return this;
        }

        public AccessTokenResponseBuilder generateRefreshToken() {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }

            ClientScopeModel offlineAccessScope = KeycloakModelUtils.getClientScopeByName(realm, OAuth2Constants.OFFLINE_ACCESS);
            boolean offlineTokenRequested = offlineAccessScope==null ? false : clientSessionCtx.getClientScopeIds().contains(offlineAccessScope.getId());
            generateRefreshToken(offlineTokenRequested);
            return this;
        }

        public AccessTokenResponseBuilder generateRefreshToken(String scope) {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }

            boolean offlineTokenRequested = Arrays.asList(scope.split(" ")).contains(OAuth2Constants.OFFLINE_ACCESS) ;
            generateRefreshToken(offlineTokenRequested);
            refreshToken.setScope(scope);
            return this;
        }

        private void generateRefreshToken(boolean offlineTokenRequested) {
            if (offlineTokenRequested) {
                UserSessionManager sessionManager = new UserSessionManager(session);
                if (!sessionManager.isOfflineTokenAllowed(clientSessionCtx)) {
                    event.error(Errors.NOT_ALLOWED);
                    throw new ErrorResponseException("not_allowed", "Offline tokens not allowed for the user or client", Response.Status.BAD_REQUEST);
                }

                refreshToken = new RefreshToken(accessToken);
                refreshToken.type(TokenUtil.TOKEN_TYPE_OFFLINE);
                if (realm.isOfflineSessionMaxLifespanEnabled())
                    refreshToken.expiration(getExpiration(true));
                sessionManager.createOrUpdateOfflineSession(clientSessionCtx.getClientSession(), userSession);
            } else {
                refreshToken = new RefreshToken(accessToken);
                refreshToken.expiration(getExpiration(false));
            }
            refreshToken.id(KeycloakModelUtils.generateId());
            refreshToken.issuedNow();
        }

        private int getExpiration(boolean offline) {
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

            return (int) TimeUnit.MILLISECONDS.toSeconds(expiration);
        }

        public AccessTokenResponseBuilder generateIDToken() {
            return generateIDToken(false);
        }

        public AccessTokenResponseBuilder generateIDToken(boolean isIdTokenAsDetachedSignature) {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }
            idToken = new IDToken();
            idToken.id(KeycloakModelUtils.generateId());
            idToken.type(TokenUtil.TOKEN_TYPE_ID);
            idToken.subject(accessToken.getSubject());
            idToken.audience(client.getClientId());
            idToken.issuedNow();
            idToken.issuedFor(accessToken.getIssuedFor());
            idToken.issuer(accessToken.getIssuer());
            idToken.setNonce(accessToken.getNonce());
            idToken.setAuthTime(accessToken.getAuthTime());
            idToken.setSessionState(accessToken.getSessionState());
            idToken.expiration(accessToken.getExpiration());

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
            return refreshToken != null && TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType());
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
                if (accessToken.getExpiration() != 0) {
                    res.setExpiresIn(accessToken.getExpiration() - Time.currentTime());
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
                if (refreshToken.getExpiration() != 0) {
                    res.setRefreshExpiresIn(refreshToken.getExpiration() - Time.currentTime());
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

    private String formatTokenType(ClientModel client) {
        if (OIDCAdvancedConfigWrapper.fromClientModel(client).isUseLowerCaseInTokenResponse()) {
            return TokenUtil.TOKEN_TYPE_BEARER.toLowerCase();
        }
        return TokenUtil.TOKEN_TYPE_BEARER;
    }

    public static class NotBeforeCheck implements TokenVerifier.Predicate<JsonWebToken> {

        private final int notBefore;

        public NotBeforeCheck(int notBefore) {
            this.notBefore = notBefore;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (t.getIssuedAt() < notBefore) {
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

    public LogoutTokenValidationCode verifyLogoutToken(KeycloakSession session, RealmModel realm, String encodedLogoutToken) {
        Optional<LogoutToken> logoutTokenOptional = toLogoutToken(encodedLogoutToken);
        if (!logoutTokenOptional.isPresent()) {
            return LogoutTokenValidationCode.DECODE_TOKEN_FAILED;
        }

        LogoutToken logoutToken = logoutTokenOptional.get();
        List<OIDCIdentityProvider> identityProviders = getOIDCIdentityProviders(realm, session).collect(Collectors.toList());
        if (identityProviders.isEmpty()) {
            return LogoutTokenValidationCode.COULD_NOT_FIND_IDP;
        }

        Stream<OIDCIdentityProvider> validOidcIdentityProviders =
                validateLogoutTokenAgainstIdpProvider(identityProviders.stream(), encodedLogoutToken, logoutToken);
        if (validOidcIdentityProviders.count() == 0) {
            return LogoutTokenValidationCode.TOKEN_VERIFICATION_WITH_IDP_FAILED;
        }

        if (logoutToken.getSubject() == null && logoutToken.getSid() == null) {
            return LogoutTokenValidationCode.MISSING_SID_OR_SUBJECT;
        }

        if (!checkLogoutTokenForEvents(logoutToken)) {
            return LogoutTokenValidationCode.BACKCHANNEL_LOGOUT_EVENT_MISSING;
        }

        if (logoutToken.getOtherClaims().get(NONCE) != null) {
            return LogoutTokenValidationCode.NONCE_CLAIM_IN_TOKEN;
        }

        if (logoutToken.getId() == null) {
            return LogoutTokenValidationCode.LOGOUT_TOKEN_ID_MISSING;
        }

        if (logoutToken.getIat() == null) {
            return LogoutTokenValidationCode.MISSING_IAT_CLAIM;
        }

        return LogoutTokenValidationCode.VALIDATION_SUCCESS;
    }

    public Optional<LogoutToken> toLogoutToken(String encodedLogoutToken) {
        try {
            JWSInput jws = new JWSInput(encodedLogoutToken);
            return Optional.of(jws.readJsonContent(LogoutToken.class));
        } catch (JWSInputException e) {
            return Optional.empty();
        }
    }


    public Stream<OIDCIdentityProvider> getValidOIDCIdentityProvidersForBackchannelLogout(RealmModel realm, KeycloakSession session, String encodedLogoutToken, LogoutToken logoutToken) {
        return validateLogoutTokenAgainstIdpProvider(getOIDCIdentityProviders(realm, session), encodedLogoutToken, logoutToken);
    }


    public Stream<OIDCIdentityProvider> validateLogoutTokenAgainstIdpProvider(Stream<OIDCIdentityProvider> oidcIdps, String encodedLogoutToken, LogoutToken logoutToken) {
            return oidcIdps
                    .filter(oidcIdp -> oidcIdp.getConfig().getIssuer() != null)
                    .filter(oidcIdp -> oidcIdp.isIssuer(logoutToken.getIssuer(), null))
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

    private Stream<OIDCIdentityProvider> getOIDCIdentityProviders(RealmModel realm, KeycloakSession session) {
        try {
            return realm.getIdentityProvidersStream()
                    .map(idpModel ->
                        IdentityBrokerService.getIdentityProviderFactory(session, idpModel).create(session, idpModel))
                    .filter(OIDCIdentityProvider.class::isInstance)
                    .map(OIDCIdentityProvider.class::cast);
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

}
