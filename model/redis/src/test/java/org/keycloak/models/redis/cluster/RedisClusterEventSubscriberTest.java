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

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.models.redis.cluster.RedisClusterEventSubscriber;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.keycloak.models.redis.TestConstants.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for RedisClusterEventSubscriber.
 */
class RedisClusterEventSubscriberTest {

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private RedisPubSubCommands<String, String> sync;
    private Map<String, ClusterListener> listeners;
    private RedisClusterEventSubscriber subscriber;

    @org.junit.jupiter.api.BeforeAll
    static void setupProtoStream() throws Exception {
        // Register test event marshaller for ProtoStream
        SerializationContext ctx = ProtobufUtil.newSerializationContext();

        // Register schemas for both test packages
        String schema = "package test;\n" +
                "message TestClusterEvent {\n" +
                "  optional string data = 1;\n" +
                "}\n" +
                "package cluster;\n" +
                "message TestClusterEvent {\n" +
                "  optional string data = 1;\n" +
                "}\n";
        ctx.registerProtoFiles(org.infinispan.protostream.FileDescriptorSource.fromString("test.proto", schema));

        // Register marshaller for test.TestClusterEvent (this test class)
        ctx.registerMarshaller(new MessageMarshaller<TestClusterEvent>() {
            @Override
            public TestClusterEvent readFrom(ProtoStreamReader reader) throws IOException {
                String data = reader.readString("data");
                return new TestClusterEvent(data);
            }

            @Override
            public void writeTo(ProtoStreamWriter writer, TestClusterEvent event) throws IOException {
                writer.writeString("data", event.getData());
            }

            @Override
            public Class<? extends TestClusterEvent> getJavaClass() {
                return TestClusterEvent.class;
            }

            @Override
            public String getTypeName() {
                return "test.TestClusterEvent";
            }
        });

        // Register marshaller for cluster.TestClusterEvent (RedisClusterProviderTest)
        ctx.registerMarshaller(new MessageMarshaller<org.keycloak.models.redis.cluster.RedisClusterProviderTest.TestClusterEvent>() {
            @Override
            public org.keycloak.models.redis.cluster.RedisClusterProviderTest.TestClusterEvent readFrom(ProtoStreamReader reader) throws IOException {
                String data = reader.readString("data");
                return new org.keycloak.models.redis.cluster.RedisClusterProviderTest.TestClusterEvent(data);
            }

            @Override
            public void writeTo(ProtoStreamWriter writer, org.keycloak.models.redis.cluster.RedisClusterProviderTest.TestClusterEvent event) throws IOException {
                writer.writeString("data", event.getData());
            }

            @Override
            public Class<? extends org.keycloak.models.redis.cluster.RedisClusterProviderTest.TestClusterEvent> getJavaClass() {
                return org.keycloak.models.redis.cluster.RedisClusterProviderTest.TestClusterEvent.class;
            }

            @Override
            public String getTypeName() {
                return "cluster.TestClusterEvent";
            }
        });

        // Use reflection to set the context in RedisClusterEventSubscriber
        java.lang.reflect.Field field = RedisClusterEventSubscriber.class.getDeclaredField("serializationContext");
        field.setAccessible(true);
        field.set(null, ctx);
    }

    @BeforeEach
    void setUp() {
        pubSubConnection = mock(StatefulRedisPubSubConnection.class);
        sync = mock(RedisPubSubCommands.class);
        listeners = new ConcurrentHashMap<>();

        when(pubSubConnection.sync()).thenReturn(sync);
    }

    @Test
    void testConstructor() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        assertThat(subscriber).isNotNull();
        verify(pubSubConnection).addListener(subscriber);
    }

    @Test
    void testSubscribeToClusterEvents() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        subscriber.subscribeToClusterEvents();

        verify(sync).psubscribe("kc:cluster:*");
    }

    @Test
    void testSubscribeToClusterEvents_ExceptionHandled() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);
        
        doThrow(new RuntimeException("Test exception")).when(sync).psubscribe(anyString());

        // Should not throw
        assertThatCode(() -> subscriber.subscribeToClusterEvents()).doesNotThrowAnyException();
    }

    @Test
    void testSubscribed() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should not throw
        assertThatCode(() -> subscriber.subscribed("test:channel", 1L)).doesNotThrowAnyException();
    }

    @Test
    void testPsubscribed() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should not throw
        assertThatCode(() -> subscriber.psubscribed("test:pattern", 1L)).doesNotThrowAnyException();
    }

    @Test
    void testUnsubscribed() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should not throw
        assertThatCode(() -> subscriber.unsubscribed("test:channel", 1L)).doesNotThrowAnyException();
    }

    @Test
    void testPunsubscribed() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should not throw
        assertThatCode(() -> subscriber.punsubscribed("test:pattern", 1L)).doesNotThrowAnyException();
    }


    @Test
    void testMessage_NoListenerRegistered() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        TestClusterEvent event = new TestClusterEvent("test-data");
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("other-node", event);
        String message = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        // Should not throw when no listener registered
        assertThatCode(() -> subscriber.message("kc:cluster:unknown-task", message))
                .doesNotThrowAnyException();
    }


    @Test
    void testMessage_InvalidChannel() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should not throw for non-cluster channel
        assertThatCode(() -> subscriber.message("other:channel", "test-message"))
                .doesNotThrowAnyException();
    }

    @Test
    void testMessage_InvalidData() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Should handle invalid serialized data gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", "invalid-base64-data"))
                .doesNotThrowAnyException();

        verify(listener, never()).eventReceived(any());
    }



    @Test
    void testClose_ConnectionOpen() {
        when(pubSubConnection.isOpen()).thenReturn(true);
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        subscriber.close();

        verify(sync).punsubscribe("kc:cluster:*");
    }

    @Test
    void testClose_ConnectionClosed() {
        when(pubSubConnection.isOpen()).thenReturn(false);
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should not throw
        assertThatCode(() -> subscriber.close()).doesNotThrowAnyException();
    }

    @Test
    void testClose_ExceptionHandled() {
        when(pubSubConnection.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("Test exception")).when(sync).punsubscribe(anyString());
        
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should handle exception gracefully
        assertThatCode(() -> subscriber.close()).doesNotThrowAnyException();
    }

    @Test
    void testClusterEventWrapper_GettersSetters() {
        ClusterEvent event = mock(ClusterEvent.class);
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper = 
                new RedisClusterEventSubscriber.ClusterEventWrapper();

        wrapper.setSenderId("test-node");
        wrapper.setEvent(event);

        assertThat(wrapper.getSenderId()).isEqualTo("test-node");
        assertThat(wrapper.getEvent()).isEqualTo(event);
    }

    @Test
    void testClusterEventWrapper_Constructor() {
        ClusterEvent event = mock(ClusterEvent.class);
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper = 
                new RedisClusterEventSubscriber.ClusterEventWrapper("test-node", event);

        assertThat(wrapper.getSenderId()).isEqualTo("test-node");
        assertThat(wrapper.getEvent()).isEqualTo(event);
    }

    @Test
    void testHandleMessage_WithChannelPrefix() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Test message handling with proper channel prefix
        assertThatCode(() -> subscriber.message("kc:cluster:task", "test-message"))
                .doesNotThrowAnyException();
    }

    @Test
    void testClose_WhenConnectionIsNull() {
        when(pubSubConnection.isOpen()).thenReturn(false);
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should handle gracefully
        assertThatCode(() -> subscriber.close()).doesNotThrowAnyException();
    }

    @Test
    void testSubscribeToClusterEvents_VerifyPatternSubscription() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        subscriber.subscribeToClusterEvents();

        // Verify pattern subscription was attempted
        verify(sync).psubscribe("kc:cluster:*");
    }

    @Test
    void testMessage_EmptyMessage() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should handle empty messages gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:task", ""))
                .doesNotThrowAnyException();
    }

    @Test
    void testMessage_NullMessage() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Should handle null messages gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:task", null))
                .doesNotThrowAnyException();
    }

    @Test
    void testSubscribed_WithVariousCounts() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        assertThatCode(() -> subscriber.subscribed("channel1", 0L)).doesNotThrowAnyException();
        assertThatCode(() -> subscriber.subscribed("channel2", 1L)).doesNotThrowAnyException();
        assertThatCode(() -> subscriber.subscribed("channel3", 100L)).doesNotThrowAnyException();
    }

    @Test
    void testPsubscribed_WithVariousCounts() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        assertThatCode(() -> subscriber.psubscribed("pattern1", 0L)).doesNotThrowAnyException();
        assertThatCode(() -> subscriber.psubscribed("pattern2", 1L)).doesNotThrowAnyException();
        assertThatCode(() -> subscriber.psubscribed("pattern3", 100L)).doesNotThrowAnyException();
    }

    @Test
    void testUnsubscribed_WithVariousCounts() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        assertThatCode(() -> subscriber.unsubscribed("channel1", 0L)).doesNotThrowAnyException();
        assertThatCode(() -> subscriber.unsubscribed("channel2", 1L)).doesNotThrowAnyException();
    }

    @Test
    void testPunsubscribed_WithVariousCounts() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        assertThatCode(() -> subscriber.punsubscribed("pattern1", 0L)).doesNotThrowAnyException();
        assertThatCode(() -> subscriber.punsubscribed("pattern2", 1L)).doesNotThrowAnyException();
    }

    @Test
    void testSerializeWrapper_Success() {
        TestClusterEvent event = new TestClusterEvent("test-data");
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("sender-node", event);

        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        assertThat(serialized).isNotNull();
        assertThat(serialized).isNotEmpty();
        // Base64 encoded strings contain only valid Base64 characters
        assertThat(serialized).matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    @Test
    void testMessage_PatternBased_SuccessfulDispatch() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        TestClusterEvent event = new TestClusterEvent("test-data");
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("node-2", event);
        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        System.out.println("Serialized data: " + serialized);
        System.out.println("Serialized data length: " + serialized.length());

        // Call pattern-based message method
        subscriber.message("kc:cluster:*", "kc:cluster:test-task", serialized);

        // Add a small delay to ensure async processing completes
        try { Thread.sleep(100); } catch (InterruptedException e) { }

        verify(listener).eventReceived(any(ClusterEvent.class));
    }

    @Test
    void testHandleMessage_SameNodeId_Ignored() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        TestClusterEvent event = new TestClusterEvent("test-data");
        // Use same node ID as subscriber
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper(NODE_ID, event);
        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        subscriber.message("kc:cluster:test-task", serialized);

        // Listener should NOT be called for events from same node
        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testHandleMessage_ListenerThrowsException() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        TestClusterEvent event = new TestClusterEvent("test-data");
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("node-2", event);
        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        doThrow(new RuntimeException("Listener failed")).when(listener).eventReceived(any());

        // Should handle listener exception gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", serialized))
                .doesNotThrowAnyException();
    }

    @Test
    void testHandleMessage_NullEventInWrapper() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Create wrapper with null event
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("node-2", null);
        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        subscriber.message("kc:cluster:test-task", serialized);

        // Listener should not be called when event is null
        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testDeserializeWrapper_NullData() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Call with null data
        subscriber.message("kc:cluster:test-task", null);

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testDeserializeWrapper_EmptyData() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Call with empty string
        subscriber.message("kc:cluster:test-task", "");

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testDeserializeWrapper_InvalidClassType() throws Exception {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Create serialized data with a class that's not a ClusterEvent
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);

        dos.writeUTF("node-2"); // sender ID
        dos.writeUTF("java.lang.String"); // Invalid class (not a ClusterEvent)
        dos.writeInt(10); // non-zero length
        dos.write(new byte[10]); // dummy bytes
        dos.flush();

        String serialized = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

        // Should handle gracefully - not throw and not call listener
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", serialized))
                .doesNotThrowAnyException();

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testDeserializeWrapper_ClassNotFoundException() throws Exception {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Create serialized data with a non-existent class
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);

        dos.writeUTF("node-2"); // sender ID
        dos.writeUTF("com.nonexistent.FakeClusterEvent"); // Non-existent class
        dos.writeInt(10); // non-zero length
        dos.write(new byte[10]); // dummy bytes
        dos.flush();

        String serialized = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

        // Should handle ClassNotFoundException gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", serialized))
                .doesNotThrowAnyException();

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testDeserializeWrapper_CorruptedData() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Create base64 encoded but structurally invalid data (too short)
        String serialized = java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        // Should handle corrupted data gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", serialized))
                .doesNotThrowAnyException();

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testDeserializeWrapper_ProtoStreamDeserializationFailure() throws Exception {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Create serialized data with valid structure but invalid ProtoStream bytes
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);

        dos.writeUTF("node-2"); // sender ID
        dos.writeUTF(TestClusterEvent.class.getName()); // Valid class
        dos.writeInt(5); // length
        dos.write(new byte[]{-1, -1, -1, -1, -1}); // Invalid ProtoStream bytes
        dos.flush();

        String serialized = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

        // Should handle ProtoStream deserialization failure gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", serialized))
                .doesNotThrowAnyException();

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testSerializeWrapper_WithNullEvent() {
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("sender-node", null);

        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        assertThat(serialized).isNotNull();
        assertThat(serialized).isNotEmpty();

        // Verify it can be deserialized back
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        subscriber.message("kc:cluster:test-task", serialized);

        // Should not call listener when event is null
        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testMessage_PatternMethod_InvokesHandleMessage() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        TestClusterEvent event = new TestClusterEvent("test-data");
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("node-2", event);
        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        // Use the pattern-based message method (3 args)
        subscriber.message("kc:cluster:*", "kc:cluster:test-task", serialized);

        // Should dispatch to listener
        verify(listener).eventReceived(any(ClusterEvent.class));
    }

    @Test
    void testMessage_RegularMethod_InvokesHandleMessage() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        TestClusterEvent event = new TestClusterEvent("test-data");
        RedisClusterEventSubscriber.ClusterEventWrapper wrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("node-2", event);
        String serialized = RedisClusterEventSubscriber.serializeWrapper(wrapper);

        // Use the regular message method (2 args)
        subscriber.message("kc:cluster:test-task", serialized);

        // Should dispatch to listener
        verify(listener).eventReceived(any(ClusterEvent.class));
    }

    @Test
    void testHandleMessage_ExceptionInDeserialization() {
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "node-1");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Invalid Base64 that will cause exception
        String invalidBase64 = "!!!invalid-base64!!!";

        // Should handle exception gracefully
        assertThatCode(() -> subscriber.message("kc:cluster:test-task", invalidBase64))
                .doesNotThrowAnyException();

        verify(listener, never()).eventReceived(any());
    }

    @Test
    void testClose_WithNullConnection() {
        // Create subscriber but set connection to null scenario
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, NODE_ID);

        // Mock connection returning null for isOpen
        when(pubSubConnection.isOpen()).thenReturn(false);

        // Should handle gracefully without calling punsubscribe
        assertThatCode(() -> subscriber.close()).doesNotThrowAnyException();

        verify(sync, never()).punsubscribe(anyString());
    }

    @Test
    void testSerializationRoundTrip() {
        TestClusterEvent originalEvent = new TestClusterEvent("round-trip-data");
        RedisClusterEventSubscriber.ClusterEventWrapper originalWrapper =
                new RedisClusterEventSubscriber.ClusterEventWrapper("sender-node", originalEvent);

        // Serialize
        String serialized = RedisClusterEventSubscriber.serializeWrapper(originalWrapper);

        // Setup subscriber to deserialize
        subscriber = new RedisClusterEventSubscriber(pubSubConnection, listeners, "receiver-node");
        ClusterListener listener = mock(ClusterListener.class);
        listeners.put("test-task", listener);

        // Deserialize by sending message
        subscriber.message("kc:cluster:test-task", serialized);

        // Verify the event was received
        verify(listener).eventReceived(argThat(event ->
            event instanceof TestClusterEvent &&
            "round-trip-data".equals(((TestClusterEvent) event).getData())
        ));
    }

    /**
     * Simple test event for unit tests.
     */
    static class TestClusterEvent implements ClusterEvent, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final String data;

        public TestClusterEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }
}
