package org.keycloak.ssf.transmitter.support;

import java.time.Duration;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

public class SsfUtil {

    private static final Logger log = Logger.getLogger(SsfUtil.class);

    public static String getIssuerUrl(KeycloakSession session) {
        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();

        String frontendUrl = realm.getAttribute("frontendUrl");
        if (frontendUrl != null && !frontendUrl.isBlank())  {
            return frontendUrl;
        }

        String hostnameUrl = System.getenv().get("KC_HOSTNAME_URL");
        if (hostnameUrl != null && !hostnameUrl.isBlank()) {
            return appendRealmPath(hostnameUrl, realm.getName());
        }

        String configuredHostname = Config.scope("hostname", "v2").get("hostname");
        if (configuredHostname != null && !configuredHostname.isBlank()
            && (configuredHostname.startsWith("http://") || configuredHostname.startsWith("https://"))) {
            return appendRealmPath(configuredHostname, realm.getName());
        }

        try {
            return appendRealmPath(context.getUri().getBaseUri().toString(), realm.getName());
        } catch (RuntimeException ignored) {
            // No active HTTP request context (e.g. scheduled outbox drainer).
        }

        throw new IllegalStateException(
                "Cannot resolve SSF issuer URL for realm '" + realm.getName() + "' outside an HTTP request. "
                        + "Configure one of: the realm 'frontendUrl' attribute, the KC_HOSTNAME_URL environment variable, "
                        + "or the Keycloak '--hostname' option with a full URL.");
    }

    private static String appendRealmPath(String baseUrl, String realmName) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + "realms/" + realmName;
    }

    /**
     * Minimal duration parser: supports {@code ms}, {@code s}, {@code m},
     * {@code h}, {@code d} suffixes, falling back to seconds when no unit
     * is given.
     */
    public static long parseDurationMillis(String value, long defaultMillis) {
        try {
            String trimmed = value.trim().toLowerCase();
            if (trimmed.endsWith("ms")) {
                return Long.parseLong(trimmed.substring(0, trimmed.length() - 2).trim());
            }
            if (trimmed.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            if (trimmed.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            if (trimmed.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            if (trimmed.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim())).toMillis();
            }
            return Duration.ofSeconds(Long.parseLong(trimmed)).toMillis();
        } catch (NumberFormatException e) {
            log.warnf("Invalid interval '%s' — falling back to default %dms", value, defaultMillis);
            return defaultMillis;
        }
    }

    public static Map<String, Object> treeToMap(JsonNode node) {
        return JsonSerialization.mapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    private static final String ADMIN_EVENT_USERS_PREFIX = "users/";

    public static String userIdFromAdminEventPath(AdminEvent adminEvent) {
        if (adminEvent == null) {
            return null;
        }
        if (!ResourceType.USER.equals(adminEvent.getResourceType())) {
            return null;
        }
        return userIdFromAdminEventPath(adminEvent.getResourcePath());
    }

    public static String userIdFromAdminEventPath(String resourcePath) {
        if (resourcePath == null || !resourcePath.startsWith(ADMIN_EVENT_USERS_PREFIX)) {
            return null;
        }
        int start = ADMIN_EVENT_USERS_PREFIX.length();
        int end = resourcePath.indexOf('/', start);
        String id = end < 0 ? resourcePath.substring(start) : resourcePath.substring(start, end);
        return id.isEmpty() ? null : id;
    }

}
