package org.keycloak.adapters;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.util.UriUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterUtils {

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
}
