package org.keycloak.cookie;

import org.keycloak.models.KeycloakContext;
import org.keycloak.services.resources.RealmsResource;

class CookiePathResolver {

    private final KeycloakContext context;
    private String realmPath;

    private String requestPath;

    CookiePathResolver(KeycloakContext context) {
        this.context = context;
    }

    String resolvePath(CookieType cookieType) {
        switch (cookieType.getPath()) {
            case REALM:
                if (realmPath == null) {
                    realmPath = RealmsResource.realmBaseUrl(context.getUri()).path("/").build(context.getRealm().getName()).getRawPath();
                }
                return realmPath;
            case REQUEST:
                if (requestPath == null) {
                    requestPath = context.getUri().getRequestUri().getRawPath();
                }
                return requestPath;
            default:
                throw new IllegalArgumentException("Unsupported enum value " + cookieType.getPath().name());
        }
    }

}
