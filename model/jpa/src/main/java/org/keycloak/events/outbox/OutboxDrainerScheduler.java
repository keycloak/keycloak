package org.keycloak.events.outbox;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TaskRunner;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

/**
 * Schedules an {@link OutboxDrainerTask} so that its ticks run on an
 * {@link ExecutorsProvider} executor rather than on the timer thread.
 * Keycloak's basic {@code TimerProvider} is a single
 * {@code java.util.Timer}; a tick that waits on a slow destination
 * would otherwise delay every other scheduled task on the node
 * (session expiration, event cleanup, ...).
 *
 * <p>The timer only triggers; the tick itself is handed to the
 * {@value #TICK_EXECUTOR} executor (sizeable via
 * {@code spi-executors-default-outbox-drainer-min/max}). A trigger
 * that fires while the previous tick of the same task is still running
 * on this node is skipped, so ticks never queue up. Across nodes the
 * {@link ClusterAwareScheduledTaskRunner} lock, keyed by
 * {@link OutboxDrainerTask#getTaskName()}, keeps one tick per kind.
 */
public final class OutboxDrainerScheduler {

    private static final Logger log = Logger.getLogger(OutboxDrainerScheduler.class);

    public static final String TICK_EXECUTOR = "outbox-drainer";

    private OutboxDrainerScheduler() {
    }

    /**
     * Schedules {@code task} every {@code intervalMillis} with a
     * cluster-aware runner keyed by the task name.
     */
    public static TaskRunner schedule(KeycloakSession session, OutboxDrainerTask task, long intervalMillis) {
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        TaskRunner runner = new ClusterAwareScheduledTaskRunner(factory, task, intervalMillis, task.getTaskName());
        return schedule(session, runner, intervalMillis);
    }

    /**
     * Schedules an already built runner (for callers that customise
     * it) every {@code intervalMillis}, under the runner's task name.
     */
    public static TaskRunner schedule(KeycloakSession session, TaskRunner runner, long intervalMillis) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(runner, "runner");
        ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor(TICK_EXECUTOR);
        AtomicBoolean running = new AtomicBoolean();
        String name = runner.getTaskName();
        session.getProvider(TimerProvider.class).schedule(() -> trigger(runner, executor, running, name),
                intervalMillis, intervalMillis, name);
        return runner;
    }

    static void trigger(TaskRunner runner, ExecutorService executor, AtomicBoolean running, String name) {
        if (!running.compareAndSet(false, true)) {
            log.debugf("Skipping tick of %s: previous tick still running on this node", name);
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    runner.run();
                } finally {
                    running.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            running.set(false);
            log.warnf("Tick of %s rejected by executor %s — retried on the next trigger", name, TICK_EXECUTOR);
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }
}
