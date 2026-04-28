package org.keycloak.services.resources.admin;

import java.util.List;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.cors.Cors;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * Created by st on 21/03/17.
 */
public class AdminCorsPreflightService {

    private final ClientModel adminConsole;

    public AdminCorsPreflightService(ClientModel adminConsole) {
        this.adminConsole = adminConsole;
    }

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("{any:.*}")
    @OPTIONS
    public Response preflight() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        return Cors.builder().preflight().allowedMethods("GET", "PUT", "POST", "DELETE").auth()
                .allowedOrigins(session, adminConsole).add(Response.ok());
    }

    public static ClientModel resolveAdminConsoleClient(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            String realmName = extractRealmName(session.getContext().getUri().getPathSegments());
            if (realmName != null) {
                realm = session.realms().getRealmByName(realmName);
            }
        }
        return realm == null ? null : realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private static String extractRealmName(List<PathSegment> segments) {
        if (segments.size() < 3) {
            return null;
        }
        // /admin/realms/{realm}/... (v1) or /admin/api/{realm}/... (v2)
        String resourceType = segments.get(1).getPath();
        if ("realms".equals(resourceType) || "api".equals(resourceType)) {
            return segments.get(2).getPath();
        }
        return null;
    }

}
