package org.keycloak.testframework.remote.runonserver;


import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @deprecated transition class to ease test migration to the new test framework
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
