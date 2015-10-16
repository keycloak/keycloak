package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
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
import org.keycloak.protocol.oidc.utils.WebOriginsUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.util.TokenUtil;
import org.keycloak.common.util.Time;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateful object that creates tokens and manages oauth access codes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenManager {
    protected static final Logger logger = Logger.getLogger(TokenManager.class);

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
        public final ClientSessionModel clientSession;
        public final AccessToken newToken;

        public TokenValidation(UserModel user, UserSessionModel userSession, ClientSessionModel clientSession, AccessToken newToken) {
            this.user = user;
            this.userSession = userSession;
            this.clientSession = clientSession;
            this.newToken = newToken;
        }
    }

    public TokenValidation validateToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, AccessToken oldToken, HttpHeaders headers) throws OAuthErrorException {
        UserModel user = session.users().getUserById(oldToken.getSubject(), realm);
        if (user == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Unknown user");
        }

        if (!user.isEnabled()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "User disabled", "User disabled");
        }

        UserSessionModel userSession = null;
        ClientSessionModel clientSession = null;
        if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(oldToken.getType())) {

            UserSessionManager sessionManager = new UserSessionManager(session);
            clientSession = sessionManager.findOfflineClientSession(realm, oldToken.getClientSession(), oldToken.getSessionState());
            if (clientSession != null) {
                userSession = clientSession.getUserSession();

                // Revoke timeouted offline userSession
                if (userSession.getLastSessionRefresh() < Time.currentTime() - realm.getOfflineSessionIdleTimeout()) {
                    sessionManager.revokeOfflineUserSession(userSession);
                    userSession = null;
                    clientSession = null;
                }
            }
        } else {
            // Find userSession regularly for online tokens
            userSession = session.sessions().getUserSession(realm, oldToken.getSessionState());
            if (!AuthenticationManager.isSessionValid(realm, userSession)) {
                AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session not active", "Session not active");
            }

            for (ClientSessionModel clientSessionModel : userSession.getClientSessions()) {
                if (clientSessionModel.getId().equals(oldToken.getClientSession())) {
                    clientSession = clientSessionModel;
                    break;
                }
            }
        }

        if (clientSession == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Client session not active", "Client session not active");
        }

        ClientModel client = clientSession.getClient();

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

    public RefreshResult refreshAccessToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, ClientModel authorizedClient, String encodedRefreshToken, EventBuilder event, HttpHeaders headers) throws OAuthErrorException {
        RefreshToken refreshToken = verifyRefreshToken(realm, encodedRefreshToken);

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
            int serverStartupTime = (int)(session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000);

            if (refreshToken.getIssuedAt() < validation.clientSession.getTimestamp() && (serverStartupTime != validation.clientSession.getTimestamp())) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale token");
            }

        }

        validation.clientSession.setTimestamp(currentTime);
        validation.userSession.setLastSessionRefresh(currentTime);

        AccessTokenResponse res = responseBuilder(realm, authorizedClient, event, session, validation.userSession, validation.clientSession)
                .accessToken(validation.newToken)
                .generateIDToken()
                .generateRefreshToken()
                .build();

        return new RefreshResult(res, TokenUtil.TOKEN_TYPE_OFFLINE.equals(refreshToken.getType()));
    }

    public RefreshToken verifyRefreshToken(RealmModel realm, String encodedRefreshToken) throws OAuthErrorException {
        JWSInput jws = new JWSInput(encodedRefreshToken);
        RefreshToken refreshToken = null;
        try {
            if (!RSAProvider.verify(jws, realm.getPublicKey())) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token");
            }
            refreshToken = jws.readJsonContent(RefreshToken.class);
        } catch (Exception e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", e);
        }
        if (refreshToken.getExpiration() != 0 && refreshToken.isExpired()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Refresh token expired");
        }

        if (refreshToken.getIssuedAt() < realm.getNotBefore()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale refresh token");
        }
        return refreshToken;
    }
    public IDToken verifyIDToken(RealmModel realm, String encodedIDToken) throws OAuthErrorException {
        JWSInput jws = new JWSInput(encodedIDToken);
        IDToken idToken = null;
        try {
            if (!RSAProvider.verify(jws, realm.getPublicKey())) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken");
            }
            idToken = jws.readJsonContent(IDToken.class);
        } catch (IOException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid IDToken", e);
        }
        if (idToken.isExpired()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "IDToken expired");
        }

        if (idToken.getIssuedAt() < realm.getNotBefore()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale IDToken");
        }
        return idToken;
    }

    public AccessToken createClientAccessToken(KeycloakSession session, Set<RoleModel> requestedRoles, RealmModel realm, ClientModel client, UserModel user, UserSessionModel userSession, ClientSessionModel clientSession) {
        AccessToken token = initToken(realm, client, user, userSession, clientSession, session.getContext().getUri());
        for (RoleModel role : requestedRoles) {
            addComposites(token, role);
        }
        token = transformAccessToken(session, token, realm, client, user, userSession, clientSession);
        return token;
    }

    public static void attachClientSession(UserSessionModel session, ClientSessionModel clientSession) {
        if (clientSession.getUserSession() != null) {
            return;
        }

        UserModel user = session.getUser();
        clientSession.setUserSession(session);
        Set<String> requestedRoles = new HashSet<String>();
        // todo scope param protocol independent
        String scopeParam = clientSession.getNote(OAuth2Constants.SCOPE);
        for (RoleModel r : TokenManager.getAccess(scopeParam, true, clientSession.getClient(), user)) {
            requestedRoles.add(r.getId());
        }
        clientSession.setRoles(requestedRoles);

        Set<String> requestedProtocolMappers = new HashSet<String>();
        for (ProtocolMapperModel protocolMapper : clientSession.getClient().getProtocolMappers()) {
            if (protocolMapper.getProtocol().equals(clientSession.getAuthMethod())) {
                requestedProtocolMappers.add(protocolMapper.getId());
            }
        }
        clientSession.setProtocolMappers(requestedProtocolMappers);

        Map<String, String> transferredNotes = clientSession.getUserSessionNotes();
        for (Map.Entry<String, String> entry : transferredNotes.entrySet()) {
            session.setNote(entry.getKey(), entry.getValue());
        }

    }


    public static void dettachClientSession(UserSessionProvider sessions, RealmModel realm, ClientSessionModel clientSession) {
        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession == null) {
            return;
        }

        clientSession.setUserSession(null);
        clientSession.setRoles(null);
        clientSession.setProtocolMappers(null);

        if (userSession.getClientSessions().isEmpty()) {
            sessions.removeUserSession(realm, userSession);
        }
    }

    public static Set<RoleModel> getAccess(String scopeParam, boolean applyScopeParam, ClientModel client, UserModel user) {
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();

        Set<RoleModel> roleMappings = user.getRoleMappings();

        if (client.isFullScopeAllowed()) {
            requestedRoles = roleMappings;
        } else {

            Set<RoleModel> scopeMappings = client.getScopeMappings();
            scopeMappings.addAll(client.getRoles());

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
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = new ClientSessionCode(realm, clientSession).getRequestedProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null || !(mapper instanceof OIDCAccessTokenMapper)) continue;
            token = ((OIDCAccessTokenMapper)mapper).transformAccessToken(token, mapping, session, userSession, clientSession);

        }
        return token;
    }
    public void transformIDToken(KeycloakSession session, IDToken token, RealmModel realm, ClientModel client, UserModel user,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = new ClientSessionCode(realm, clientSession).getRequestedProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null || !(mapper instanceof OIDCIDTokenMapper)) continue;
            token = ((OIDCIDTokenMapper)mapper).transformIDToken(token, mapping, session, userSession, clientSession);

        }
    }


    protected AccessToken initToken(RealmModel realm, ClientModel client, UserModel user, UserSessionModel session, ClientSessionModel clientSession, UriInfo uriInfo) {
        AccessToken token = new AccessToken();
        if (clientSession != null) token.clientSession(clientSession.getId());
        token.id(KeycloakModelUtils.generateId());
        token.type(TokenUtil.TOKEN_TYPE_BEARER);
        token.subject(user.getId());
        token.audience(client.getClientId());
        token.issuedNow();
        token.issuedFor(client.getClientId());
        token.issuer(clientSession.getNote(OIDCLoginProtocol.ISSUER));
        token.setNonce(clientSession.getNote(OIDCLoginProtocol.NONCE_PARAM));
        if (session != null) {
            token.setSessionState(session.getId());
        }
        if (realm.getAccessTokenLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getAccessTokenLifespan());
        }
        Set<String> allowedOrigins = client.getWebOrigins();
        if (allowedOrigins != null) {
            token.setAllowedOrigins(WebOriginsUtils.resolveValidWebOrigins(uriInfo, client));
        }
        return token;
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

    public String encodeToken(RealmModel realm, Object token) {
        String encodedToken = new JWSBuilder()
                .jsonContent(token)
                .rsa256(realm.getPrivateKey());
        return encodedToken;
    }

    public AccessTokenResponseBuilder responseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        return new AccessTokenResponseBuilder(realm, client, event, session, userSession, clientSession);
    }

    public class AccessTokenResponseBuilder {
        RealmModel realm;
        ClientModel client;
        EventBuilder event;
        KeycloakSession session;
        UserSessionModel userSession;
        ClientSessionModel clientSession;

        AccessToken accessToken;
        RefreshToken refreshToken;
        IDToken idToken;

        public AccessTokenResponseBuilder(RealmModel realm, ClientModel client, EventBuilder event, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
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
            idToken.setSessionState(accessToken.getSessionState());
            if (realm.getAccessTokenLifespan() > 0) {
                idToken.expiration(Time.currentTime() + realm.getAccessTokenLifespan());
            }
            transformIDToken(session, idToken, realm, client, userSession.getUser(), userSession, clientSession);
            return this;
        }



        public AccessTokenResponse build() {
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
            if (idToken != null) {
                String encodedToken = new JWSBuilder().jsonContent(idToken).rsa256(realm.getPrivateKey());
                res.setIdToken(encodedToken);
            }
            if (accessToken != null) {
                String encodedToken = new JWSBuilder().jsonContent(accessToken).rsa256(realm.getPrivateKey());
                res.setToken(encodedToken);
                res.setTokenType("bearer");
                res.setSessionState(accessToken.getSessionState());
                if (accessToken.getExpiration() != 0) {
                    res.setExpiresIn(accessToken.getExpiration() - Time.currentTime());
                }
            }
            if (refreshToken != null) {
                String encodedToken = new JWSBuilder().jsonContent(refreshToken).rsa256(realm.getPrivateKey());
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
