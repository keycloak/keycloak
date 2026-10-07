package org.keycloak.testframework.realm;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ManagedCleanupTest {

    @Test
    public void realmCleanupRunsAllTasksAndReportsFailures() {
        List<String> executed = new ArrayList<>();
        ManagedRealmCleanup cleanup = new ManagedRealmCleanup()
                .add(r -> fail(executed, "first"))
                .add(r -> executed.add("second"))
                .add(r -> fail(executed, "third"));

        assertFailures(() -> cleanup.runCleanupTasks(null), "Failed to run 2 of 3 cleanup tasks for the realm", "first", "third");
        Assertions.assertEquals(List.of("first", "second", "third"), executed);

        executed.clear();
        cleanup.runCleanupTasks(null);
        Assertions.assertEquals(List.of(), executed);
    }

    @Test
    public void userCleanupRunsAllTasksAndReportsFailures() {
        List<String> executed = new ArrayList<>();
        ManagedUserCleanup cleanup = new ManagedUserCleanup()
                .add(u -> fail(executed, "first"))
                .add(u -> executed.add("second"))
                .add(u -> fail(executed, "third"));

        assertFailures(() -> cleanup.runCleanupTasks(null), "Failed to run 2 of 3 cleanup tasks for the user", "first", "third");
        Assertions.assertEquals(List.of("first", "second", "third"), executed);

        executed.clear();
        cleanup.runCleanupTasks(null);
        Assertions.assertEquals(List.of(), executed);
    }

    @Test
    public void clientCleanupRunsAllTasksAndReportsFailures() {
        List<String> executed = new ArrayList<>();
        ManagedClientCleanup cleanup = new ManagedClientCleanup()
                .add(c -> fail(executed, "first"))
                .add(c -> executed.add("second"))
                .add(c -> fail(executed, "third"));

        assertFailures(() -> cleanup.runCleanupTasks(null), "Failed to run 2 of 3 cleanup tasks for the client", "first", "third");
        Assertions.assertEquals(List.of("first", "second", "third"), executed);

        executed.clear();
        cleanup.runCleanupTasks(null);
        Assertions.assertEquals(List.of(), executed);
    }

    private static void fail(List<String> executed, String task) {
        executed.add(task);
        throw new RuntimeException(task);
    }

    private static void assertFailures(Runnable cleanup, String expectedMessage, String... expectedSuppressed) {
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, cleanup::run);
        Assertions.assertEquals(expectedMessage, failure.getMessage());

        List<String> suppressed = new ArrayList<>();
        for (Throwable t : failure.getSuppressed()) {
            suppressed.add(t.getMessage());
        }
        Assertions.assertEquals(List.of(expectedSuppressed), suppressed);
    }

}
