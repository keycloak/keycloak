package org.keycloak.protocol.ssf.support;

import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class SsfUtil {

    public static String getIssuerUrl(KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        String frontendUrl = realm.getAttribute("frontendUrl");
        if (frontendUrl != null && !frontendUrl.isBlank())  {
            return frontendUrl;
        }

        String hostnameUrl = System.getenv().get("KC_HOSTNAME_URL");
        if (hostnameUrl != null && !hostnameUrl.isBlank()) {
            if (!hostnameUrl.endsWith("/")) {
                hostnameUrl += "/";
            }
            return hostnameUrl + "realms/" + realm.getName();
        }

        String baseUrl = context.getUri().getBaseUri().toString() + "realms/" + realm.getName();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    public static Set<String> parseEventTypeAliases(String eventAliases) {
        return Set.copyOf(Stream.of(eventAliases.split(",")).map(String::trim).toList());
    }
}
