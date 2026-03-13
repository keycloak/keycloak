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

package org.keycloak.models.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating Redis connection providers using Lettuce.
 * Supports both standalone and cluster mode configurations.
 */
public class DefaultRedisConnectionProviderFactory implements RedisConnectionProviderFactory,
        ServerInfoAwareProviderFactory, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(DefaultRedisConnectionProviderFactory.class);

    public static final String PROVIDER_ID = "default";

    // Configuration keys
    private static final String CONFIG_HOST = "host";
    private static final String CONFIG_PORT = "port";
    private static final String CONFIG_PASSWORD = "password";
    private static final String CONFIG_DATABASE = "database";
    private static final String CONFIG_SSL = "ssl";
    private static final String CONFIG_SSL_VERIFY_PEER = "sslVerifyPeer";
    private static final String CONFIG_CLUSTER = "cluster";
    private static final String CONFIG_TIMEOUT = "timeout";
    private static final String CONFIG_KEY_PREFIX = "keyPrefix";
    private static final String CONFIG_CLUSTER_TOPOLOGY_REFRESH = "clusterTopologyRefresh";
    private static final String CONFIG_CLUSTER_ADAPTIVE_REFRESH = "clusterAdaptiveRefresh";
    private static final String CONFIG_CLUSTER_REFRESH_INTERVAL = "clusterRefreshInterval";
    private static final String CONFIG_VALIDATE_CLUSTER_MEMBERSHIP = "validateClusterMembership";
    private static final String CONFIG_IO_THREADS = "ioThreads";
    private static final String CONFIG_COMPUTE_THREADS = "computeThreads";
    private static final String CONFIG_CONNECTION_RETRIES = "connectionRetries";
    private static final String CONFIG_RETRY_DELAY_MS = "retryDelayMs";
    
    // Additional tuning options
    private static final String CONFIG_CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String CONFIG_SOCKET_TIMEOUT = "socketTimeout";
    private static final String CONFIG_HEALTH_CHECK_INTERVAL = "healthCheckInterval";
    private static final String CONFIG_AUTO_RECONNECT = "autoReconnect";

    // Default values
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;
    private static final boolean DEFAULT_SSL_VERIFY_PEER = true;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final String DEFAULT_KEY_PREFIX = "kc:";
    private static final boolean DEFAULT_CLUSTER_TOPOLOGY_REFRESH = true;
    private static final boolean DEFAULT_CLUSTER_ADAPTIVE_REFRESH = true;
    private static final int DEFAULT_CLUSTER_REFRESH_INTERVAL = 300; // 5 minutes
    private static final boolean DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP = true;
    private static final int DEFAULT_IO_THREADS = 4;
    private static final int DEFAULT_COMPUTE_THREADS = 4;
    private static final int DEFAULT_CONNECTION_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 1000;
    
    // Additional tuning defaults
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    private static final int DEFAULT_HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
    private static final boolean DEFAULT_AUTO_RECONNECT = true;
    
    // Type constants
    private static final String TYPE_BOOLEAN = "boolean";

    private Config.Scope config;
    private volatile ClientResources clientResources;
    private volatile RedisClient redisClient;
    private volatile RedisClusterClient redisClusterClient;
    private volatile StatefulRedisConnection<String, String> connection;
    private volatile StatefulRedisClusterConnection<String, String> clusterConnection;
    private volatile DefaultRedisConnectionProvider connectionProvider;
    private volatile boolean clusterMode;
    private volatile String connectionInfo;
    private RedisClientFactory clientFactory = new DefaultRedisClientFactory();
    private RedisHealthCheckScheduler healthCheckScheduler;


    void setClientFactory(RedisClientFactory factory) {
        this.clientFactory = factory;
    }

    @Override
    public RedisConnectionProvider create(KeycloakSession session) {
        lazyInit();
        return connectionProvider;
    }

    private synchronized void lazyInit() {
        if (connectionProvider != null) {
            return;
        }

        int maxRetries = config.getInt(CONFIG_CONNECTION_RETRIES, DEFAULT_CONNECTION_RETRIES);
        int retryDelayMs = config.getInt(CONFIG_RETRY_DELAY_MS, DEFAULT_RETRY_DELAY_MS);

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                attemptConnection();
                logger.infof("Redis connection initialized successfully on attempt %d/%d", attempt, maxRetries);
                return; // Success!
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warnf("Redis connection attempt %d/%d failed: %s. Retrying in %dms...",
                            attempt, maxRetries, e.getMessage(), retryDelayMs);
                    waitWithMonitorLockRelease(retryDelayMs);
                } else {
                    logger.errorf(e, "Failed to initialize Redis connection after %d attempts", maxRetries);
                }
            }
        }
        
        // All retries failed
        throw new RedisConnectionException("Failed to initialize Redis connection after " + maxRetries + " attempts", lastException);
    }

    /**
     * Wait for specified duration while properly releasing monitor lock.
     * Uses wait() in a while loop to handle spurious wakeups correctly.
     *
     * @param delayMs Duration to wait in milliseconds
     * @throws RedisConnectionException if interrupted
     */
    private synchronized void waitWithMonitorLockRelease(int delayMs) {
        try {
            long startTime = System.currentTimeMillis();
            long remainingWait = delayMs;
            
            while (remainingWait > 0) {
                wait(remainingWait);
                long elapsed = System.currentTimeMillis() - startTime;
                remainingWait = delayMs - elapsed;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RedisConnectionException("Redis initialization interrupted", ie);
        }
    }

    private void attemptConnection() {
        String host = config.get(CONFIG_HOST, DEFAULT_HOST);
        int port = config.getInt(CONFIG_PORT, DEFAULT_PORT);
        String password = config.get(CONFIG_PASSWORD);
        int database = config.getInt(CONFIG_DATABASE, DEFAULT_DATABASE);
        boolean ssl = config.getBoolean(CONFIG_SSL, false);
        boolean sslVerifyPeer = config.getBoolean(CONFIG_SSL_VERIFY_PEER, DEFAULT_SSL_VERIFY_PEER);
        clusterMode = config.getBoolean(CONFIG_CLUSTER, false);
        String keyPrefix = config.get(CONFIG_KEY_PREFIX, DEFAULT_KEY_PREFIX);
        int ioThreads = config.getInt(CONFIG_IO_THREADS, DEFAULT_IO_THREADS);
        int computeThreads = config.getInt(CONFIG_COMPUTE_THREADS, DEFAULT_COMPUTE_THREADS);

        // Load additional tuning options
        int connTimeout = config.getInt(CONFIG_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        int sockTimeout = config.getInt(CONFIG_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
        
        logger.infof("Initializing Redis connection: host=%s, port=%d, cluster=%s, ssl=%s, sslVerifyPeer=%s, ioThreads=%d, computeThreads=%d, connTimeout=%dms, sockTimeout=%dms",
                host, port, clusterMode, ssl, sslVerifyPeer, ioThreads, computeThreads, connTimeout, sockTimeout);

        // Create client resources (thread pool, etc.)
        clientResources = DefaultClientResources.builder()
                .ioThreadPoolSize(ioThreads)
                .computationThreadPoolSize(computeThreads)
                .build();

        // Build Redis URI with timeout configuration
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(database)
                .withSsl(ssl)
                .withTimeout(Duration.ofMillis(connTimeout)); // Connection timeout

        // Disable SSL peer verification if configured (for CNAME-based connections)
        if (ssl && !sslVerifyPeer) {
            uriBuilder.withVerifyPeer(false);
        }

        if (password != null && !password.isEmpty()) {
            uriBuilder.withPassword(password.toCharArray());
        }

        RedisURI redisURI = uriBuilder.build();
        connectionInfo = String.format("redis%s://%s:%d/%d", ssl ? "s" : "", host, port, database);

        if (clusterMode) {
            initClusterMode(redisURI, keyPrefix);
        } else {
            initStandaloneMode(redisURI, keyPrefix);
        }
    }

    private void initStandaloneMode(RedisURI redisURI, String keyPrefix) {
        redisClient = clientFactory.createStandaloneClient(clientResources, redisURI);
        connection = clientFactory.connectStandalone(redisClient);
        connectionProvider = new DefaultRedisConnectionProvider(redisClient, connection, keyPrefix, connectionInfo);
        
        // Start automated health check if enabled
        startHealthCheckScheduler();
    }

    private void initClusterMode(RedisURI redisURI, String keyPrefix) {
        // Configure cluster topology refresh
        boolean enableTopologyRefresh = config.getBoolean(CONFIG_CLUSTER_TOPOLOGY_REFRESH, DEFAULT_CLUSTER_TOPOLOGY_REFRESH);
        boolean enableAdaptiveRefresh = config.getBoolean(CONFIG_CLUSTER_ADAPTIVE_REFRESH, DEFAULT_CLUSTER_ADAPTIVE_REFRESH);
        int refreshIntervalSeconds = config.getInt(CONFIG_CLUSTER_REFRESH_INTERVAL, DEFAULT_CLUSTER_REFRESH_INTERVAL);
        boolean validateMembership = config.getBoolean(CONFIG_VALIDATE_CLUSTER_MEMBERSHIP, DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP);
        
        // Build topology refresh options
        ClusterTopologyRefreshOptions.Builder refreshBuilder = ClusterTopologyRefreshOptions.builder();
        
        if (enableTopologyRefresh) {
            refreshBuilder.enablePeriodicRefresh(Duration.ofSeconds(refreshIntervalSeconds));
        }
        
        if (enableAdaptiveRefresh) {
            refreshBuilder
                .enableAllAdaptiveRefreshTriggers()
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30));
        }
        
        ClusterTopologyRefreshOptions refreshOptions = refreshBuilder.build();
        
        // Build cluster client options
        ClusterClientOptions options = ClusterClientOptions.builder()
            .topologyRefreshOptions(refreshOptions)
            .validateClusterNodeMembership(validateMembership)
            .build();
        
        // Create and configure cluster client
        redisClusterClient = clientFactory.createClusterClient(clientResources, redisURI);
        clientFactory.setClusterOptions(redisClusterClient, options);
        clusterConnection = clientFactory.connectCluster(redisClusterClient);
        
        logger.infof("Redis Cluster initialized: topologyRefresh=%s, adaptiveRefresh=%s, validateMembership=%s",
            enableTopologyRefresh, enableAdaptiveRefresh, validateMembership);
        
        connectionProvider = new DefaultRedisConnectionProvider(redisClusterClient, clusterConnection, keyPrefix, connectionInfo);
        
        // Start automated health check if enabled
        startHealthCheckScheduler();
    }

    /**
     * Start automated health check scheduler if enabled in configuration.
     */
    private void startHealthCheckScheduler() {
        boolean autoReconnect = config.getBoolean(CONFIG_AUTO_RECONNECT, DEFAULT_AUTO_RECONNECT);
        int healthCheckInterval = config.getInt(CONFIG_HEALTH_CHECK_INTERVAL, DEFAULT_HEALTH_CHECK_INTERVAL);
        
        if (autoReconnect && healthCheckInterval > 0) {
            healthCheckScheduler = new RedisHealthCheckScheduler();
            healthCheckScheduler.start(connectionProvider, healthCheckInterval);
            logger.infof("Redis health check scheduler started (interval=%dms)", healthCheckInterval);
        } else {
            logger.infof("Redis health check scheduler disabled (autoReconnect=%s)", autoReconnect);
        }
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
        logger.info("DefaultRedisConnectionProviderFactory initialized");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        logger.info("Redis connection provider initialized");
    }

    @Override
    public void close() {
        logger.info("Closing Redis connections");

        // Stop health check scheduler first
        if (healthCheckScheduler != null) {
            try {
                healthCheckScheduler.stop();
            } catch (Exception e) {
                logger.warn("Error stopping health check scheduler", e);
            }
            healthCheckScheduler = null;
        }

        // Close all Pub/Sub connections in the shared provider
        if (connectionProvider != null) {
            try {
                connectionProvider.closeAllConnections();
            } catch (Exception e) {
                logger.warn("Error closing Pub/Sub connections", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                logger.warn("Error closing Redis connection", e);
            }
            connection = null;
        }

        if (clusterConnection != null) {
            try {
                clusterConnection.close();
            } catch (Exception e) {
                logger.warn("Error closing Redis cluster connection", e);
            }
            clusterConnection = null;
        }

        if (redisClient != null) {
            try {
                redisClient.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down Redis client", e);
            }
            redisClient = null;
        }

        if (redisClusterClient != null) {
            try {
                redisClusterClient.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down Redis cluster client", e);
            }
            redisClusterClient = null;
        }

        if (clientResources != null) {
            try {
                clientResources.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down client resources", e);
            }
            clientResources = null;
        }

        connectionProvider = null;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(CONFIG_HOST)
                    .type("string")
                    .label("Redis Host")
                    .helpText("Redis server hostname or IP address")
                    .defaultValue(DEFAULT_HOST)
                    .add()
                .property()
                    .name(CONFIG_PORT)
                    .type("int")
                    .label("Redis Port")
                    .helpText("Redis server port")
                    .defaultValue(String.valueOf(DEFAULT_PORT))
                    .add()
                .property()
                    .name(CONFIG_PASSWORD)
                    .type("password")
                    .label("Redis Password")
                    .helpText("Redis server password (optional)")
                    .add()
                .property()
                    .name(CONFIG_DATABASE)
                    .type("int")
                    .label("Redis Database")
                    .helpText("Redis database number (0-15)")
                    .defaultValue(String.valueOf(DEFAULT_DATABASE))
                    .add()
                .property()
                    .name(CONFIG_SSL)
                    .type(TYPE_BOOLEAN)
                    .label("SSL/TLS")
                    .helpText("Enable SSL/TLS for Redis connection")
                    .defaultValue("false")
                    .add()
                .property()
                    .name(CONFIG_SSL_VERIFY_PEER)
                    .type(TYPE_BOOLEAN)
                    .label("SSL Verify Peer")
                    .helpText("Verify SSL/TLS peer certificate and hostname. Set to false when using CNAME-based connections to AWS ElastiCache where the certificate hostname doesn't match the CNAME. Default: true (recommended for security)")
                    .defaultValue(String.valueOf(DEFAULT_SSL_VERIFY_PEER))
                    .add()
                .property()
                    .name(CONFIG_CLUSTER)
                    .type(TYPE_BOOLEAN)
                    .label("Cluster Mode")
                    .helpText("Enable Redis Cluster mode")
                    .defaultValue("false")
                    .add()
                .property()
                    .name(CONFIG_TIMEOUT)
                    .type("int")
                    .label("Connection Timeout")
                    .helpText("Connection timeout in milliseconds")
                    .defaultValue(String.valueOf(DEFAULT_TIMEOUT))
                    .add()
                .property()
                    .name(CONFIG_KEY_PREFIX)
                    .type("string")
                    .label("Key Prefix")
                    .helpText("Prefix for all Redis keys")
                    .defaultValue(DEFAULT_KEY_PREFIX)
                    .add()
                .property()
                    .name(CONFIG_CLUSTER_TOPOLOGY_REFRESH)
                    .type(TYPE_BOOLEAN)
                    .label("Enable Cluster Topology Refresh")
                    .helpText("Periodically refresh cluster topology to detect node changes")
                    .defaultValue(String.valueOf(DEFAULT_CLUSTER_TOPOLOGY_REFRESH))
                    .add()
                .property()
                    .name(CONFIG_CLUSTER_ADAPTIVE_REFRESH)
                    .type(TYPE_BOOLEAN)
                    .label("Enable Adaptive Topology Refresh")
                    .helpText("Refresh topology on MOVED/ASK redirects and connection issues")
                    .defaultValue(String.valueOf(DEFAULT_CLUSTER_ADAPTIVE_REFRESH))
                    .add()
                .property()
                    .name(CONFIG_CLUSTER_REFRESH_INTERVAL)
                    .type("int")
                    .label("Topology Refresh Interval (seconds)")
                    .helpText("How often to refresh cluster topology (when periodic refresh is enabled)")
                    .defaultValue(String.valueOf(DEFAULT_CLUSTER_REFRESH_INTERVAL))
                    .add()
                .property()
                    .name(CONFIG_VALIDATE_CLUSTER_MEMBERSHIP)
                    .type(TYPE_BOOLEAN)
                    .label("Validate Cluster Membership")
                    .helpText("Validate that nodes are part of the cluster (set false for SSH tunnels)")
                    .defaultValue(String.valueOf(DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP))
                    .add()
                .property()
                    .name(CONFIG_IO_THREADS)
                    .type("int")
                    .label("I/O Thread Pool Size")
                    .helpText("Number of threads for I/O operations (network communication)")
                    .defaultValue(String.valueOf(DEFAULT_IO_THREADS))
                    .add()
                .property()
                    .name(CONFIG_COMPUTE_THREADS)
                    .type("int")
                    .label("Computation Thread Pool Size")
                    .helpText("Number of threads for computation operations (serialization, etc.)")
                    .defaultValue(String.valueOf(DEFAULT_COMPUTE_THREADS))
                    .add()
                .property()
                    .name(CONFIG_CONNECTION_RETRIES)
                    .type("int")
                    .label("Connection Retry Attempts")
                    .helpText("Number of times to retry connection on failure (1-10)")
                    .defaultValue(String.valueOf(DEFAULT_CONNECTION_RETRIES))
                    .add()
                .property()
                    .name(CONFIG_RETRY_DELAY_MS)
                    .type("int")
                    .label("Retry Delay (milliseconds)")
                    .helpText("Delay between connection retry attempts")
                    .defaultValue(String.valueOf(DEFAULT_RETRY_DELAY_MS))
                    .add()
                .build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("provider", PROVIDER_ID);

        if (connectionProvider != null) {
            info.put("connected", String.valueOf(connectionProvider.isHealthy()));
            info.put("clusterMode", String.valueOf(clusterMode));
            info.put("connectionInfo", connectionInfo);

            // Expose metrics for monitoring and observability
            if (connectionProvider instanceof DefaultRedisConnectionProvider) {
                DefaultRedisConnectionProvider provider = (DefaultRedisConnectionProvider) connectionProvider;
                Map<String, Long> metrics = provider.getMetrics();

                // Add metrics to operational info
                metrics.forEach((key, value) -> info.put("metrics." + key, String.valueOf(value)));

                logger.debugf("Operational metrics: gets=%d, puts=%d, deletes=%d, errors=%d, hitRate=%d%%",
                        metrics.get("operations.get"),
                        metrics.get("operations.put"),
                        metrics.get("operations.delete"),
                        metrics.get("errors.total"),
                        metrics.get("cache.hitRate"));
            }
        } else {
            info.put("connected", "false");
            info.put("status", "not initialized");
        }

        return info;
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
