/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis.cluster;

import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.RedisSerializer;

import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ExecutionResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of ClusterProvider for distributed coordination.
 */
public class RedisClusterProvider implements ClusterProvider {

    private static final Logger logger = Logger.getLogger(RedisClusterProvider.class);

    private static final String LOCK_CACHE = "work";

    private final RedisConnectionProvider redis;
    private final RedisSerializer serializer;
    private final String nodeId;
    private final int clusterStartupTime;
    private final Map<String, ClusterListener> listeners;

    public RedisClusterProvider(RedisConnectionProvider redis, String nodeId, int clusterStartupTime,
                                 Map<String, ClusterListener> sharedListeners) {
        this.redis = redis;
        this.serializer = RedisSerializer.getInstance();
        this.nodeId = nodeId;
        this.clusterStartupTime = clusterStartupTime;
        this.listeners = sharedListeners; // Use shared map instead of creating new one
    }

    @Override
    public int getClusterStartupTime() {
        // Return the fixed startup time from when the factory was initialized,
        // NOT the current time. This is critical for token reuse validation.
        return clusterStartupTime;
    }

    @Override
    public void close() {
        // Do not clear the shared listeners map - it's shared across all sessions
        // and should only be cleared when the factory is closed
    }

    /**
     * Get the listeners map for factory access.
     * @return Map of task keys to cluster listeners
     */
    Map<String, ClusterListener> getListeners() {
        return listeners;
    }

    @Override
    public <T> ExecutionResult<T> executeIfNotExecuted(String taskKey, int taskTimeoutInSeconds, Callable<T> task) {
        String lockKey = "lock:" + taskKey;
        String lockValue = nodeId + ":" + UUID.randomUUID();

        Object existing = redis.putIfAbsent(LOCK_CACHE, lockKey, lockValue, taskTimeoutInSeconds, TimeUnit.SECONDS);

        if (existing == null) {
            try {
                T result = task.call();
                return ExecutionResult.executed(result);
            } catch (Exception e) {
                logger.warnf(e, "Error executing task %s", taskKey);
                throw new RuntimeException("Error executing task " + taskKey, e);
            } finally {
                redis.delete(LOCK_CACHE, lockKey);
            }
        } else {
            logger.debugf("Task %s already being executed by another node", taskKey);
            return ExecutionResult.notExecuted();
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Future<Boolean> executeIfNotExecutedAsync(String taskKey, int taskTimeoutInSeconds, Callable task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ExecutionResult result = executeIfNotExecuted(taskKey, taskTimeoutInSeconds, task);
                if (result.isExecuted()) {
                    Object r = result.getResult();
                    return r instanceof Boolean ? (Boolean) r : true;
                }
                return false;
            } catch (Exception e) {
                logger.warnf(e, "Async task execution failed for %s", taskKey);
                return false;
            }
        });
    }

    @Override
    public void registerListener(String taskKey, ClusterListener listener) {
        listeners.put(taskKey, listener);
        logger.debugf("Registered cluster listener for task: %s", taskKey);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void notify(String taskKey, ClusterEvent event, boolean ignoreSender, DCNotify dcNotify) {
        logger.debugf("Cluster notification for task %s: %s", taskKey, event.getClass().getSimpleName());

        // Notify local listener first if not ignoring sender
        if (!ignoreSender) {
            ClusterListener listener = listeners.get(taskKey);
            if (listener != null) {
                try {
                    listener.eventReceived(event);
                } catch (Exception e) {
                    logger.warnf(e, "Error notifying local listener for task %s", taskKey);
                }
            }
        }

        // Publish to Redis for other cluster nodes
        try {
            String channel = "kc:cluster:" + taskKey;

            // Wrap event with sender node ID to prevent echo
            RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                    new RedisClusterEventSubscriber.ClusterEventWrapper(nodeId, event);

            // Use Java serialization instead of Jackson (Keycloak events aren't Jackson-compatible)
            String serializedEvent = RedisClusterEventSubscriber.serializeWrapper(wrapper);
            redis.publish(channel, serializedEvent);

            logger.debugf("Published cluster event %s to channel %s",
                    event.getClass().getSimpleName(), channel);
        } catch (Exception e) {
            logger.warnf(e, "Failed to publish cluster event to Redis for task %s", taskKey);
        }
    }
}
