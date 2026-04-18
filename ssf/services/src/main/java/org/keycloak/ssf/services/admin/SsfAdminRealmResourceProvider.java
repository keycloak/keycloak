package org.keycloak.ssf.services.admin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;

/**
 * Exposes the {@link SsfAdminResource}
 */
public class SsfAdminRealmResourceProvider implements AdminRealmResourceProvider {

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        SubjectManagementService subjectManagementService = transmitter.subjectManagementService();
        SsfStreamStore streamStore = transmitter.streamStore();
        return new SsfAdminResource(session, realm, auth, adminEvent, transmitter, subjectManagementService, streamStore);
    }

    @Override
    public void close() {
        // NOOP
    }
}
