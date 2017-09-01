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

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.HashProvider;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.WebOriginsUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;
import org.keycloak.common.util.Time;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless object that creates tokens and manages oauth access codes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenManager {
    private static final Logger logger = Logger.getLogger(TokenManager.class);
    private static final String JWT = "JWT";

    // Harcoded for now
    Algorithm jwsAlgorithm = Algorithm.RS256;

    public static void applyScope(RoleModel role, RoleModel scope, Set<RoleModel> visited, Set<RoleModel> requested) {
        if (visited.contains(scope)) return;
        visited.add(scope);
        if (role.hasRole(scope)) {
            requested.add(scope);
            return;
        }
        if (!scope.isComposite()) return;

        for (RoleModel contained : scope.getComposites()) {
            applyScope(role, contained, visited, requested);
        }
    }

    public static class TokenValidation {
        public final UserModel user;
        public final UserSessionModel userSession;
        public final AuthenticatedClientSessionModel clientSession;
        public final AccessToken newToken;

        public TokenValidation(UserModel user, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession, AccessToken newToken) {
            this.user = user;
            this.userSession = userSession;
            this.clientSession = clientSession;
            this.newToken = newToken;
        }
    }

    public TokenValidation validateToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, AccessToken oldToken, HttpHeaders headers) throws OAuthErrorException {
        UserSessionModel userSession = null;
        if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(oldToken.getType())) {

            UserSessionManager sessionManager = new UserSessionManager(session);
            userSession = sessionManager.findOfflineUserSession(realm, oldToken.getSessionState());
            if (userSession != null) {

                // Revoke timeouted offline userSession
                if (userSession.getLastSessionRefresh() < Time.currentTime() - realm.getOfflineSessionIdleTimeout()) {
                    sessionManager.revokeOfflineUserSession(userSession);
                    throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Offline session not active", "Offline session not active");
                }

            }
        } else {
            // Find userSession regularly for online tokens
            userSession = session.sessions().getUserSession(realm, oldToken.getSessionState());
            if (!AuthenticationManager.isSessionValid(realm, userSession)) {
                AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session not active", "Session not active");
            }
        }

        if (userSession == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Offline user session not found", "Offline user session not found");
        }

        UserModel user = userSession.getUser();
        if (user == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Unknown user");
        }

        if (!user.isEnabled()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "User disabled", "User disabled");
        }

        ClientModel client = session.getContext().getClient();
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(client.getId());

        if (!client.getClientId().equals(oldToken.getIssuedFor())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Unmatching clients", "Unmatching clients");
        }

        if (oldToken.getIssuedAt() < client.getNotBefore()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
        }
        if (oldToken.getIssuedAt() < realm.getNotBefore()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
        }


        // recreate token.
        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        Set<RoleModel> requestedRoles = TokenManager.getAccess(scopeParam, true, clientSession.getClient(), user);
        AccessToken newToken = createClientAccessToken(session, requestedRoles, realm, client, user, userSession, clientSession);
        verifyAccess(oldToken, newToken);

        return new TokenValidation(user, userSession, clientSession, newToken);
    }

    public boolean isTokenValid(KeycloakSession session, RealmModel realm, AccessToken token) throws OAuthErrorException {
        if (!token.isActive()) {
            return false;
        }

        if (token.getIssuedAt() < realm.getNotBefore()) {
            return false;
        }


        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null || !client.isEnabled()) {
            return false;
        }

        UserSessionModel userSession =  session.sessions().getUserSession(realm, token.getSessionState());

        UserModel user = userSession.getUser();
        if (user == null) {
            return false;
        }
        if (!user.isEnabled()) {
            return false;
        }

        if (AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(client.getId());
            if (clientSession != null) {
                return true;
            }
        }

        userSession = session.sessions().getOfflineUserSession(realm, token.getSessionState());
        if (AuthenticationManager.isOfflineSessionValid(realm, userSession)) {
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(client.getId());
            if (clientSession != null) {
                return true;
            }
        }

        return false;
    }

    public RefreshResult refreshAccessToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, ClientModel authorizedClient,
                                            String encodedRefreshToken, EventBuilder event, HttpHeaders headers) throws OAuthErrorException {
        RefreshToken refreshToken = verifyRefreshToken(session, realm, encodedRefreshToken);

        event.user(refreshToken.getSubject()).session(refreshToken.getSessionState())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, refreshToken.getType());

        TokenValidation validation = validateToken(session, uriInfo, connection, realm, refreshToken, headers);
        // validate authorizedClient is same as validated client
        if (!validation.clientSession.getClient().getId().equals(authorizedClient.getId())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token. Token client and authorized client don't match");
        }

        int currentTime = Time.currentTime();

        if (realm.isRevokeRefreshToken()) {
            int clusterStartupTime = session.getProvider(ClusterProvider.class).getClusterStartupTime();

            if (refreshToken.getIssuedAt() < validation.clientSession.getTimestamp() && (clusterStartupTime != validation.clientSession.getTimestamp())) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
            }

        }

        validation.clientSession.setTimestamp(currentTime);
        validation.userSession.setLastSessionRefresh(currentTime);

        AccessTokenResponseBuilder responseBuilder = responseBuilder(realm, authorizedClient, event, session, validation.userSession, validation.clientSession)
                .accessToken(validation.newToken)
                .generateRefreshToken();

        String scopeParam = validation.clientSession.getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken();
        }

        AccessTokenResponse res = responseBuilder.build();

        return new RefreshResult(res, TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType()));
    }

    public RefreshToken verifyRefreshToken(KeycloakSession session, RealmModel realm, String encodedRefreshToken) throws OAuthErrorException {
        return verifyRefreshToken(session, realm, encodedRefreshToken, true);
    }

    public RefreshToken verifyRefreshToken(KeycloakSession session, RealmModel realm, String encodedRefreshToken, boolean checkExpiration) throws OAuthErrorException {
        try {
            RefreshToken refreshToken = toRefreshToken(session, realm, encodedRefreshToken);

            if (checkExpiration) {
                if (refreshToken.getExpiration() != 0 && refreshToken.isExpired()) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Refresh token expired");
                }

                if (refreshToken.getIssuedAt() < realm.getNotBefore()) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale refresh token");
                }
            }

            return refreshToken;
        } catch (JWSInputException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", e);
        }
    }

    public RefreshToken toRefreshToken(KeycloakSession session, RealmModel realm, String encodedRefreshToken) throws JWSInputException, OAuthErrorException {
        JWSInput jws = new JWSInput(encodedRefreshToken);

        PublicKey publicKey;

        // Backwards compatibility. Old offline tokens didn't have KID in the header
        if (jws.getHeader().getKeyId() == null && TokenUtil.isOfflineToken(encodedRefreshToken)) {
            logger.debugf("KID is null in offline token. Using the realm active key to verify token signature.");
            publicKey = session.keys().getActiveRsaKey(realm).getPublicKey();
        } else {
            publicKey = session.keys().getRsaPublicKey(realm, jws.getHeader().getKeyId());
        }

        if (!RSAProvider.verify(jws, publicKey)) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token");
        }

        return jws.readJsonContent(RefreshToken.class);
    }

    public IDToken verifyIDToken(KeycloakSession session, RealmModel realm, String encodedIDToken) throws OAuthErrorException {
        try {
            JWSInput jws = new JWSInput(encodedIDToken);
            IDToken idToken;
            if (!RSAProvider.verify(jws, session.keys().getRsaPublicKey(realm, jws.getHeader().getKeyId()))) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken");
            }
            idToken = jws.readJsonContent(IDToken.class);

            if (idToken.isExpired()) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "IDToken expired");
            }

            if (idToken.getIssuedAt() < realm.getNotBefore()) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale IDToken");
            }
            return idToken;
        } catch (JWSInputException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken", e);
        }
    }

    public IDToken verifyIDTokenSignature(KeycloakSession session, RealmModel realm, String encodedIDToken) throws OAuthErrorException {
        try {
            JWSInput jws = new JWSInput(encodedIDToken);
            IDToken idToken;
            if (!RSAProvider.verify(jws, session.keys().getRsaPublicKey(realm, jws.getHeader().getKeyId()))) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken");
            }
            idToken = jws.readJsonContent(IDToken.class);

            return idToken;
        } catch (JWSInputException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken", e);
        }
    }

    public AccessToken createClientAccessToken(KeycloakSession session, Set<RoleModel> requestedRoles, RealmModel realm, ClientModel client, UserModel user, UserSessionModel userSession,
                                               AuthenticatedClientSessionModel clientSession) {
        AccessToken token = initToken(realm, client, user, userSession, clientSession, session.getContext().getUri());
        for (RoleModel role : requestedRoles) {
            addComposites(token, role);
        }
        token = transformAccessToken(session, token, realm, client, user, userSession, clientSession);
        return token;
    }


    public static AuthenticatedClientSessionModel attachAuthenticationSession(KeycloakSession session, UserSessionModel userSession, AuthenticationSessionModel authSession) {
        ClientModel client = authSession.getClient();

        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(client.getId());
        if (clientSession == null) {
            clientSession = session.sessions().createClientSession(userSession.getRealm(), client, userSession);
        }

        clientSession.setRedirectUri(authSession.getRedirectUri());
        clientSession.setProtocol(authSession.getProtocol());

        clientSession.setRoles(authSession.getRoles());
        clientSession.setProtocolMappers(authSession.getProtocolMappers());

        Map<String, String> transferredNotes = authSession.getClientNotes();
        for (Map.Entry<String, String> entry : transferredNotes.entrySet()) {
            clientSession.setNote(entry.getKey(), entry.getValue());
        }

        Map<String, String> transferredUserSessionNotes = authSession.getUserSessionNotes();
        for (Map.Entry<String, String> entry : transferredUserSessionNotes.entrySet()) {
            userSession.setNote(entry.getKey(), entry.getValue());
        }

        clientSession.setTimestamp(Time.currentTime());

        // Remove authentication session now
        new AuthenticationSessionManager(session).removeAuthenticationSession(userSession.getRealm(), authSession, true);

        return clientSession;
    }


    public static void dettachClientSession(UserSessionProvider sessions, RealmModel realm, AuthenticatedClientSessionModel clientSession) {
        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession == null) {
            return;
        }

        clientSession.setUserSession(null);

        if (userSession.getAuthenticatedClientSessions().isEmpty()) {
            sessions.removeUserSession(realm, userSession);
        }
    }

    public static void addGroupRoles(GroupModel group, Set<RoleModel> roleMappings) {
        roleMappings.addAll(group.getRoleMappings());
        if (group.getParentId() == null) return;
        addGroupRoles(group.getParent(), roleMappings);
    }

    public static Set<RoleModel> getAccess(String scopeParam, boolean applyScopeParam, ClientModel client, UserModel user) {
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();

        Set<RoleModel> mappings = user.getRoleMappings();
        Set<RoleModel> roleMappings = new HashSet<>();
        roleMappings.addAll(mappings);
        for (GroupModel group : user.getGroups()) {
            addGroupRoles(group, roleMappings);
        }


        ClientTemplateModel template = client.getClientTemplate();

        boolean useTemplateScope = template != null && client.useTemplateScope();

        if ( (useTemplateScope && template.isFullScopeAllowed()) || (client.isFullScopeAllowed())) {
            logger.debug("Using full scope for client");
            requestedRoles = roleMappings;
        } else {
            Set<RoleModel> scopeMappings = new HashSet<>();
            if (useTemplateScope) {
                logger.debug("Adding template scope mappings");
                scopeMappings.addAll(template.getScopeMappings());
            }
            scopeMappings.addAll(client.getRoles());
            Set<RoleModel> clientScopeMappings = client.getScopeMappings();
            scopeMappings.addAll(clientScopeMappings);
            for (RoleModel role : roleMappings) {
                for (RoleModel desiredRole : scopeMappings) {
                    Set<RoleModel> visited = new HashSet<RoleModel>();
                    applyScope(role, desiredRole, visited, requestedRoles);
                }
            }
        }
        if (applyScopeParam) {
            Collection<String> scopeParamRoles;
            if (scopeParam != null) {
                String[] scopes = scopeParam.split(" ");
                scopeParamRoles = Arrays.asList(scopes);
            } else {
                scopeParamRoles = Collections.emptyList();
            }

            Set<RoleModel> roles = new HashSet<>();
            for (RoleModel role : requestedRoles) {
                String roleName = getRoleNameForScopeParam(role);
                if (!role.isScopeParamRequired() || scopeParamRoles.contains(roleName)) {
                    roles.add(role);
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Role '%s' excluded by scope param. Client is '%s', User is '%s', Scope param is '%s' ", role.getName(), client.getClientId(), user.getUsername(), scopeParam);
                    }
                }
            }

            // Add all roles specified in scope parameter directly into requestedRoles, even if they are available just through composite role
            List<RoleModel> scopeRoles = new LinkedList<>();
            for (String scopeParamPart : scopeParamRoles) {
                RoleModel scopeParamRole = getRoleFromScopeParam(client.getRealm(), scopeParamPart);
                if (scopeParamRole != null) {
                    for (RoleModel role : roles) {
                        if (role.hasRole(scopeParamRole)) {
                            scopeRoles.add(scopeParamRole);
                        }
                    }
                }
            }

            roles.addAll(scopeRoles);
            requestedRoles = roles;
        }

        return requestedRoles;
    }

    // For now, just use "roleName" for realm roles and "clientId/roleName" for client roles
    private static String getRoleNameForScopeParam(RoleModel role) {
        if (role.getContainer() instanceof RealmModel) {
            return role.getName();
        } else {
            ClientModel client = (ClientModel) role.getContainer();
            return client.getClientId() + "/" + role.getName();
        }
    }

    // For now, just use "roleName" for realm roles and "clientId/roleName" for client roles
    private static RoleModel getRoleFromScopeParam(RealmModel realm, String scopeParamRole) {
        String[] parts = scopeParamRole.split("/");
        if (parts.length == 1) {
            return realm.getRole(parts[0]);
        } else {
            ClientModel roleClient = realm.getClientByClientId(parts[0]);
            return roleClient!=null ? roleClient.getRole(parts[1]) : null;
        }
    }

    public void verifyAccess(AccessToken token, AccessToken newToken) throws OAuthErrorException {
        if (token.getRealmAccess() != null) {
            if (newToken.getRealmAccess() == null) throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User no long has permission for realm roles");

            for (String roleName : token.getRealmAccess().getRoles()) {
                if (!newToken.getRealmAccess().getRoles().contains(roleName)) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User no long has permission for realm role: " + roleName);
                }
            }
        }
        if (token.getResourceAccess() != null) {
            for (Map.Entry<String, AccessToken.Access> entry : token.getResourceAccess().entrySet()) {
                AccessToken.Access appAccess = newToken.getResourceAccess(entry.getKey());
                if (appAccess == null && !entry.getValue().getRoles().isEmpty()) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User or client no longer has role permissions for client key: " + entry.getKey());

                }
                for (String roleName : entry.getValue().getRoles()) {
                    if (!appAccess.getRoles().contains(roleName)) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User no long has permission for client role " + roleName);
                    }
                }
            }
        }
    }

    public AccessToken transformAccessToken(KeycloakSession session, AccessToken token, RealmModel realm, ClientModel client, UserModel user,
                                            UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = ClientSessionCode.getRequestedProtocolMappers(clientSession.getProtocolMappers(), client);
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper instanceof OIDCAccessTokenMapper) {
                token = ((OIDCAccessTokenMapper) mapper).transformAccessToken(token, mapping, session, userSession, clientSession);
            }
        }

        return token;
    }

    public AccessToken transformUserInfoAccessToken(KeycloakSession session, AccessToken token, RealmModel realm, ClientModel client, UserModel user,
                                            UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = ClientSessionCode.getRequestedProtocolMappers(clientSession.getProtocolMappers(), client);
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper instanceof UserInfoTokenMapper) {
                token = ((UserInfoTokenMapper) mapper).transformUserInfoToken(token, mapping, session, userSession, clientSession);
            }
        }

        return token;
    }

    public void transformIDToken(KeycloakSession session, IDToken token, RealmModel realm, ClientModel client, UserModel user,
                                      UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = ClientSessionCode.getRequestedProtocolMappers(clientSession.getProtocolMappers(), client);
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper instanceof OIDCIDTokenMapper) {
                token = ((OIDCIDTokenMapper) mapper).transformIDToken(token, mapping, session, userSession, clientSession);
            }
        }
    }

    protected AccessToken initToken(RealmModel realm, ClientModel client, UserModel user, UserSessionModel session, AuthenticatedClientSessionModel clientSession, UriInfo uriInfo) {
        AccessToken token = new AccessToken();
        token.id(KeycloakModelUtils.generateId());
        token.type(TokenUtil.TOKEN_TYPE_BEARER);
        token.subject(user.getId());
        token.audience(client.getClientId());
        token.issuedNow();
        token.issuedFor(client.getClientId());
        token.issuer(clientSession.getNote(OIDCLoginProtocol.ISSUER));
        token.setNonce(clientSession.getNote(OIDCLoginProtocol.NONCE_PARAM));

        // Best effort for "acr" value. Use 0 if clientSession was authenticated through cookie ( SSO )
        // TODO: Add better acr support. See KEYCLOAK-3314
        String acr = (AuthenticationManager.isSSOAuthentication(clientSession)) ? "0" : "1";
        token.setAcr(acr);

        String authTime = session.getNote(AuthenticationManager.AUTH_TIME);
        if (authTime != null) {
            token.setAuthTime(Integer.parseInt(authTime));
        }


        token.setSessionState(session.getId());

        int tokenLifespan = getTokenLifespan(realm, clientSession);
        if (tokenLifespan > 0) {
            token.expiration(Time.currentTime() + tokenLifespan);
        }
        Set<String> allowedOrigins = client.getWebOrigins();
        if (allowedOrigins != null) {
            token.setAllowedOrigins(WebOriginsUtils.resolveValidWebOrigins(uriInfo, client));
        }
        return token;
    }

    private int getTokenLifespan(RealmModel realm, AuthenticatedClientSessionModel clientSession) {
        boolean implicitFlow = false;
        String responseType = clientSession.getNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        if (responseType != null) {
            implicitFlow = OIDCResponseType.parse(responseType).isImplicitFlow();
        }
        return implicitFlow ? realm.getAccessTokenLifespanForImplicitFlow() : realm.getAccessTokenLifespan();
    }

    protected void addComposites(AccessToken token, RoleModel role) {
        AccessToken.Access access = null;
        if (role.getContainer() instanceof RealmModel) {
            access = token.getRealmAccess();
            if (token.getRealmAccess() == null) {
                access = new AccessToken.Access();
                token.setRealmAccess(access);
            } else if (token.getRealmAccess().getRoles() != null && token.getRealmAccess().isUserInRole(role.getName()))
                return;

        } else {
            ClientModel app = (ClientModel) role.getContainer();
            access = token.getResourceAccess(app.getClientId());
            if (access == null) {
                access = token.addAccess(app.getClientId());
                if (app.isSurrogateAuthRequired()) access.verifyCaller(true);
            } else if (access.isUserInRole(role.getName())) return;

        }
        access.addRole(role.getName());
        if (!role.isComposite()) return;

        for (RoleModel composite : role.getComposites()) {
            addComposites(token, composite);
        }

    }

    public String encodeToken(KeycloakSession session, RealmModel realm, Object token) {
        KeyManager.ActiveRsaKey activeRsaKey = session.keys().getActiveRsaKey(realm);
        return new JWSBuilder().type(JWT).kid(activeRsaKey.getKid()).jsonContent(token).sign(jwsAlgorithm, activeRsaKey.getPrivateKey());
    }

    public AccessTokenResponseBuilder responseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        return new AccessTokenResponseBuilder(realm, client, event, session, userSession, clientSession);
    }

    public class AccessTokenResponseBuilder {
        RealmModel realm;
        ClientModel client;
        EventBuilder event;
        KeycloakSession session;
        UserSessionModel userSession;
        AuthenticatedClientSessionModel clientSession;

        AccessToken accessToken;
        RefreshToken refreshToken;
        IDToken idToken;

        boolean generateAccessTokenHash = false;
        String codeHash;

        public AccessTokenResponseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
            this.realm = realm;
            this.client = client;
            this.event = event;
            this.session = session;
            this.userSession = userSession;
            this.clientSession = clientSession;
        }

        public AccessTokenResponseBuilder accessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        public AccessTokenResponseBuilder refreshToken(RefreshToken refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AccessTokenResponseBuilder generateAccessToken() {
            UserModel user = userSession.getUser();
            String scopeParam = clientSession.getNote(OIDCLoginProtocol.SCOPE_PARAM);
            Set<RoleModel> requestedRoles = getAccess(scopeParam, true, client, user);
            accessToken = createClientAccessToken(session, requestedRoles, realm, client, user, userSession, clientSession);
            return this;
        }

        public AccessTokenResponseBuilder generateRefreshToken() {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }

            String scopeParam = clientSession.getNote(OIDCLoginProtocol.SCOPE_PARAM);
            boolean offlineTokenRequested = TokenUtil.isOfflineTokenRequested(scopeParam);
            if (offlineTokenRequested) {
                UserSessionManager sessionManager = new UserSessionManager(session);
                if (!sessionManager.isOfflineTokenAllowed(clientSession)) {
                    event.error(Errors.NOT_ALLOWED);
                    throw new ErrorResponseException("not_allowed", "Offline tokens not allowed for the user or client", Response.Status.BAD_REQUEST);
                }

                refreshToken = new RefreshToken(accessToken);
                refreshToken.type(TokenUtil.TOKEN_TYPE_OFFLINE);
                sessionManager.createOrUpdateOfflineSession(clientSession, userSession);
            } else {
                refreshToken = new RefreshToken(accessToken);
                refreshToken.expiration(Time.currentTime() + realm.getSsoSessionIdleTimeout());
            }
            refreshToken.id(KeycloakModelUtils.generateId());
            refreshToken.issuedNow();
            return this;
        }

        public AccessTokenResponseBuilder generateIDToken() {
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
            idToken.setAcr(accessToken.getAcr());
            transformIDToken(session, idToken, realm, client, userSession.getUser(), userSession, clientSession);
            return this;
        }

        public AccessTokenResponseBuilder generateAccessTokenHash() {
            generateAccessTokenHash = true;
            return this;
        }

        public AccessTokenResponseBuilder generateCodeHash(String code) {
            codeHash = HashProvider.oidcHash(jwsAlgorithm, code);
            return this;
        }


        public AccessTokenResponse build() {
            KeyManager.ActiveRsaKey activeRsaKey = session.keys().getActiveRsaKey(realm);

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
                String encodedToken = new JWSBuilder().type(JWT).kid(activeRsaKey.getKid()).jsonContent(accessToken).sign(jwsAlgorithm, activeRsaKey.getPrivateKey());
                res.setToken(encodedToken);
                res.setTokenType("bearer");
                res.setSessionState(accessToken.getSessionState());
                if (accessToken.getExpiration() != 0) {
                    res.setExpiresIn(accessToken.getExpiration() - Time.currentTime());
                }
            }

            if (generateAccessTokenHash) {
                String atHash = HashProvider.oidcHash(jwsAlgorithm, res.getToken());
                idToken.setAccessTokenHash(atHash);
            }
            if (codeHash != null) {
                idToken.setCodeHash(codeHash);
            }

            if (idToken != null) {
                String encodedToken = new JWSBuilder().type(JWT).kid(activeRsaKey.getKid()).jsonContent(idToken).sign(jwsAlgorithm, activeRsaKey.getPrivateKey());
                res.setIdToken(encodedToken);
            }
            if (refreshToken != null) {
                String encodedToken = new JWSBuilder().type(JWT).kid(activeRsaKey.getKid()).jsonContent(refreshToken).sign(jwsAlgorithm, activeRsaKey.getPrivateKey());
                res.setRefreshToken(encodedToken);
                if (refreshToken.getExpiration() != 0) {
                    res.setRefreshExpiresIn(refreshToken.getExpiration() - Time.currentTime());
                }
            }
            int notBefore = realm.getNotBefore();
            if (client.getNotBefore() > notBefore) notBefore = client.getNotBefore();
            res.setNotBeforePolicy(notBefore);
            return res;
        }
    }

    public class RefreshResult {

        private final AccessTokenResponse response;
        private final boolean offlineToken;

        private RefreshResult(AccessTokenResponse response, boolean offlineToken) {
            this.response = response;
            this.offlineToken = offlineToken;
        }

        public AccessTokenResponse getResponse() {
            return response;
        }

        public boolean isOfflineToken() {
            return offlineToken;
        }
    }

}
