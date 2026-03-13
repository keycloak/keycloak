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
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * Factory interface for creating Redis clients and connections.
 * This abstraction allows for better testability by enabling mock implementations.
 */
public interface RedisClientFactory {
    
    /**
     * Create a standalone Redis client.
     * 
     * @param resources Client resources for thread pools, etc.
     * @param uri Redis URI with connection details
     * @return Redis client instance
     */
    RedisClient createStandaloneClient(ClientResources resources, RedisURI uri);
    
    /**
     * Connect to a standalone Redis server.
     * 
     * @param client Redis client
     * @return Stateful connection
     */
    StatefulRedisConnection<String, String> connectStandalone(RedisClient client);
    
    /**
     * Create a Redis cluster client.
     * 
     * @param resources Client resources for thread pools, etc.
     * @param uri Redis URI with connection details
     * @return Redis cluster client instance
     */
    RedisClusterClient createClusterClient(ClientResources resources, RedisURI uri);
    
    /**
     * Configure cluster client options.
     * 
     * @param client Cluster client to configure
     * @param options Cluster client options
     */
    void setClusterOptions(RedisClusterClient client, ClusterClientOptions options);
    
    /**
     * Connect to a Redis cluster.
     * 
     * @param client Redis cluster client
     * @return Stateful cluster connection
     */
    StatefulRedisClusterConnection<String, String> connectCluster(RedisClusterClient client);
}
