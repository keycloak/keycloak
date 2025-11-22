package org.keycloak.protocol.ssf.endpoint.admin;

import jakarta.ws.rs.Path;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * SsfAdmin resource to manage SSF related components.
 *
 * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf}
 */
public class SsfAdminResource {

    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final AdminPermissionEvaluator auth;
    protected final AdminEventBuilder adminEvent;

    public SsfAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    /**
     * Exposes the {@link SsfReceiverAdminResource} for managing SSF Receivers as a custom endpoint.
     *
     * Checks if the current user can access the SSF admin resource for receivers.
     *
     * The endpoint is available via {@code $KC_ADMIN_URL/admin/realms/{realm}/ssf/receivers}
     * @return
     */
    @Path("receivers")
    public SsfReceiverAdminResource receiverManagementEndpoint() {

        checkReceiverAdminResourceAccess();

        return receiverAdminResource();
    }

    /**
     * Provies the actual {@link SsfReceiverAdminResource}.
     * @return
     */
    protected SsfReceiverAdminResource receiverAdminResource() {
        return new SsfReceiverAdminResource(session, auth);
    }

    /**
     * Checks if the current user can access the SSF admin resource for receivers.
     */
    protected void checkReceiverAdminResourceAccess() {
        auth.realm().requireManageIdentityProviders();
    }
}
