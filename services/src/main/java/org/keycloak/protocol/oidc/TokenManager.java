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
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.util.Time;

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

    public AccessToken refreshAccessToken(KeycloakSession session, UriInfo uriInfo, ClientConnection connection, RealmModel realm, ClientModel client, String encodedRefreshToken, EventBuilder event) throws OAuthErrorException {
        RefreshToken refreshToken = verifyRefreshToken(realm, encodedRefreshToken);

        event.user(refreshToken.getSubject()).session(refreshToken.getSessionState()).detail(Details.REFRESH_TOKEN_ID, refreshToken.getId());

        UserModel user = session.users().getUserById(refreshToken.getSubject(), realm);
        if (user == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Unknown user");
        }

        if (!user.isEnabled()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "User disabled", "User disabled");
        }

        UserSessionModel userSession = session.sessions().getUserSession(realm, refreshToken.getSessionState());
        int currentTime = Time.currentTime();
        if (!AuthenticationManager.isSessionValid(realm, userSession)) {
            AuthenticationManager.logout(session, realm, userSession, uriInfo, connection);
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Session not active", "Session not active");
        }

        if (!client.getClientId().equals(refreshToken.getIssuedFor())) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Unmatching clients", "Unmatching clients");
        }

        if (refreshToken.getIssuedAt() < client.getNotBefore()) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Stale refresh token");
        }

        verifyAccess(refreshToken, realm, client, user);

        AccessToken accessToken = initToken(realm, client, user, userSession);
        accessToken.setRealmAccess(refreshToken.getRealmAccess());
        accessToken.setResourceAccess(refreshToken.getResourceAccess());

        userSession.setLastSessionRefresh(currentTime);

        return accessToken;
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

    public AccessToken createClientAccessToken(Set<RoleModel> requestedRoles, RealmModel realm, ClientModel client, UserModel user, UserSessionModel session) {
        AccessToken token = initToken(realm, client, user, session);
        for (RoleModel role : requestedRoles) {
            addComposites(token, role);
        }
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

    public void verifyAccess(AccessToken token, RealmModel realm, ClientModel client, UserModel user) throws OAuthErrorException {
        ApplicationModel clientApp = (client instanceof ApplicationModel) ? (ApplicationModel)client : null;


        if (token.getRealmAccess() != null) {
            for (String roleName : token.getRealmAccess().getRoles()) {
                RoleModel role = realm.getRole(roleName);
                if (role == null) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid realm role " + roleName);
                }
                if (!user.hasRole(role)) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User no long has permission for realm role: " + roleName);
                }
                if (!client.hasScope(role)) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "Client no longer has realm scope: " + roleName);
                }
            }
        }
        if (token.getResourceAccess() != null) {
            for (Map.Entry<String, AccessToken.Access> entry : token.getResourceAccess().entrySet()) {
                ApplicationModel app = realm.getApplicationByName(entry.getKey());
                if (app == null) {
                    throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "Application no longer exists", "Application no longer exists: " + app.getName());
                }
                for (String roleName : entry.getValue().getRoles()) {
                    RoleModel role = app.getRole(roleName);
                    if (role == null) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Invalid refresh token", "Unknown application role: " + roleName);
                    }
                    if (!user.hasRole(role)) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "User no long has permission for application role " + roleName);
                    }
                    if (clientApp != null && !clientApp.equals(app) && !client.hasScope(role)) {
                        throw new OAuthErrorException(OAuthErrorException.INVALID_SCOPE, "Client no longer has application scope" + roleName);
                    }
                }

            }
        }
    }

    public void initClaims(IDToken token, ClientModel model, UserModel user) {
        if (ClaimMask.hasUsername(model.getAllowedClaimsMask())) {
            token.setPreferredUsername(user.getUsername());
        }
        if (ClaimMask.hasEmail(model.getAllowedClaimsMask())) {
            token.setEmail(user.getEmail());
            token.setEmailVerified(user.isEmailVerified());
        }
        if (ClaimMask.hasName(model.getAllowedClaimsMask())) {
            token.setFamilyName(user.getLastName());
            token.setGivenName(user.getFirstName());
            StringBuilder fullName = new StringBuilder();
            if (user.getFirstName() != null) fullName.append(user.getFirstName()).append(" ");
            if (user.getLastName() != null) fullName.append(user.getLastName());
            token.setName(fullName.toString());
        }
    }

    protected IDToken initIDToken(RealmModel realm, ClientModel claimer, UserModel client, UserModel user) {
        IDToken token = new IDToken();
        token.id(KeycloakModelUtils.generateId());
        token.subject(user.getId());
        token.audience(claimer.getClientId());
        token.issuedNow();
        token.issuedFor(client.getUsername());
        token.issuer(realm.getName());
        if (realm.getAccessTokenLifespan() > 0) {
            token.expiration(Time.currentTime() + realm.getAccessTokenLifespan());
        }
        initClaims(token, claimer, user);
        return token;
    }



    protected AccessToken initToken(RealmModel realm, ClientModel client, UserModel user, UserSessionModel session) {
        AccessToken token = new AccessToken();
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
        initClaims(token, client, user);
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

    public AccessTokenResponseBuilder responseBuilder(RealmModel realm, ClientModel client, EventBuilder event) {
        return new AccessTokenResponseBuilder(realm, client, event);
    }

    public class AccessTokenResponseBuilder {
        RealmModel realm;
        ClientModel client;
        AccessToken accessToken;
        RefreshToken refreshToken;
        IDToken idToken;
        EventBuilder event;

        public AccessTokenResponseBuilder(RealmModel realm, ClientModel client, EventBuilder event) {
            this.realm = realm;
            this.client = client;
            this.event = event;
        }

        public AccessTokenResponseBuilder accessToken(AccessToken accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        public AccessTokenResponseBuilder refreshToken(RefreshToken refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AccessTokenResponseBuilder generateAccessToken(String scopeParam, ClientModel client, UserModel user, UserSessionModel session) {
            Set<RoleModel> requestedRoles = getAccess(scopeParam, client, user);
            accessToken = createClientAccessToken(requestedRoles, realm, client, user, session);
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
            idToken.setPreferredUsername(accessToken.getPreferredUsername());
            idToken.setGivenName(accessToken.getGivenName());
            idToken.setMiddleName(accessToken.getMiddleName());
            idToken.setFamilyName(accessToken.getFamilyName());
            idToken.setName(accessToken.getName());
            idToken.setNickName(accessToken.getNickName());
            idToken.setGender(accessToken.getGender());
            idToken.setPicture(accessToken.getPicture());
            idToken.setProfile(accessToken.getProfile());
            idToken.setWebsite(accessToken.getWebsite());
            idToken.setBirthdate(accessToken.getBirthdate());
            idToken.setEmail(accessToken.getEmail());
            idToken.setEmailVerified(accessToken.getEmailVerified());
            idToken.setLocale(accessToken.getLocale());
            idToken.setFormattedAddress(accessToken.getFormattedAddress());
            idToken.setAddress(accessToken.getAddress());
            idToken.setStreetAddress(accessToken.getStreetAddress());
            idToken.setLocality(accessToken.getLocality());
            idToken.setRegion(accessToken.getRegion());
            idToken.setPostalCode(accessToken.getPostalCode());
            idToken.setCountry(accessToken.getCountry());
            idToken.setPhoneNumber(accessToken.getPhoneNumber());
            idToken.setPhoneNumberVerified(accessToken.getPhoneNumberVerified());
            idToken.setZoneinfo(accessToken.getZoneinfo());
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
            }
            int notBefore = realm.getNotBefore();
            if (client.getNotBefore() > notBefore) notBefore = client.getNotBefore();
            res.setNotBeforePolicy(notBefore);
            return res;
        }
    }

}
