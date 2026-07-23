package org.keycloak.scim.services.admin;

import jakarta.ws.rs.Path;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * Root admin resource for SCIM, exposed under {@code /admin/realms/{realm}/scim}.
 */
public class ScimAdminResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public ScimAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth,
            AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @Path("resource-types")
    public ScimResourceTypesAdminResource resourceTypes() {
        return new ScimResourceTypesAdminResource(session, realm, auth, adminEvent);
    }
}
