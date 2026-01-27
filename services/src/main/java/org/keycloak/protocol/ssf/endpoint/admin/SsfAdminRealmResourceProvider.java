package org.keycloak.protocol.ssf.endpoint.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * Exposes the {@link SsfAdminResource}
 */
public class SsfAdminRealmResourceProvider implements AdminRealmResourceProvider {

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new SsfAdminResource(session, realm, auth, adminEvent);
    }

    @Override
    public void close() {
        // NOOP
    }
}
