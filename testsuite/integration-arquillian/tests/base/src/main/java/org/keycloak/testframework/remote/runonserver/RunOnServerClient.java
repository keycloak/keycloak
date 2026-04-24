package org.keycloak.testframework.remote.runonserver;


import org.keycloak.tests.utils.runonserver.RunOnServerHelpers;
import org.keycloak.testsuite.client.KeycloakTestingClient;

/**
 * @deprecated Bridge class for legacy Arquillian testsuite.
 * This version will be removed when the legacy testsuite is fully migrated.
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
