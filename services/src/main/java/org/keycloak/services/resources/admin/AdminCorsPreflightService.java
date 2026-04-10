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
import org.keycloak.services.managers.RealmManager;
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
        List<PathSegment> segments = session.getContext().getUri().getPathSegments();
        for (int i = 0; i + 1 < segments.size(); i++) {
            String segment = segments.get(i).getPath();
            if ("realms".equals(segment) || "api".equals(segment)) {
                RealmModel realm = new RealmManager(session).getRealmByName(segments.get(i + 1).getPath());
                return realm == null ? null : realm.getClientByClientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
            }
        }
        return null;
    }

}
