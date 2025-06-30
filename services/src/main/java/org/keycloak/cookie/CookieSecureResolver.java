package org.keycloak.cookie;

import jakarta.ws.rs.core.NewCookie;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;

import java.net.URI;

class CookieSecureResolver {

    private final KeycloakContext context;
    private final boolean sameSiteLegacyEnabled;

    CookieSecureResolver(KeycloakContext context, boolean sameSiteLegacyEnabled) {
        this.context = context;
        this.sameSiteLegacyEnabled = sameSiteLegacyEnabled;
    }

    boolean resolveSecure(NewCookie.SameSite sameSite) {
        // Due to cookies with SameSite=none secure context is always required when same-site legacy cookies are disabled
        if (!sameSiteLegacyEnabled) {
            return true;
        } else {
            // SameSite=none requires secure context
            if (NewCookie.SameSite.NONE.equals(sameSite)) {
                return true;
            }

            URI requestUri = context.getUri().getRequestUri();
            RealmModel realm = context.getRealm();
            if (realm != null && realm.getSslRequired().isRequired(requestUri.getHost())) {
                return true;
            }

            return false;
        }
    }

}
