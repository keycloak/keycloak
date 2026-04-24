package org.keycloak.tests.utils.runonserver;

import org.keycloak.testframework.remote.runonserver.RunOnServer;
import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @deprecated Bridge class for legacy Arquillian testsuite.
 * This version will be removed when the legacy testsuite is fully migrated.
 */
@Deprecated
public final class RunOnServerHelpers {

    private static KeycloakTestingClient testingClient;

    public static void setTestingClient(KeycloakTestingClient keycloakTestingClient) {
        testingClient = keycloakTestingClient;
    }

    public static RunOnServer removeUserSession(String realmName, String sessionId) {
        return session -> testingClient.testing().removeUserSession(realmName, sessionId);
    }
}
