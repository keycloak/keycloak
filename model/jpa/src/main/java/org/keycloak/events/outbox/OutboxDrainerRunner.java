package org.keycloak.events.outbox;

import java.util.Objects;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.logging.MappedDiagnosticContextUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TaskRunner;
import org.keycloak.tracing.TracingProvider;

import org.jboss.logging.Logger;

/**
 * Runs an {@link OutboxDrainerTask} tick under a cluster-wide lock
 * keyed by the task name, without a surrounding transaction.
 *
 * <p>{@code ScheduledTaskRunner} wraps a whole task run in one session
 * transaction. For the drainer that transaction would span every
 * delivery of the tick: it is subject to the transaction timeout even
 * though all of the tick's database work happens in short transactions
 * of its own, and any connection it acquired would stay bound to the
 * thread while destinations are being called. This runner opens a
 * session only to reach the {@link ClusterProvider} lock and the
 * executors, and never begins a transaction on it; the drainer opens
 * the ones it needs.
 */
public class OutboxDrainerRunner implements TaskRunner {

    private static final Logger log = Logger.getLogger(OutboxDrainerRunner.class);

    private final KeycloakSessionFactory sessionFactory;
    private final OutboxDrainerTask task;
    private final int intervalSecs;

    /**
     * @param intervalMillis the schedule interval; doubles as the
     *                       cluster lock timeout, like
     *                       {@code ClusterAwareScheduledTaskRunner}.
     */
    public OutboxDrainerRunner(KeycloakSessionFactory sessionFactory, OutboxDrainerTask task, long intervalMillis) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory");
        this.task = Objects.requireNonNull(task, "task");
        this.intervalSecs = (int) (intervalMillis / 1000);
    }

    @Override
    public void run() {
        TracingProvider tracing = sessionFactory.getProviderFactory(TracingProvider.class).create(null);
        try {
            tracing.trace(OutboxDrainerRunner.class, "run", span -> {
                try (KeycloakSession session = sessionFactory.create()) {
                    runTick(session);
                }
            });
        } catch (Throwable t) {
            log.errorf(t, "Failed to run outbox drainer %s", task.getTaskName());
        } finally {
            tracing.close();
            MappedDiagnosticContextUtil.clearMdc();
        }
    }

    /**
     * Takes the cluster lock and runs the tick; {@code session} has no
     * transaction. Skips the tick when another node holds the lock.
     */
    protected void runTick(KeycloakSession session) {
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        String taskKey = task.getTaskName();
        ExecutionResult<Void> result = cluster.executeIfNotExecuted(taskKey, intervalSecs, () -> {
            task.run(session);
            return null;
        });
        if (result.isExecuted()) {
            log.debugf("Executed outbox drainer %s", taskKey);
        } else {
            log.debugf("Skipped outbox drainer %s as another cluster node is executing it", taskKey);
        }
    }

    @Override
    public ScheduledTask getTask() {
        return task;
    }
}
