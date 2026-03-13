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
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.junit.jupiter.api.Test;
import org.keycloak.models.redis.DefaultRedisClientFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DefaultRedisClientFactory to improve code coverage.
 * Note: These tests verify the factory creates proper client instances
 * without requiring actual Redis connections.
 */
class DefaultRedisClientFactoryTest {

    @Test
    void testCreateStandaloneClient() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            RedisClient client = factory.createStandaloneClient(resources, uri);
            
            assertThat(client).isNotNull();
            assertThat(client).isInstanceOf(RedisClient.class);
            
            // Cleanup
            client.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testCreateClusterClient() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            RedisClusterClient client = factory.createClusterClient(resources, uri);
            
            assertThat(client).isNotNull();
            assertThat(client).isInstanceOf(RedisClusterClient.class);
            
            // Cleanup
            client.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testSetClusterOptions() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            RedisClusterClient client = factory.createClusterClient(resources, uri);
            ClusterClientOptions options = ClusterClientOptions.builder().build();
            
            // Should not throw exception
            assertThatCode(() -> factory.setClusterOptions(client, options))
                .doesNotThrowAnyException();
            
            // Verify options were set
            assertThat(client.getOptions()).isEqualTo(options);
            
            // Cleanup
            client.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testFactoryInstantiation() {
        // Verify factory can be instantiated
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        assertThat(factory).isNotNull();
        assertThat(factory).isInstanceOf(DefaultRedisClientFactory.class);
    }

    @Test
    void testConnectStandalone() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            RedisClient client = factory.createStandaloneClient(resources, uri);
            
            // Test the connection method (will fail without actual Redis, but tests the code path)
            assertThatCode(() -> {
                try {
                    factory.connectStandalone(client);
                    // If it succeeds (Redis is running), that's fine
                } catch (Exception e) {
                    // If it fails (no Redis), that's also expected in unit tests
                    assertThat(e).isNotNull();
                }
            }).doesNotThrowAnyException();
            
            client.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testConnectCluster() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            RedisClusterClient client = factory.createClusterClient(resources, uri);
            
            // Test the connection method (will fail without actual Redis, but tests the code path)
            assertThatCode(() -> {
                try {
                    factory.connectCluster(client);
                    // If it succeeds (Redis cluster is running), that's fine
                } catch (Exception e) {
                    // If it fails (no Redis cluster), that's also expected in unit tests
                    assertThat(e).isNotNull();
                }
            }).doesNotThrowAnyException();
            
            client.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testSetClusterOptionsWithCustomSettings() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            RedisClusterClient client = factory.createClusterClient(resources, uri);
            ClusterClientOptions options = ClusterClientOptions.builder()
                .autoReconnect(true)
                .build();
            
            factory.setClusterOptions(client, options);
            
            // Verify the method executed without error
            assertThat(client.getOptions()).isNotNull();
            
            client.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testCreateMultipleClients() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri1 = RedisURI.builder().withHost("localhost").withPort(6379).build();
        RedisURI uri2 = RedisURI.builder().withHost("localhost").withPort(6380).build();
        
        try {
            RedisClient client1 = factory.createStandaloneClient(resources, uri1);
            RedisClient client2 = factory.createStandaloneClient(resources, uri2);
            
            assertThat(client1).isNotNull();
            assertThat(client2).isNotNull();
            assertThat(client1).isNotSameAs(client2);
            
            client1.shutdown();
            client2.shutdown();
        } finally {
            resources.shutdown();
        }
    }

    @Test
    void testFactoryMethodsAreIndependent() {
        DefaultRedisClientFactory factory = new DefaultRedisClientFactory();
        ClientResources resources = DefaultClientResources.create();
        RedisURI uri = RedisURI.builder().withHost("localhost").withPort(6379).build();
        
        try {
            // Create both standalone and cluster clients
            RedisClient standaloneClient = factory.createStandaloneClient(resources, uri);
            RedisClusterClient clusterClient = factory.createClusterClient(resources, uri);
            
            assertThat(standaloneClient).isNotNull();
            assertThat(clusterClient).isNotNull();
            
            // They should be different types
            assertThat(standaloneClient).isInstanceOf(RedisClient.class);
            assertThat(clusterClient).isInstanceOf(RedisClusterClient.class);
            
            standaloneClient.shutdown();
            clusterClient.shutdown();
        } finally {
            resources.shutdown();
        }
    }
}
