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
 * Default implementation of RedisClientFactory that creates actual Lettuce Redis clients.
 */
public class DefaultRedisClientFactory implements RedisClientFactory {
    
    @Override
    public RedisClient createStandaloneClient(ClientResources resources, RedisURI uri) {
        return RedisClient.create(resources, uri);
    }
    
    @Override
    public StatefulRedisConnection<String, String> connectStandalone(RedisClient client) {
        return client.connect();
    }
    
    @Override
    public RedisClusterClient createClusterClient(ClientResources resources, RedisURI uri) {
        return RedisClusterClient.create(resources, uri);
    }
    
    @Override
    public void setClusterOptions(RedisClusterClient client, ClusterClientOptions options) {
        client.setOptions(options);
    }
    
    @Override
    public StatefulRedisClusterConnection<String, String> connectCluster(RedisClusterClient client) {
        return client.connect();
    }
}
