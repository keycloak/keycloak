package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.Path;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * http://localhost:8081/admin/realms/ssf-demo/ssf
 */
public class SsfAdminResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public SsfAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    /**
     * http://localhost:8081/admin/realms/ssf-demo/ssf/receivers
     * @return
     */
    @Path("receivers")
    public SsfReceiverAdminResource receiverManagementEndpoint() {

        auth.realm().requireManageIdentityProviders();

        return new SsfReceiverAdminResource(session, auth);
    }
}
