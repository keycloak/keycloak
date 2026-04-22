package org.keycloak.testframework.remote.runonserver;

import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @deprecated transition class to ease test migration to the new test framework
 */
@Deprecated
public final class RunOnServerUtils {

    private static KeycloakTestingClient testingClient;

    public static void setTestingClient(KeycloakTestingClient keycloakTestingClient) {
        testingClient = keycloakTestingClient;
    }

    public static void removeUserSession(KeycloakSession session, String realmName, String sessionId) {
        testingClient.testing().removeUserSession(realmName, sessionId);
    }

}
