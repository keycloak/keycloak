package org.keycloak.adapters;

import java.util.Collections;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.UriUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterUtils {

    private static Logger log = Logger.getLogger(AdapterUtils.class);

    public static String getOrigin(String browserRequestURL, KeycloakSecurityContext session) {
        if (session instanceof RefreshableKeycloakSecurityContext) {
            KeycloakDeployment deployment = ((RefreshableKeycloakSecurityContext)session).getDeployment();
            switch (deployment.getRelativeUrls()) {
                case ALL_REQUESTS:
                    // Resolve baseURI from the request
                    return UriUtils.getOrigin(browserRequestURL);
                case BROWSER_ONLY:
                case NEVER:
                    // Resolve baseURI from the codeURL (This is already non-relative and based on our hostname)
                    return UriUtils.getOrigin(deployment.getCodeUrl());
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
}
