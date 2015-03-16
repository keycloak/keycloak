package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.keycloak.ClientConnection;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.ApplicationModel;
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
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.Time;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashSet;
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

        UserSessionModel userSession = session.sessions().getUserSession(realm, oldToken.getSessionState());
        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticationManager.logout(session, realm, userSession, uriInfo, connection, headers);
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session not active", "Session not active");
        }
        ClientSessionModel clientSession = null;
        for (ClientSessionModel clientSessionModel : userSession.getClientSessions()) {
            if (clientSessionModel.getId().equals(oldToken.getClientSession())) {
                clientSession = clientSessionModel;
                break;
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
        Set<RoleModel> requestedRoles = TokenManager.getAccess(null, clientSession.getClient(), user);
        AccessToken newToken = createClientAccessToken(session, requestedRoles, realm, client, user, userSession, clientSession);
        verifyAccess(oldToken, newToken);

        return new TokenValidation(user, userSession, clientSession, newToken);


    }

    public AccessTokenResponse refreshAccessToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, ClientModel authorizedClient, String encodedRefreshToken, EventBuilder event, HttpHeaders headers) throws OAuthErrorException {
        RefreshToken refreshToken = verifyRefreshToken(realm, encodedRefreshToken);

        event.user(refreshToken.getSubject()).session(refreshToken.getSessionState()).detail(Details.REFRESH_TOKEN_ID, refreshToken.getId());

        TokenValidation validation = validateToken(session, uriInfo, connection, realm, refreshToken, headers);
        // validate authorizedClient is same as validated client
        if (!validation.clientSession.getClient().getId().equals(authorizedClient.getId())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token. Token client and authorized client don't match");
        }

        int currentTime = Time.currentTime();
        validation.userSession.setLastSessionRefresh(currentTime);

        AccessTokenResponse res = responseBuilder(realm, authorizedClient, event, session, validation.userSession, validation.clientSession)
                .accessToken(validation.newToken)
                .generateIDToken()
                .generateRefreshToken().build();
        return res;
    }

    public RefreshToken verifyRefreshToken(RealmModel realm, String encodedRefreshToken) throws OAuthErrorException {
        JWSInput jws = new JWSInput(encodedRefreshToken);
        RefreshToken refreshToken = null;
        try {
            if (!RSAProvider.verify(jws, realm.getPublicKey())) {
                throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token");
            }
            refreshToken = jws.readJsonContent(RefreshToken.class);
        } catch (IOException e) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", e);
        }
        if (refreshToken.isExpired()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Refresh token expired");
        }

        if (refreshToken.getIssuedAt() < realm.getNotBefore()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale refresh token");
        }
        return refreshToken;
    }

    public AccessToken createClientAccessToken(KeycloakSession session, Set<RoleModel> requestedRoles, RealmModel realm, ClientModel client, UserModel user, UserSessionModel userSession, ClientSessionModel clientSession) {
        AccessToken token = initToken(realm, client, user, userSession, clientSession);
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
        for (RoleModel r : TokenManager.getAccess(null, clientSession.getClient(), user)) {
            requestedRoles.add(r.getId());
        }
        clientSession.setRoles(requestedRoles);
    }

    public static void dettachClientSession(UserSessionProvider sessions, RealmModel realm, ClientSessionModel clientSession) {
        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession == null) {
            return;
        }

        clientSession.setUserSession(null);
        clientSession.setRoles(null);

        if (userSession.getClientSessions().isEmpty()) {
            sessions.removeUserSession(realm, userSession);
        }
    }

    public static Set<RoleModel> getAccess(String scopeParam, ClientModel client, UserModel user) {
        // todo scopeParam is ignored until we figure out a scheme that fits with openid connect
        Set<RoleModel> requestedRoles = new HashSet<RoleModel>();

        Set<RoleModel> roleMappings = user.getRoleMappings();
        if (client.isFullScopeAllowed()) return roleMappings;

        Set<RoleModel> scopeMappings = client.getScopeMappings();
        if (client instanceof ApplicationModel) {
            scopeMappings.addAll(((ApplicationModel) client).getRoles());
        }

        for (RoleModel role : roleMappings) {
            for (RoleModel desiredRole : scopeMappings) {
                Set<RoleModel> visited = new HashSet<RoleModel>();
                applyScope(role, desiredRole, visited, requestedRoles);
            }
        }

        return requestedRoles;
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
                    throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User or application no longer has role permissions for application key: " + entry.getKey());

                }
                for (String roleName : entry.getValue().getRoles()) {
                    if (!appAccess.getRoles().contains(roleName)) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User no long has permission for application role " + roleName);
                    }
                }
            }
        }
    }

    public AccessToken transformAccessToken(KeycloakSession session, AccessToken token, RealmModel realm, ClientModel client, UserModel user,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = client.getProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {
            if (!mapping.getProtocol().equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) continue;

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null || !(mapper instanceof OIDCAccessTokenMapper)) continue;
            token = ((OIDCAccessTokenMapper)mapper).transformAccessToken(token, mapping, session, userSession, clientSession);



        }
        return token;
    }
    public void transformIDToken(KeycloakSession session, IDToken token, RealmModel realm, ClientModel client, UserModel user,
                                      UserSessionModel userSession, ClientSessionModel clientSession) {
        Set<ProtocolMapperModel> mappings = client.getProtocolMappers();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        for (ProtocolMapperModel mapping : mappings) {
            if (!mapping.getProtocol().equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) continue;

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null || !(mapper instanceof OIDCIDTokenMapper)) continue;
            token = ((OIDCIDTokenMapper)mapper).transformIDToken(token, mapping, session, userSession, clientSession);



        }
    }


    protected AccessToken initToken(RealmModel realm, ClientModel client, UserModel user, UserSessionModel session, ClientSessionModel clientSession) {
        AccessToken token = new AccessToken();
        if (clientSession != null) token.clientSession(clientSession.getId());
        token.id(KeycloakModelUtils.generateId());
        token.subject(user.getId());
        token.audience(client.getClientId());
        token.issuedNow();
        token.issuedFor(client.getClientId());
        token.issuer(realm.getName());
        if (session != null) {
            token.setSessionState(session.getId());
        }
        if (realm.getAccessTokenLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getAccessTokenLifespan());
        }
        Set<String> allowedOrigins = client.getWebOrigins();
        if (allowedOrigins != null) {
            token.setAllowedOrigins(allowedOrigins);
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
            ApplicationModel app = (ApplicationModel) role.getContainer();
            access = token.getResourceAccess(app.getName());
            if (access == null) {
                access = token.addAccess(app.getName());
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

        public AccessTokenResponseBuilder generateAccessToken(KeycloakSession session, String scopeParam, ClientModel client, UserModel user, UserSessionModel userSession, ClientSessionModel clientSession) {
            Set<RoleModel> requestedRoles = getAccess(scopeParam, client, user);
            accessToken = createClientAccessToken(session, requestedRoles, realm, client, user, userSession, clientSession);
            return this;
        }

        public AccessTokenResponseBuilder generateRefreshToken() {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }
            refreshToken = new RefreshToken(accessToken);
            refreshToken.id(KeycloakModelUtils.generateId());
            refreshToken.issuedNow();
            refreshToken.expiration(Time.currentTime() + realm.getSsoSessionIdleTimeout());
            return this;
        }

        public AccessTokenResponseBuilder generateIDToken() {
            if (accessToken == null) {
                throw new IllegalStateException("accessToken not set");
            }
            idToken = new IDToken();
            idToken.id(KeycloakModelUtils.generateId());
            idToken.subject(accessToken.getSubject());
            idToken.audience(client.getClientId());
            idToken.issuedNow();
            idToken.issuedFor(accessToken.getIssuedFor());
            idToken.issuer(accessToken.getIssuer());
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

}
