package org.keycloak.tests.clustering;

import org.keycloak.testframework.annotations.InjectLogs;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.Logs;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

/**
 * Purpose of this test class is to verify {@link InjectLogs} with multiple cluster nodes.
 */
@KeycloakIntegrationTest
class LogsClusterTest {

    @InjectLogs
    Logs node0Logs;

    @InjectLogs(node = 1)
    Logs node1Logs;

    @Test
    void capturesStartupMessageFromBothNodes() {
        node0Logs.assertContains("Keycloak");
        node0Logs.assertContains("Listening on");
        node1Logs.assertContains("Keycloak");
        node1Logs.assertContains("Listening on");
    }

    @Test
    void filtersByLevelOnBothNodes() {
        node0Logs.assertContains(Logger.Level.INFO, "ISPN000094");
        node1Logs.assertContains(Logger.Level.INFO, "ISPN000094");
    }

    @Test
    void isolatesLogsByNode() {
        node0Logs.assertContains("Initializing master realm");
        node1Logs.assertNotContains("Initializing master realm");
    }

}
