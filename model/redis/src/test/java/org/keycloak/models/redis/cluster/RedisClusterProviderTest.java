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

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ExecutionResult;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.cluster.RedisClusterEventSubscriber;
import org.keycloak.models.redis.cluster.RedisClusterProvider;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisClusterProvider.
 */
class RedisClusterProviderTest {

    private RedisConnectionProvider redis;
    private RedisClusterProvider provider;
    private Map<String, ClusterListener> sharedListeners;
    private static final int TEST_STARTUP_TIME = (int) (System.currentTimeMillis() / 1000);

    /**
     * Simple serializable test event for unit tests.
     */
    static class TestClusterEvent implements ClusterEvent, Serializable {
        private static final long serialVersionUID = 1L;
        private final String data;

        public TestClusterEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    @org.junit.jupiter.api.BeforeAll
    static void setupProtoStream() throws Exception {
        // Register test event marshaller for ProtoStream
        SerializationContext ctx = ProtobufUtil.newSerializationContext();

        // Register schemas for both test packages
        String schema = "package cluster;\n" +
                "message TestClusterEvent {\n" +
                "  optional string data = 1;\n" +
                "}\n" +
                "package test;\n" +
                "message TestClusterEvent {\n" +
                "  optional string data = 1;\n" +
                "}\n";
        ctx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString("cluster.proto", schema));

        // Register marshaller for cluster.TestClusterEvent (this test class)
        ctx.registerMarshaller(new MessageMarshaller<TestClusterEvent>() {
            @Override
            public TestClusterEvent readFrom(MessageMarshaller.ProtoStreamReader reader) throws IOException {
                String data = reader.readString("data");
                return new TestClusterEvent(data);
            }

            @Override
            public void writeTo(MessageMarshaller.ProtoStreamWriter writer, TestClusterEvent event) throws IOException {
                writer.writeString("data", event.getData());
            }

            @Override
            public Class<? extends TestClusterEvent> getJavaClass() {
                return TestClusterEvent.class;
            }

            @Override
            public String getTypeName() {
                return "cluster.TestClusterEvent";
            }
        });

        // Register marshaller for test.TestClusterEvent (RedisClusterEventSubscriberTest)
        ctx.registerMarshaller(new MessageMarshaller<org.keycloak.models.redis.cluster.RedisClusterEventSubscriberTest.TestClusterEvent>() {
            @Override
            public org.keycloak.models.redis.cluster.RedisClusterEventSubscriberTest.TestClusterEvent readFrom(MessageMarshaller.ProtoStreamReader reader) throws IOException {
                String data = reader.readString("data");
                return new org.keycloak.models.redis.cluster.RedisClusterEventSubscriberTest.TestClusterEvent(data);
            }

            @Override
            public void writeTo(MessageMarshaller.ProtoStreamWriter writer, org.keycloak.models.redis.cluster.RedisClusterEventSubscriberTest.TestClusterEvent event) throws IOException {
                writer.writeString("data", event.getData());
            }

            @Override
            public Class<? extends org.keycloak.models.redis.cluster.RedisClusterEventSubscriberTest.TestClusterEvent> getJavaClass() {
                return org.keycloak.models.redis.cluster.RedisClusterEventSubscriberTest.TestClusterEvent.class;
            }

            @Override
            public String getTypeName() {
                return "test.TestClusterEvent";
            }
        });

        // Use reflection to set the context in RedisClusterEventSubscriber
        java.lang.reflect.Field field = RedisClusterEventSubscriber.class.getDeclaredField("serializationContext");
        field.setAccessible(true);
        field.set(null, ctx);
    }

    @BeforeEach
    void setUp() {
        redis = mock(RedisConnectionProvider.class);
        sharedListeners = new ConcurrentHashMap<>();
        provider = new RedisClusterProvider(redis, NODE_ID, TEST_STARTUP_TIME, sharedListeners);
    }

    @Test
    void testGetClusterStartupTime() {
        int startupTime = provider.getClusterStartupTime();
        
        // Should return the fixed startup time passed to constructor
        assertThat(startupTime).isEqualTo(TEST_STARTUP_TIME);
    }

    @Test
    void testClose() {
        ClusterListener listener = mock(ClusterListener.class);
        provider.registerListener(TASK_NAME_1, listener);

        provider.close();

        // Listeners should NOT be cleared since the map is shared across all provider instances
        // Only the factory should clear listeners on shutdown
        assertThat(provider.getListeners()).isNotEmpty();
        assertThat(provider.getListeners()).containsKey(TASK_NAME_1);
    }

    @Test
    void testExecuteIfNotExecuted_Success() throws Exception {
        when(redis.putIfAbsent(
                eq("work"),
                eq("lock:task1"),
                anyString(),
                eq(60L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(null); // No existing lock
        
        Callable<String> task = () -> "result";
        
        ExecutionResult<String> result = provider.executeIfNotExecuted(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        assertThat(result.isExecuted()).isTrue();
        assertThat(result.getResult()).isEqualTo("result");
        verify(redis).delete("work", "lock:task1");
    }

    @Test
    void testExecuteIfNotExecuted_AlreadyExecuting() throws Exception {
        when(redis.putIfAbsent(
                eq("work"),
                eq("lock:task1"),
                anyString(),
                eq(60L),
                eq(TimeUnit.SECONDS)
        )).thenReturn("existing-lock"); // Lock already exists
        
        Callable<String> task = mock(Callable.class);
        
        ExecutionResult<String> result = provider.executeIfNotExecuted(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        assertThat(result.isExecuted()).isFalse();
        verify(task, never()).call();
        verify(redis, never()).delete(anyString(), anyString());
    }

    @Test
    void testExecuteIfNotExecuted_TaskThrowsException() {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<String> task = () -> {
            throw new RuntimeException("Task failed");
        };
        
        assertThatThrownBy(() -> provider.executeIfNotExecuted(TASK_NAME_1, LOCK_TIMEOUT, task))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error executing task task1");
        
        // Should still cleanup the lock
        verify(redis).delete("work", "lock:task1");
    }

    @Test
    void testExecuteIfNotExecutedAsync_Success() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<Boolean> task = () -> true;
        
        Future<Boolean> future = provider.executeIfNotExecutedAsync(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        Boolean result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isTrue();
    }

    @Test
    void testExecuteIfNotExecutedAsync_NotExecuted() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn("existing-lock");
        
        Callable<Boolean> task = () -> true;
        
        Future<Boolean> future = provider.executeIfNotExecutedAsync(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        Boolean result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isFalse();
    }

    @Test
    void testExecuteIfNotExecutedAsync_TaskFails() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<Boolean> task = () -> {
            throw new RuntimeException("Task failed");
        };
        
        Future<Boolean> future = provider.executeIfNotExecutedAsync(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        Boolean result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isFalse();
    }

    @Test
    void testRegisterListener() {
        ClusterListener listener = mock(ClusterListener.class);
        
        provider.registerListener(TASK_NAME_1, listener);
        
        assertThat(provider.getListeners()).containsKey(TASK_NAME_1);
        assertThat(provider.getListeners().get(TASK_NAME_1)).isEqualTo(listener);
    }

    @Test
    void testRegisterMultipleListeners() {
        ClusterListener listener1 = mock(ClusterListener.class);
        ClusterListener listener2 = mock(ClusterListener.class);
        
        provider.registerListener(TASK_NAME_1, listener1);
        provider.registerListener(TASK_NAME_2, listener2);
        
        assertThat(provider.getListeners()).hasSize(2);
        assertThat(provider.getListeners().get(TASK_NAME_1)).isEqualTo(listener1);
        assertThat(provider.getListeners().get(TASK_NAME_2)).isEqualTo(listener2);
    }

    @Test
    void testNotify_LocalListenerNotIgnored() {
        ClusterListener listener = mock(ClusterListener.class);
        ClusterEvent event = new TestClusterEvent("test-data");

        provider.registerListener(TASK_NAME_1, listener);
        provider.notify(TASK_NAME_1, event, false, null);

        verify(listener).eventReceived(event);
        verify(redis).publish(eq("kc:cluster:task1"), anyString());
    }

    @Test
    void testNotify_LocalListenerIgnored() {
        ClusterListener listener = mock(ClusterListener.class);
        ClusterEvent event = new TestClusterEvent("test-data");

        provider.registerListener(TASK_NAME_1, listener);
        provider.notify(TASK_NAME_1, event, true, null);

        verify(listener, never()).eventReceived(event);
        verify(redis).publish(eq("kc:cluster:task1"), anyString());
    }

    @Test
    void testNotify_NoListener() {
        ClusterEvent event = new TestClusterEvent("test-data");

        provider.notify(TASK_NAME_1, event, false, null);

        verify(redis).publish(eq("kc:cluster:task1"), anyString());
    }

    @Test
    void testNotify_ListenerThrowsException() {
        ClusterListener listener = mock(ClusterListener.class);
        ClusterEvent event = new TestClusterEvent("test-data");
        doThrow(new RuntimeException(ERROR_LISTENER_FAILED)).when(listener).eventReceived(event);

        provider.registerListener(TASK_NAME_1, listener);

        // Should not throw, just log warning
        provider.notify(TASK_NAME_1, event, false, null);

        verify(redis).publish(eq("kc:cluster:task1"), anyString());
    }

    @Test
    void testNotify_PublishFails() {
        ClusterListener listener = mock(ClusterListener.class);
        ClusterEvent event = new TestClusterEvent("test-data");
        doThrow(new RuntimeException(ERROR_PUBLISH_FAILED)).when(redis).publish(anyString(), anyString());
        
        provider.registerListener(TASK_NAME_1, listener);
        
        // Should not throw, just log warning
        provider.notify(TASK_NAME_1, event, false, null);
        
        verify(listener).eventReceived(event);
    }

    @Test
    void testGetListeners() {
        ClusterListener listener1 = mock(ClusterListener.class);
        ClusterListener listener2 = mock(ClusterListener.class);
        
        provider.registerListener(TASK_NAME_1, listener1);
        provider.registerListener(TASK_NAME_2, listener2);
        
        var listeners = provider.getListeners();
        
        assertThat(listeners).containsKeys(TASK_NAME_1, TASK_NAME_2);
        assertThat(listeners.get(TASK_NAME_1)).isEqualTo(listener1);
        assertThat(listeners.get(TASK_NAME_2)).isEqualTo(listener2);
    }

    @Test
    void testExecuteIfNotExecuted_WithNonBooleanResult() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<String> task = () -> "test-result";
        
        ExecutionResult<String> result = provider.executeIfNotExecuted(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        assertThat(result.isExecuted()).isTrue();
        assertThat(result.getResult()).isEqualTo("test-result");
    }

    @Test
    void testExecuteIfNotExecutedAsync_WithNonBooleanResult() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<String> task = () -> "test-result";
        
        Future<Boolean> future = provider.executeIfNotExecutedAsync(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        // Non-boolean results should return true
        Boolean result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isTrue();
    }

    @Test
    void testExecuteIfNotExecutedAsync_WithBooleanFalse() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<Boolean> task = () -> false;
        
        Future<Boolean> future = provider.executeIfNotExecutedAsync(TASK_NAME_1, LOCK_TIMEOUT, task);
        
        Boolean result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isFalse();
    }

    @Test
    void testLockKeyFormat() throws Exception {
        when(redis.putIfAbsent(anyString(), anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);
        
        Callable<String> task = () -> "result";
        provider.executeIfNotExecuted(TASK_KEY, LOCK_TIMEOUT, task);
        
        verify(redis).putIfAbsent(
                eq("work"),
                eq("lock:my-task-key"),
                anyString(),
                anyLong(),
                any(TimeUnit.class)
        );
    }
}
