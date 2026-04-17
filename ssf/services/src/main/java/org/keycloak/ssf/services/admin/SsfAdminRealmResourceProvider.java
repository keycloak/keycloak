package org.keycloak.ssf.services.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;

/**
 * Exposes the {@link SsfAdminResource}
 */
public class SsfAdminRealmResourceProvider implements AdminRealmResourceProvider {

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        return new SsfAdminResource(session, realm, auth, adminEvent, transmitter);
    }

    @Override
    public void close() {
        // NOOP
    }
}
