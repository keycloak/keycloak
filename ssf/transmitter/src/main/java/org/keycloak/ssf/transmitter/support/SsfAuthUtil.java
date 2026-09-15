package org.keycloak.ssf.transmitter.support;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.ErrorResponseException;
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
        AuthenticationManager.AuthResult auth;
        try {
            auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        } catch (NotAuthorizedException e) {
            // Thrown by AppAuthManager.extractAuthorizationHeaderToken when the
            // Authorization header does not carry a Bearer token (unsupported
            // scheme or no token part). RFC 6750 §3.1 treats an attempt with an
            // unsupported authentication method like a request without
            // credentials: bare challenge, no error code. The exception's own
            // response has no entity and would be rewritten by
            // KeycloakErrorHandler without the challenge, hence the mapping.
            throw unauthorized(session, null);
        }
        if (auth == null) {
            // RFC 6750 §3.1: a request without any credentials gets a bare
            // challenge; only a request that presented a bearer token which
            // failed validation gets error="invalid_token".
            boolean tokenPresent = session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.AUTHORIZATION) != null;
            throw unauthorized(session, tokenPresent ? "Token verification failed" : null);
        }
        SsfAuthUtil.setAuth(session, auth);
        return auth;
    }

    /**
     * Builds a 401 response carrying the RFC 6750 {@code WWW-Authenticate: Bearer}
     * challenge for the current realm. When {@code errorDescription} is
     * {@code null} the challenge carries no error code (no credentials were
     * presented); otherwise it carries {@code error="invalid_token"}.
     */
    public static Response unauthorizedResponse(KeycloakSession session, String errorDescription) {
        StringBuilder challenge = new StringBuilder("Bearer realm=")
                .append(quote(session.getContext().getRealm().getName()));
        Object entity;
        if (errorDescription != null) {
            challenge.append(", error=").append(quote(OAuthErrorException.INVALID_TOKEN))
                    .append(", error_description=").append(quote(errorDescription));
            entity = new OAuth2ErrorRepresentation(OAuthErrorException.INVALID_TOKEN, errorDescription);
        } else {
            // RFC 6750 §3.1: no credentials presented, so neither the
            // challenge nor the body carries error information. The entity
            // must still be non-null so RESTEasy returns this response
            // as-is instead of routing it through KeycloakErrorHandler.
            entity = Map.of();
        }
        return Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, challenge.toString())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(entity)
                .build();
    }

    /**
     * Builds a 403 response for a bearer token that verified but lacks the
     * privileges required by the endpoint (RFC 6750 §3.1 {@code insufficient_scope}).
     * Used for every {@link #canRead()} / {@link #canManage()} failure: missing
     * scope, missing required role, non-service-account bearer, or a client
     * that is not configured as an SSF receiver. The challenge advertises the
     * required scope so the receiver knows what to request.
     */
    public static Response insufficientScopeResponse(KeycloakSession session, String requiredScope) {
        String description = "Token is not authorized for the " + requiredScope + " scope";
        String challenge = "Bearer realm=" + quote(session.getContext().getRealm().getName())
                + ", error=" + quote(OAuthErrorException.INSUFFICIENT_SCOPE)
                + ", error_description=" + quote(description)
                + ", scope=" + quote(requiredScope);
        return Response.status(Response.Status.FORBIDDEN)
                .header(HttpHeaders.WWW_AUTHENTICATE, challenge)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new OAuth2ErrorRepresentation(OAuthErrorException.INSUFFICIENT_SCOPE, description))
                .build();
    }

    private static final Pattern HEADER_CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");

    /**
     * Renders a value as an RFC 9110 quoted-string for use in a
     * {@code WWW-Authenticate} challenge: ASCII control characters (including
     * CR/LF) are stripped so the value cannot break or ambiguously shape the
     * header, and {@code "} / {@code \} are backslash-escaped. Non-ASCII is
     * kept, as RFC 9110 permits obs-text inside quoted-strings.
     */
    static String quote(String value) {
        String sanitized = value == null ? "" : HEADER_CONTROL_CHARS.matcher(value).replaceAll("");
        return '"' + sanitized.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /**
     * Exception variant of {@link #unauthorizedResponse(KeycloakSession, String)}
     * for sub-resource locators that cannot return a {@link Response}. The
     * response carries an entity, so RESTEasy returns it as-is instead of
     * handing the exception to {@code KeycloakErrorHandler}, which would
     * drop the {@code WWW-Authenticate} header. {@link ErrorResponseException}
     * still marks the transaction rollback-only.
     */
    private static WebApplicationException unauthorized(KeycloakSession session, String errorDescription) {
        return new ErrorResponseException(unauthorizedResponse(session, errorDescription));
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
        if (client == null) {
            log.trace("SSF auth denied: authentication result carries no client");
            return false;
        }

        // 1. Client must be configured as an SSF receiver.
        if (!SsfUtil.isReceiverClient(client)) {
            log.tracef("SSF auth denied: client %s is not configured as an SSF receiver", client.getClientId());
            return false;
        }

        // ...and the client itself must be enabled.
        if (!client.isEnabled()) {
            log.tracef("SSF auth denied: SSF receiver client %s is disabled", client.getClientId());
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
