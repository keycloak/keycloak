package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.UriUtils;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterUtils {

    private static Logger log = Logger.getLogger(AdapterUtils.class);

    /**
     * Best effort to find origin for REST request calls from web UI application to REST application. In case of relative or absolute
     * "auth-server-url" is returned the URL from request. In case of "auth-server-url-for-backend-request" used in configuration, it returns
     * the origin of auth server.
     *
     * This may be the optimization in cluster, so if you have keycloak and applications on same host, the REST request doesn't need to
     * go through loadbalancer, but can be sent directly to same host.
     *
     * @param browserRequestURL
     * @param session
     * @return
     */
    public static String getOriginForRestCalls(String browserRequestURL, KeycloakSecurityContext session) {
        if (session instanceof RefreshableKeycloakSecurityContext) {
            KeycloakDeployment deployment = ((RefreshableKeycloakSecurityContext)session).getDeployment();
            switch (deployment.getRelativeUrls()) {
                case ALL_REQUESTS:
                case NEVER:
                    // Resolve baseURI from the request
                    return UriUtils.getOrigin(browserRequestURL);
                case BROWSER_ONLY:
                    // Resolve baseURI from the codeURL (This is already non-relative and based on our hostname)
                    return UriUtils.getOrigin(deployment.getTokenUrl());
                default:
                    return "";
            }
        } else {
            return UriUtils.getOrigin(browserRequestURL);
        }
    }

    public static Set<String> getRolesFromSecurityContext(RefreshableKeycloakSecurityContext session) {
        Set<String> roles = null;
        AccessToken accessToken = session.getToken();
        if (session.getDeployment().isUseResourceRoleMappings()) {
            if (log.isTraceEnabled()) {
                log.trace("useResourceRoleMappings");
            }
            AccessToken.Access access = accessToken.getResourceAccess(session.getDeployment().getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            if (log.isTraceEnabled()) {
                log.trace("use realm role mappings");
            }
            AccessToken.Access access = accessToken.getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        if (roles == null) roles = Collections.emptySet();
        if (log.isTraceEnabled()) {
            log.trace("Setting roles: ");
            for (String role : roles) {
                log.trace("   role: " + role);
            }
        }
        return roles;
    }

    public static String getPrincipalName(KeycloakDeployment deployment, AccessToken token) {
        String attr = "sub";
        if (deployment.getPrincipalAttribute() != null) attr = deployment.getPrincipalAttribute();
        String name = null;

        if ("sub".equals(attr)) {
            name = token.getSubject();
        } else if ("email".equals(attr)) {
            name = token.getEmail();
        } else if ("preferred_username".equals(attr)) {
            name = token.getPreferredUsername();
        } else if ("name".equals(attr)) {
            name = token.getName();
        } else if ("given_name".equals(attr)) {
            name = token.getGivenName();
        } else if ("family_name".equals(attr)) {
            name = token.getFamilyName();
        } else if ("nickname".equals(attr)) {
            name = token.getNickName();
        }
        if (name == null) name = token.getSubject();
        return name;
    }

    public static KeycloakPrincipal<RefreshableKeycloakSecurityContext> createPrincipal(KeycloakDeployment deployment, RefreshableKeycloakSecurityContext securityContext) {
        return new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(getPrincipalName(deployment, securityContext.getToken()), securityContext);
    }
}
