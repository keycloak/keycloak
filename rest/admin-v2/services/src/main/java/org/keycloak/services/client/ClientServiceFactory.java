package org.keycloak.services.client;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public final class ClientServiceFactory {

    public static final String SCIM_SERVICE_ENABLED_PROPERTY = "kc.admin-v2.client-service.scim.enabled";

    private ClientServiceFactory() {
    }

    public static boolean isScimServiceEnabled() {
        return Boolean.parseBoolean(System.getProperty(SCIM_SERVICE_ENABLED_PROPERTY, "false"));
    }

    public static ClientService create(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator permissions) {
        DefaultClientService delegate = new DefaultClientService(session, realm, permissions);
        if (isScimServiceEnabled()) {
            return new ScimBackedClientService(session, permissions, delegate);
        }
        return delegate;
    }
}
