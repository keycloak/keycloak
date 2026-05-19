package org.keycloak.testframework.tests;

import java.util.List;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.log.InjectLogs;
import org.keycloak.testframework.log.LogEntry;
import org.keycloak.testframework.log.Logs;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LogTest {

    private static final Logger testLogger = Logger.getLogger("org.keycloak.mytest");

    @InjectLogs
    Logs logs;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    @Order(1)
    public void checkLogEntries() {
        testLogger.warn("Some warning from the test");
        runOnServer.run(s -> {
            Logger.getLogger("org.keycloak.package1").warn("First warning from the server");
            Logger.getLogger("org.keycloak.package2").warnf("%s %s from the server", "Second", "warning");
            Logger.getLogger("org.keycloak.package3").warnv("{0} {1} from the server", "Third", "warning");
        });

        List<LogEntry> logDuringTest = logs.getManagedKeycloakLogs().toList();

        Assertions.assertEquals(3, logDuringTest.size());
        assertLog(logDuringTest.get(0), "org.keycloak.package1", "First warning from the server");
        assertLog(logDuringTest.get(1), "org.keycloak.package2", "Second warning from the server");
        assertLog(logDuringTest.get(2), "org.keycloak.package3", "Third warning from the server");
    }

    @Test
    @Order(2)
    public void checkNoLogsFromPreviousTests() {
        Assertions.assertEquals(0, logs.getManagedKeycloakLogs().count());
    }

    private void assertLog(LogEntry logEntry, String expectedLoggerName, String expectedMessage) {
        Assertions.assertEquals(expectedLoggerName, logEntry.getLoggerName());
        Assertions.assertEquals(expectedMessage, logEntry.getMessage());
    }

}
