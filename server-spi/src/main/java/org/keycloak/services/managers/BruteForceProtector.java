package org.keycloak.services.managers;

import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface BruteForceProtector extends Provider {
    void failedLogin(RealmModel realm, String username, ClientConnection clientConnection);

    boolean isTemporarilyDisabled(KeycloakSession session, RealmModel realm, String username);
}
