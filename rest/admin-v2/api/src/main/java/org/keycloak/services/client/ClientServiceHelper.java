package org.keycloak.services.client;

import jakarta.annotation.Nonnull;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.RealmAdminResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.jboss.logging.Logger;

/**
 * Helper method to obtain client service implementation for Admin Client API v2
 * <p>
 * TODO this is only temporary solution to distinguish between legacy and default client service
 */
public class ClientServiceHelper {
    private static final Logger log = Logger.getLogger(ClientServiceHelper.class);

    public static boolean isLegacyClientServiceEnabled() {
        return Boolean.parseBoolean(System.getProperty("kc.admin-v2.client-service.legacy.enabled", "true"));
    }

    public static ClientService getClientService(@Nonnull KeycloakSession session,
                                                 @Nonnull RealmModel realm,
                                                 @Nonnull AdminPermissionEvaluator permissions,
                                                 @Nonnull RealmAdminResource realmAdminResource) {
        if (isLegacyClientServiceEnabled()) {
            return new DefaultClientService(session, realm, permissions, realmAdminResource);
        } else {
            log.debug("New ClientService is used");
            return new NewClientService(session, realm, permissions, realmAdminResource);
        }
    }

}
