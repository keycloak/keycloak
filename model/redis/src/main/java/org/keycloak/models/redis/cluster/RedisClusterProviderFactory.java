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

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.cluster.ClusterProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Factory for RedisClusterProvider.
 */
public class RedisClusterProviderFactory implements ClusterProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(RedisClusterProviderFactory.class);

    public static final String PROVIDER_ID = "redis";

    private String nodeId;
    private int clusterStartupTime;
    private RedisClusterEventSubscriber subscriber;
    private final Map<String, ClusterListener> sharedListeners = new ConcurrentHashMap<>();

    @Override
    public ClusterProvider create(KeycloakSession session) {
        RedisConnectionProvider redis = session.getProvider(RedisConnectionProvider.class);
        if (redis == null) {
            throw new IllegalStateException("RedisConnectionProvider not available");
        }
        // Pass shared listeners map directly so all provider instances share the same listeners
        // This ensures listeners registered in any session are visible to the Pub/Sub subscriber
        return new RedisClusterProvider(redis, nodeId, clusterStartupTime, sharedListeners);
    }

    @Override
    public void init(Config.Scope config) {
        nodeId = config.get("nodeId", UUID.randomUUID().toString());
        logger.infof("Redis ClusterProviderFactory initializing with nodeId: %s", nodeId);
        // Note: clusterStartupTime will be initialized in postInit() when Redis is available
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Initialize cluster startup time from Redis (shared across all nodes)
        initializeClusterStartupTime(factory);

        // Initialize Pub/Sub subscriber for cluster event distribution
        try {
            // Create a temporary session to get RedisConnectionProvider
            KeycloakSession session = factory.create();
            try {
                RedisConnectionProvider redis = session.getProvider(RedisConnectionProvider.class);
                if (redis == null) {
                    logger.warnf("RedisConnectionProvider not available - cluster events disabled");
                    return;
                }

                // Create dedicated Pub/Sub connection
                Object pubSubConnObj = redis.createPubSubConnection();
                if (pubSubConnObj == null) {
                    logger.warnf("Failed to create Pub/Sub connection - cluster events will only work locally");
                    return;
                }

                // Cast to the proper type
                if (!(pubSubConnObj instanceof StatefulRedisPubSubConnection)) {
                    logger.warnf("Invalid Pub/Sub connection type - cluster events disabled");
                    return;
                }

                @SuppressWarnings("unchecked")
                StatefulRedisPubSubConnection<String, String> pubSubConn =
                        (StatefulRedisPubSubConnection<String, String>) pubSubConnObj;

                // Initialize subscriber with shared listeners map
                subscriber = new RedisClusterEventSubscriber(pubSubConn, sharedListeners, nodeId);

                // Subscribe to cluster event channels
                subscriber.subscribeToClusterEvents();

                logger.infof("Redis cluster Pub/Sub subscriber initialized successfully for node %s", nodeId);
                logger.infof("Multi-node cluster event distribution is now ACTIVE");

            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.errorf(e, "Failed to initialize Redis cluster Pub/Sub subscriber - cluster events will only work locally");
        }
    }

    /**
     * Initialize cluster startup time from Redis.
     * The first node to start sets the time, subsequent nodes use that value.
     * This ensures all nodes in the cluster have the same startup time for token validation.
     */
    private void initializeClusterStartupTime(KeycloakSessionFactory factory) {
        try {
            KeycloakSession session = factory.create();
            try {
                RedisConnectionProvider redis = session.getProvider(RedisConnectionProvider.class);
                if (redis == null) {
                    logger.warnf("RedisConnectionProvider not available - using local startup time");
                    clusterStartupTime = (int) (System.currentTimeMillis() / 1000);
                    return;
                }

                String startupTimeKey = "cluster:startup-time";
                Integer existingStartupTime = redis.get("work", startupTimeKey, Integer.class);

                if (existingStartupTime != null) {
                    // Use existing cluster startup time
                    clusterStartupTime = existingStartupTime;
                    logger.infof("Using existing cluster startup time: %d (node: %s)", clusterStartupTime, nodeId);
                } else {
                    // First node to start - set the cluster startup time
                    clusterStartupTime = (int) (System.currentTimeMillis() / 1000);

                    // Store in Redis with no expiration (persistent for cluster lifetime)
                    // Use putIfAbsent to handle race condition if multiple nodes start simultaneously
                    Integer previousValue = redis.putIfAbsent("work", startupTimeKey, clusterStartupTime, -1, TimeUnit.MILLISECONDS);

                    if (previousValue != null) {
                        // Another node beat us to it, use their value
                        clusterStartupTime = previousValue;
                        logger.infof("Another node set cluster startup time: %d (node: %s)", clusterStartupTime, nodeId);
                    } else {
                        logger.infof("Initialized cluster startup time: %d (first node: %s)", clusterStartupTime, nodeId);
                    }
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.errorf(e, "Failed to initialize cluster startup time from Redis - using local time");
            clusterStartupTime = (int) (System.currentTimeMillis() / 1000);
        }
    }

    @Override
    public void close() {
        if (subscriber != null) {
            try {
                subscriber.close();
                logger.infof("Redis cluster event subscriber closed");
            } catch (Exception e) {
                logger.warnf(e, "Error closing cluster event subscriber");
            }
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.REDIS_STORAGE);
    }

    @Override
    public int order() {
        return 10; // Higher order to take precedence over Infinispan when configured
    }
}
