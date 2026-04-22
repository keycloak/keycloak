package org.keycloak.testframework.remote.runonserver;


import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @deprecated transition class to ease test migration to the new test framework
 */
@Deprecated
public class RunOnServerClient {

    public RunOnServerClient(KeycloakTestingClient keycloakTestingClient) {
        RunOnServerHelpers.setTestingClient(keycloakTestingClient);
    }

    public void run(RunOnServer function) {
        function.run(null);
    }
}
