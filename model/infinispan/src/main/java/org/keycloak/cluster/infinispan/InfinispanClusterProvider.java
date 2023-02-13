/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.cluster.infinispan;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.Time;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanClusterProvider implements ClusterProvider {

    protected static final Logger logger = Logger.getLogger(InfinispanClusterProvider.class);

    public static final String CLUSTER_STARTUP_TIME_KEY = "cluster-start-time";
    private static final String TASK_KEY_PREFIX = "task::";

    private final int clusterStartupTime;
    private final String myAddress;
    private final CrossDCAwareCacheFactory crossDCAwareCacheFactory;
    private final InfinispanNotificationsManager notificationsManager; // Just to extract notifications related stuff to separate class

    private final ExecutorService localExecutor;

    public InfinispanClusterProvider(int clusterStartupTime, String myAddress, CrossDCAwareCacheFactory crossDCAwareCacheFactory, InfinispanNotificationsManager notificationsManager, ExecutorService localExecutor) {
        this.myAddress = myAddress;
        this.clusterStartupTime = clusterStartupTime;
        this.crossDCAwareCacheFactory = crossDCAwareCacheFactory;
        this.notificationsManager = notificationsManager;
        this.localExecutor = localExecutor;
    }


    @Override
    public int getClusterStartupTime() {
        return clusterStartupTime;
    }


    @Override
    public void close() {
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
        TaskCallback callback = this.notificationsManager.registerTaskCallback(TASK_KEY_PREFIX + taskKey, newCallback);

        // We successfully submitted our task
        if (newCallback == callback) {
            Callable<Boolean> wrappedTask = () -> {
                boolean executed = executeIfNotExecuted(taskKey, taskTimeoutInSeconds, task).isExecuted();

                if (!executed) {
                    logger.infof("Task already in progress on other cluster node. Will wait until it's finished");
                }

                callback.getTaskCompletedLatch().await(taskTimeoutInSeconds, TimeUnit.SECONDS);
                return callback.isSuccess();
            };

            Future<Boolean> future = localExecutor.submit(wrappedTask);
            callback.setFuture(future);
        } else {
            logger.infof("Task already in progress on this cluster node. Will wait until it's finished");
        }

        return callback.getFuture();
    }


    @Override
    public void registerListener(String taskKey, ClusterListener task) {
        this.notificationsManager.registerListener(taskKey, task);
    }


    @Override
    public void notify(String taskKey, ClusterEvent event, boolean ignoreSender, DCNotify dcNotify) {
        this.notificationsManager.notify(taskKey, event, ignoreSender, dcNotify);
    }

    private LockEntry createLockEntry() {
        LockEntry lock = new LockEntry();
        lock.setNode(myAddress);
        lock.setTimestamp(Time.currentTime());
        return lock;
    }


    private boolean tryLock(String cacheKey, int taskTimeoutInSeconds) {
        LockEntry myLock = createLockEntry();

        LockEntry existingLock = InfinispanClusterProviderFactory.putIfAbsentWithRetries(crossDCAwareCacheFactory, cacheKey, myLock, taskTimeoutInSeconds);
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


    private void removeFromCache(String cacheKey) {
        // More attempts to send the message (it may fail if some node fails in the meantime)
        Retry.executeWithBackoff((int iteration) -> {

            crossDCAwareCacheFactory.getCache().remove(cacheKey);
            if (logger.isTraceEnabled()) {
                logger.tracef("Task %s removed from the cache", cacheKey);
            }

        }, 10, 10);
    }

}
