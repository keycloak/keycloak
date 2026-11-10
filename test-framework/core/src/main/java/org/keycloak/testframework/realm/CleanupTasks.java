package org.keycloak.testframework.realm;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

final class CleanupTasks {

    private CleanupTasks() {
    }

    /**
     * Run all cleanup tasks, even if some of them fail, and clear the list afterwards
     *
     * @param tasks the cleanup tasks to run
     * @param runner runs a single cleanup task
     * @param type the type of resource being cleaned up, used in the failure message
     * @throws IllegalStateException if any of the tasks failed, with the failures added as suppressed exceptions
     */
    static <T> void runAll(List<T> tasks, Consumer<T> runner, String type) {
        List<Throwable> failures = new LinkedList<>();
        int total = tasks.size();
        try {
            for (T task : tasks) {
                try {
                    runner.accept(task);
                } catch (RuntimeException | AssertionError e) {
                    failures.add(e);
                }
            }
        } finally {
            tasks.clear();
        }

        if (!failures.isEmpty()) {
            IllegalStateException failure = new IllegalStateException(
                    "Failed to run %d of %d cleanup tasks for the %s".formatted(failures.size(), total, type));
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
    }

}
