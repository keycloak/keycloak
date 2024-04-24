package org.keycloak.cluster.infinispan.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.cluster.infinispan.LockEntry;
import org.keycloak.cluster.infinispan.TaskCallback;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.keycloak.cluster.infinispan.InfinispanClusterProvider.TASK_KEY_PREFIX;
import static org.keycloak.cluster.infinispan.remote.RemoteInfinispanClusterProviderFactory.putIfAbsentWithRetries;

public class RemoteInfinispanClusterProvider implements ClusterProvider {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());
    private final SharedData data;

    public RemoteInfinispanClusterProvider(SharedData data) {
        this.data = Objects.requireNonNull(data);
    }


    @Override
    public int getClusterStartupTime() {
        return data.clusterStartupTime();
    }

    @Override
    public <T> ExecutionResult<T> executeIfNotExecuted(String taskKey, int taskTimeoutInSeconds, Callable<T> task) {
        String cacheKey = TASK_KEY_PREFIX + taskKey;
        boolean locked = tryLock(cacheKey, taskTimeoutInSeconds);
        if (locked) {
            try {
                try {
                    T result = task.call();
                    return ExecutionResult.executed(result);
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected exception when executed task " + taskKey, e);
                }
            } finally {
                removeFromCache(cacheKey);
            }
        } else {
            return ExecutionResult.notExecuted();
        }
    }

    @Override
    public Future<Boolean> executeIfNotExecutedAsync(String taskKey, int taskTimeoutInSeconds, Callable task) {
        TaskCallback newCallback = new TaskCallback();
        TaskCallback callback = data.notificationManager().registerTaskCallback(TASK_KEY_PREFIX + taskKey, newCallback);

        // We successfully submitted our task
        if (newCallback == callback) {
            Supplier<Boolean> wrappedTask = () -> {
                boolean executed = executeIfNotExecuted(taskKey, taskTimeoutInSeconds, task).isExecuted();

                if (!executed) {
                    logger.infof("Task already in progress on other cluster node. Will wait until it's finished");
                }

                try {
                    callback.getTaskCompletedLatch().await(taskTimeoutInSeconds, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return callback.isSuccess();
            };

            callback.setFuture(CompletableFuture.supplyAsync(wrappedTask, data.executor()));
        } else {
            logger.infof("Task already in progress on this cluster node. Will wait until it's finished");
        }

        return callback.getFuture();
    }

    @Override
    public void registerListener(String taskKey, ClusterListener task) {
        data.notificationManager().registerListener(taskKey, task);
    }

    @Override
    public void notify(String taskKey, ClusterEvent event, boolean ignoreSender, DCNotify dcNotify) {
        data.notificationManager().notify(taskKey, event, ignoreSender, dcNotify);
    }

    @Override
    public void close() {

    }

    private boolean tryLock(String cacheKey, int taskTimeoutInSeconds) {
        LockEntry myLock = createLockEntry();

        LockEntry existingLock = putIfAbsentWithRetries(data.cache(), cacheKey, myLock, taskTimeoutInSeconds);
        if (existingLock != null) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Task %s in progress already by node %s. Ignoring task.", cacheKey, existingLock.getNode());
            }
            return false;
        } else {
            if (logger.isTraceEnabled()) {
                logger.tracef("Successfully acquired lock for task %s. Our node is %s", cacheKey, myLock.getNode());
            }
            return true;
        }
    }

    private LockEntry createLockEntry() {
        LockEntry lock = new LockEntry();
        lock.setNode(data.notificationManager().getMyNodeName());
        lock.setTimestamp(Time.currentTime());
        return lock;
    }

    private void removeFromCache(String cacheKey) {
        // More attempts to send the message (it may fail if some node fails in the meantime)
        Retry.executeWithBackoff((int iteration) -> {
            data.cache().remove(cacheKey);
            if (logger.isTraceEnabled()) {
                logger.tracef("Task %s removed from the cache", cacheKey);
            }
        }, 10, 10);
    }

    public interface SharedData {
        int clusterStartupTime();
        RemoteCache<String, LockEntry> cache();
        RemoteInfinispanNotificationManager notificationManager();
        Executor executor();
    }
}
