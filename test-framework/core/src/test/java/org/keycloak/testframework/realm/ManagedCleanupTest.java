package org.keycloak.testframework.realm;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ManagedCleanupTest {

    @Test
    public void organizationCleanupRunsAllTasksAndReportsFailures() {
        List<String> executed = new ArrayList<>();
        ManagedOrganizationCleanup cleanup = new ManagedOrganizationCleanup()
                .add(o -> fail(executed, "first"))
                .add(o -> executed.add("second"))
                .add(o -> fail(executed, "third"));

        assertFailures(() -> cleanup.runCleanupTasks(null), "Failed to run 2 of 3 cleanup tasks for the organization", "first", "third");
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
