package org.keycloak.ssf.transmitter.support;

import java.util.List;
import java.util.regex.Pattern;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

public class SsfAuthUtil {

    private static final Logger log = Logger.getLogger(SsfAuthUtil.class);

    public static final String AUTH_KEY = "auth";

    private static final Pattern SCOPE_DELIMITER = Pattern.compile(" ");

    public static AuthenticationManager.AuthResult authenticate() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        var auth = authenticator.authenticate();
        if (auth == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        SsfAuthUtil.setAuth(session, auth);
        return auth;
    }

    private static void setAuth(KeycloakSession session, AuthenticationManager.AuthResult auth) {
        session.setAttribute(AUTH_KEY, auth);
    }

    private static AuthenticationManager.AuthResult getAuthResult() {
        return (AuthenticationManager.AuthResult) KeycloakSessionUtil.getKeycloakSession().getAttribute(AUTH_KEY);
    }

    public static boolean canManage() {
        return checkScopePermission(Ssf.SCOPE_SSF_MANAGE);
    }

    public static boolean canRead() {
        return checkScopePermission(Ssf.SCOPE_SSF_READ);
    }

    public static boolean checkScopePermission(String scope) {

        // 0. Token must be valid
        var authResult = getAuthResult();
        if (authResult == null) {
            log.trace("SSF auth denied: no authentication result available");
            return false;
        }

        ClientModel client = authResult.client();

        // 1. Client must have SSF enabled
        if (!Boolean.parseBoolean(client.getAttribute(ClientStreamStore.SSF_ENABLED_KEY))) {
            log.tracef("SSF auth denied: SSF receiver not enabled for client %s", client.getClientId());
            return false;
        }

        // 2. Service account check (default: required when attribute is
        //    absent or any value other than "false")
        String requireSaValue = client.getAttribute(ClientStreamStore.SSF_REQUIRE_SERVICE_ACCOUNT_KEY);
        boolean requireServiceAccount = !"false".equalsIgnoreCase(requireSaValue);
        if (requireServiceAccount) {
            if (!client.isServiceAccountsEnabled()) {
                log.tracef("SSF auth denied: service account required but not enabled for client %s", client.getClientId());
                return false;
            }
            // getServiceAccountClientLink() returns the internal client UUID
            // (see UserModel.setServiceAccountClientLink(String clientInternalId)
            // and ClientManager.enableServiceAccount which calls
            // user.setServiceAccountClientLink(client.getId())) — NOT the
            // public clientId. Compare against client.getId() so the gate
            // correctly accepts the receiver's own service-account bearer
            // (link == client.getId()) and rejects anything else: regular
            // users (link == null) and SAs of other clients (link == some
            // other UUID).
            if (!client.getId().equals(authResult.user().getServiceAccountClientLink())) {
                log.tracef("SSF auth denied: token user is not the service account for client %s", client.getClientId());
                return false;
            }
        }

        // 3. Role check (only when configured)
        String requiredRole = client.getAttribute(ClientStreamStore.SSF_REQUIRED_ROLE_KEY);
        if (requiredRole != null && !requiredRole.isBlank()) {
            if (!hasRole(authResult, requiredRole)) {
                log.tracef("SSF auth denied: token missing required role '%s' for client %s", requiredRole, client.getClientId());
                return false;
            }
        }

        // 4. Scope check
        String tokenScope = authResult.token().getScope();
        if (tokenScope == null) {
            log.tracef("SSF auth denied: token has no scope claim for client %s", client.getClientId());
            return false;
        }

        boolean containsScope = List.of(SCOPE_DELIMITER.split(tokenScope)).contains(scope);
        if (!containsScope) {
            log.tracef("SSF auth denied: token missing required scope '%s' for client %s", scope, client.getClientId());
            return false;
        }

        // SSF 1.0 §8.1.1 inactivity_timeout: any authenticated hit
        // on a stream-management or poll endpoint counts as eligible
        // receiver activity and MUST restart the inactivity clock.
        // Stamping here covers every receiver-facing path in one
        // place — individual resource handlers don't have to
        // remember.
        SsfActivityTracker.stamp(client);

        return true;
    }

    /**
     * Checks whether the token carries the given role. The role value
     * follows the same format the admin UI role picker produces:
     * <ul>
     *     <li>{@code roleName} — checked as a realm role.</li>
     *     <li>{@code clientId.roleName} — checked as a client role on
     *         the specified client.</li>
     * </ul>
     */
    public static boolean hasRole(AuthenticationManager.AuthResult authResult, String roleValue) {
        return hasRole(authResult.token(), roleValue);
    }

    /**
     * Token-level overload of {@link #hasRole(AuthenticationManager.AuthResult, String)}.
     * Callers that only have the decoded {@link AccessToken} (e.g. the
     * admin emit endpoint goes through {@code AdminAuth}, not the SSF
     * receiver auth pipeline) can check roles without needing a full
     * {@code AuthResult} wrapper.
     */
    public static boolean hasRole(AccessToken token, String roleValue) {
        if (token == null || roleValue == null || roleValue.isBlank()) {
            return false;
        }

        int dot = roleValue.indexOf('.');
        if (dot > 0 && dot < roleValue.length() - 1) {
            // Client role: "clientId.roleName"
            String clientId = roleValue.substring(0, dot);
            String roleName = roleValue.substring(dot + 1);
            AccessToken.Access clientAccess = token.getResourceAccess(clientId);
            return clientAccess != null
                    && clientAccess.getRoles() != null
                    && clientAccess.getRoles().contains(roleName);
        }

        // Realm role: plain "roleName"
        AccessToken.Access realmAccess = token.getRealmAccess();
        return realmAccess != null
                && realmAccess.getRoles() != null
                && realmAccess.getRoles().contains(roleValue);
    }
}
