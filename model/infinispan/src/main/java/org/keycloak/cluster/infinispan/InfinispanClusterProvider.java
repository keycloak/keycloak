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

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanClusterProvider implements ClusterProvider {

    protected static final Logger logger = Logger.getLogger(InfinispanClusterProvider.class);

    public static final String CLUSTER_STARTUP_TIME_KEY = "cluster-start-time";
    private static final String TASK_KEY_PREFIX = "task::";

    private final InfinispanClusterProviderFactory factory;
    private final KeycloakSession session;
    private final Cache<String, Serializable> cache;

    public InfinispanClusterProvider(InfinispanClusterProviderFactory factory, KeycloakSession session, Cache<String, Serializable> cache) {
        this.factory = factory;
        this.session = session;
        this.cache = cache;
    }


    @Override
    public int getClusterStartupTime() {
        Integer existingClusterStartTime = (Integer) cache.get(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY);
        if (existingClusterStartTime != null) {
            return existingClusterStartTime;
        } else {
            // clusterStartTime not yet initialized. Let's try to put our startupTime
            int serverStartTime = (int) (session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000);

            existingClusterStartTime = (Integer) cache.putIfAbsent(InfinispanClusterProvider.CLUSTER_STARTUP_TIME_KEY, serverStartTime);
            if (existingClusterStartTime == null) {
                logger.debugf("Initialized cluster startup time to %s", Time.toDate(serverStartTime).toString());
                return serverStartTime;
            } else {
                return existingClusterStartTime;
            }
        }
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
        factory.registerListener(taskKey, task);
    }


    @Override
    public void notify(String taskKey, ClusterEvent event) {
        // Put the value to the cache to notify listeners on all the nodes
        cache.put(taskKey, event);
    }


    private String getCurrentNode(Cache<String, Serializable> cache) {
        Transport transport = cache.getCacheManager().getTransport();
        return transport==null ? "local" : transport.getAddress().toString();
    }


    private LockEntry createLockEntry(Cache<String, Serializable> cache) {
        LockEntry lock = new LockEntry();
        lock.setNode(getCurrentNode(cache));
        lock.setTimestamp(Time.currentTime());
        return lock;
    }


    private boolean tryLock(String cacheKey, int taskTimeoutInSeconds) {
        LockEntry myLock = createLockEntry(cache);

        LockEntry existingLock = (LockEntry) cache.putIfAbsent(cacheKey, myLock);
        if (existingLock != null) {
            // Task likely already in progress. Check if timestamp is not outdated
            int thatTime = existingLock.getTimestamp();
            int currentTime = Time.currentTime();
            if (thatTime + taskTimeoutInSeconds < currentTime) {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Task %s outdated when in progress by node %s. Will try to replace task with our node %s", cacheKey, existingLock.getNode(), myLock.getNode());
                }
                boolean replaced = cache.replace(cacheKey, existingLock, myLock);
                if (!replaced) {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Failed to replace the task %s. Other thread replaced in the meantime. Ignoring task.", cacheKey);
                    }
                }
                return replaced;
            } else {
                if (logger.isTraceEnabled()) {
                    logger.tracef("Task %s in progress already by node %s. Ignoring task.", cacheKey, existingLock.getNode());
                }
                return false;
            }
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
                cache.getAdvancedCache()
                        .withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_SYNCHRONOUS)
                        .remove(cacheKey);
                if (logger.isTraceEnabled()) {
                    logger.tracef("Task %s removed from the cache", cacheKey);
                }
                return;
            } catch (RuntimeException e) {
                ComponentStatus status = cache.getStatus();
                if (status.isStopping() || status.isTerminated()) {
                    logger.warnf("Failed to remove task %s from the cache. Cache is already terminating", cacheKey);
                    logger.debug(e.getMessage(), e);
                    return;
                }
                retry--;
                if (retry == 0) {
                    throw e;
                }
            }
        }
    }

}
