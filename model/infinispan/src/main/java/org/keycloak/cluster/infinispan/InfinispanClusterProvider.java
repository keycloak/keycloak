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
import org.keycloak.common.util.Time;

import java.util.concurrent.Callable;
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

    public InfinispanClusterProvider(int clusterStartupTime, String myAddress, CrossDCAwareCacheFactory crossDCAwareCacheFactory, InfinispanNotificationsManager notificationsManager) {
        this.myAddress = myAddress;
        this.clusterStartupTime = clusterStartupTime;
        this.crossDCAwareCacheFactory = crossDCAwareCacheFactory;
        this.notificationsManager = notificationsManager;
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
    public void registerListener(String taskKey, ClusterListener task) {
        this.notificationsManager.registerListener(taskKey, task);
    }


    @Override
    public void notify(String taskKey, ClusterEvent event, boolean ignoreSender) {
        this.notificationsManager.notify(taskKey, event, ignoreSender);
    }


    private LockEntry createLockEntry() {
        LockEntry lock = new LockEntry();
        lock.setNode(myAddress);
        lock.setTimestamp(Time.currentTime());
        return lock;
    }


    private boolean tryLock(String cacheKey, int taskTimeoutInSeconds) {
        LockEntry myLock = createLockEntry();

        LockEntry existingLock = (LockEntry) crossDCAwareCacheFactory.getCache().putIfAbsent(cacheKey, myLock, taskTimeoutInSeconds, TimeUnit.SECONDS);
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
        // 3 attempts to send the message (it may fail if some node fails in the meantime)
        int retry = 3;
        while (true) {
            try {
                crossDCAwareCacheFactory.getCache().remove(cacheKey);
                if (logger.isTraceEnabled()) {
                    logger.tracef("Task %s removed from the cache", cacheKey);
                }
                return;
            } catch (RuntimeException e) {
                retry--;
                if (retry == 0) {
                    throw e;
                }
            }
        }
    }

}
