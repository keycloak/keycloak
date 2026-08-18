package org.keycloak.services.client;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public final class ClientServiceFactory {

    private ClientServiceFactory() {
    }

    public static ClientService create(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator permissions) {
        DefaultClientService delegate = new DefaultClientService(session, realm, permissions);
        return new ScimBackedClientService(session, permissions, delegate);
    }
}
