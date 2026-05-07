package org.keycloak.testframework.tests;

import org.keycloak.testframework.annotations.InjectLogs;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.LogEntry;
import org.keycloak.testframework.server.Logs;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest
class LogsIntegrationTest {

    @InjectLogs
    Logs logs;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    void testGetEntriesContainsParsedStartupEntry() {
        LogEntry startupEntry = logs.getEntries().stream()
                .filter(e -> Logger.Level.INFO.equals(e.level()) && e.message() != null && e.message().contains("Keycloak"))
                .findFirst()
                .orElse(null);
        assertThat("should find an INFO entry with 'Keycloak' in message", startupEntry, notNullValue());
        assertThat("startup entry should have a category", startupEntry.category(), notNullValue());
        assertThat("startup entry rawLine should contain original output", startupEntry.rawLine(), containsString("Keycloak"));
    }

    @Test
    void testStartupLogContainsListeningOn() {
        logs.assertContains("Listening on");
    }

    @Test
    void testGetOutputContainsStartupMessage() {
        assertThat("getOutput should contain startup message", logs.getOutput(), containsString("Keycloak"));
    }

    @Test
    void testInfoLevelPresent() {
        logs.assertContains(Logger.Level.INFO, "Keycloak");
    }

    @Test
    void testClassSpecificLogCaptured() {
        runOnServer.run(session -> Logger.getLogger("org.keycloak.test").warn("LOG_CAPTURE_TEST_MESSAGE"));
        logs.assertContains(Logger.Level.WARN, "LOG_CAPTURE_TEST_MESSAGE");
        logs.assertContains("LOG_CAPTURE_TEST_MESSAGE");
    }
}
