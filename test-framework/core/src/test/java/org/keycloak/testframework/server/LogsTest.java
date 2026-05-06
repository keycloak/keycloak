package org.keycloak.testframework.server;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogsTest {

    private static final LogEntry INFO_KEYCLOAK = LogEntry.parse(
            "2024-08-21 08:14:33,591 INFO  [org.keycloak.services] (main) Keycloak 26.0.0 started", false);
    private static final LogEntry DEBUG_HIBERNATE = LogEntry.parse(
            "2024-08-21 08:14:33,591 DEBUG [org.hibernate.SQL] (executor-1) select * from users", false);
    private static final LogEntry ERROR_SERVICES = LogEntry.parse(
            "2024-08-21 08:14:33,591 ERROR [org.keycloak.services] (main) Something failed", false);
    private static final LogEntry STDERR_ERROR = new LogEntry(
            Logger.Level.ERROR, "org.keycloak", "Error on stderr", "ERROR [org.keycloak] Error on stderr", true);

    private static Logs logsOf(LogEntry... entries) {
        Logs logs = new Logs();
        for (LogEntry entry : entries) {
            logs.add(entry);
        }
        return logs;
    }

    @Test
    void getEntriesReturnsUnmodifiableList() {
        assertThrows(UnsupportedOperationException.class, () -> logsOf(INFO_KEYCLOAK).getEntries().add(DEBUG_HIBERNATE));
    }

    @Test
    void assertContainsFindsMatch() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE).assertContains("Keycloak 26.0.0 started"));
    }

    @Test
    void assertContainsThrowsWhenNoMatch() {
        assertThrows(AssertionError.class, () -> logsOf(INFO_KEYCLOAK).assertContains("nonexistent message"));
    }

    @Test
    void assertNotContainsPasses() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK).assertNotContains("nonexistent"));
    }

    @Test
    void assertNotContainsThrowsWhenFound() {
        assertThrows(AssertionError.class, () -> logsOf(INFO_KEYCLOAK).assertNotContains("Keycloak"));
    }

    @Test
    void assertContainsWithLevel() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE).assertContains(Logger.Level.DEBUG, "select"));
    }

    @Test
    void assertContainsWithLevelNoMatch() {
        assertThrows(AssertionError.class, () -> logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE).assertContains(Logger.Level.ERROR, "select"));
    }

    @Test
    void assertContainsWithLevelAndCategory() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE).assertContains(Logger.Level.INFO, "org.keycloak", "Keycloak"));
    }

    @Test
    void assertContainsWithLevelAndCategoryNoMatch() {
        assertThrows(AssertionError.class, () -> logsOf(INFO_KEYCLOAK).assertContains(Logger.Level.INFO, "org.hibernate", "Keycloak"));
    }

    @Test
    void assertNotContainsWithLevel() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK).assertNotContains(Logger.Level.ERROR, "Keycloak"));
    }

    @Test
    void assertNotContainsWithLevelThrowsWhenFound() {
        assertThrows(AssertionError.class, () -> logsOf(INFO_KEYCLOAK).assertNotContains(Logger.Level.INFO, "Keycloak"));
    }

    @Test
    void assertCountMatchesExact() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK, INFO_KEYCLOAK, DEBUG_HIBERNATE).assertCount("Keycloak", 2));
    }

    @Test
    void assertCountFailsOnMismatch() {
        assertThrows(AssertionError.class, () -> logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE).assertCount("Keycloak", 3));
    }

    @Test
    void assertStdErrContainsFindsStderrEntry() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK, STDERR_ERROR).assertStdErrContains("Error on stderr"));
    }

    @Test
    void assertStdErrContainsIgnoresStdout() {
        assertThrows(AssertionError.class, () -> logsOf(ERROR_SERVICES).assertStdErrContains("Something failed"));
    }

    @Test
    void assertStdErrNotContainsPasses() {
        assertDoesNotThrow(() -> logsOf(INFO_KEYCLOAK, STDERR_ERROR).assertStdErrNotContains("nonexistent"));
    }

    @Test
    void assertStdErrNotContainsThrowsWhenFound() {
        assertThrows(AssertionError.class, () -> logsOf(STDERR_ERROR).assertStdErrNotContains("Error on stderr"));
    }

    @Test
    void getOutputJoinsAllRawLines() {
        String output = logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE).getOutput();
        assertThat("output should contain first entry", output, containsString(INFO_KEYCLOAK.rawLine()));
        assertThat("output should contain second entry", output, containsString(DEBUG_HIBERNATE.rawLine()));
    }

    @Test
    void getStdErrOnlyIncludesStderrEntries() {
        String stderr = logsOf(INFO_KEYCLOAK, STDERR_ERROR, DEBUG_HIBERNATE).getStdErr();
        assertThat("stderr should contain stderr entry", stderr, containsString("Error on stderr"));
        assertThat("stderr should not contain stdout entry", stderr, not(containsString("Keycloak 26.0.0 started")));
    }

    @Test
    void emptyLogsReturnsEmptyOutput() {
        Logs logs = logsOf();
        assertThrows(AssertionError.class, () -> logs.assertContains("anything"));
        assertDoesNotThrow(() -> logs.assertNotContains("anything"));
        assertThat("output should be empty", logs.getOutput(), is(""));
        assertThat("stderr should be empty", logs.getStdErr(), is(""));
    }

    @Test
    void clearRemovesAllEntries() {
        Logs logs = logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE, ERROR_SERVICES);
        assertThat("should have 3 entries before clear", logs.getEntries().size(), is(3));
        logs.clear();
        assertThat("entries should be empty after clear", logs.getEntries(), is(empty()));
        assertThrows(AssertionError.class, () -> logs.assertContains("Keycloak"));
    }

    @Test
    void classViewSeesStartupAndOwnEntriesOnly() {
        Logs source = logsOf(INFO_KEYCLOAK, DEBUG_HIBERNATE);
        source.markStartupComplete();

        source.add(LogEntry.parse("2024-08-21 08:14:33,591 INFO  [org.keycloak] (main) class A log", false));
        Logs classA = source.createClassView();
        source.add(ERROR_SERVICES);
        assertDoesNotThrow(() -> classA.assertContains("Keycloak"), "class A should see startup");
        assertDoesNotThrow(() -> classA.assertContains("Something failed"), "class A should see its own entry");
        assertThrows(AssertionError.class, () -> classA.assertContains("class A log"),
                "class A should NOT see entries from between startup and its creation");

        Logs classB = source.createClassView();
        source.add(STDERR_ERROR);
        assertDoesNotThrow(() -> classB.assertContains("Keycloak"), "class B should see startup");
        assertDoesNotThrow(() -> classB.assertContains("Error on stderr"), "class B should see its own entry");
        assertThrows(AssertionError.class, () -> classB.assertContains("Something failed"),
                "class B should NOT see class A's entry");
        assertThrows(AssertionError.class, () -> classB.assertContains("class A log"),
                "class B should NOT see entries between startup and first class");
    }
}
