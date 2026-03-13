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

import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;

import java.io.*;
import java.util.Base64;
import java.util.Map;

/**
 * Redis Pub/Sub subscriber for cluster event distribution.
 * Listens to Redis channels and dispatches events to registered cluster listeners.
 * Uses ProtoStream for serializing Keycloak cluster events.
 */
public class RedisClusterEventSubscriber implements RedisPubSubListener<String, String> {

    private static final Logger logger = Logger.getLogger(RedisClusterEventSubscriber.class);
    private static final String CHANNEL_PREFIX = "kc:cluster:";

    // ProtoStream context - will be initialized from Keycloak's marshalling infrastructure
    private static volatile SerializationContext serializationContext;

    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final Map<String, ClusterListener> listeners;
    private final String nodeId;

    /**
     * Get or initialize the ProtoStream serialization context.
     * Uses Keycloak's marshalling schemas.
     */
    private static SerializationContext getSerializationContext() {
        if (serializationContext == null) {
            synchronized (RedisClusterEventSubscriber.class) {
                if (serializationContext == null) {
                    try {
                        serializationContext = ProtobufUtil.newSerializationContext();

                        // Try to load Keycloak's marshalling schemas if available
                        try {
                            Class<?> marshallingClass = Class.forName("org.keycloak.marshalling.Marshalling");
                            java.lang.reflect.Method getSchemasMethod = marshallingClass.getMethod("getSchemas");
                            @SuppressWarnings("unchecked")
                            java.util.List<org.infinispan.protostream.SerializationContextInitializer> schemas =
                                (java.util.List<org.infinispan.protostream.SerializationContextInitializer>) getSchemasMethod.invoke(null);

                            for (org.infinispan.protostream.SerializationContextInitializer schema : schemas) {
                                schema.registerSchema(serializationContext);
                                schema.registerMarshallers(serializationContext);
                            }

                            logger.infof("Initialized ProtoStream with %d Keycloak marshalling schemas", schemas.size());
                        } catch (ClassNotFoundException e) {
                            logger.warnf("Keycloak marshalling not available, ProtoStream serialization may fail for some events");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize ProtoStream serialization context", e);
                    }
                }
            }
        }
        return serializationContext;
    }

    public RedisClusterEventSubscriber(StatefulRedisPubSubConnection<String, String> pubSubConnection,
                                        Map<String, ClusterListener> listeners,
                                        String nodeId) {
        this.pubSubConnection = pubSubConnection;
        this.listeners = listeners;
        this.nodeId = nodeId;

        // Initialize ProtoStream
        getSerializationContext();

        // Register this as a listener
        pubSubConnection.addListener(this);
    }

    /**
     * Subscribe to cluster event channels with pattern matching.
     */
    public void subscribeToClusterEvents() {
        try {
            RedisPubSubCommands<String, String> sync = pubSubConnection.sync();
            sync.psubscribe(CHANNEL_PREFIX + "*");
            logger.infof("Redis cluster event subscriber started for node %s, subscribed to %s*",
                    nodeId, CHANNEL_PREFIX);
        } catch (Exception e) {
            logger.errorf(e, "Failed to subscribe to cluster event channels");
        }
    }

    @Override
    public void message(String channel, String message) {
        // Regular message (not used in our pattern subscription)
        handleMessage(channel, message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        // Pattern message - this is what we receive
        handleMessage(channel, message);
    }

    @Override
    public void subscribed(String channel, long count) {
        logger.debugf("Subscribed to channel: %s (count: %d)", channel, count);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        logger.debugf("Pattern subscribed: %s (count: %d)", pattern, count);
    }

    @Override
    public void unsubscribed(String channel, long count) {
        logger.debugf("Unsubscribed from channel: %s (count: %d)", channel, count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        logger.debugf("Pattern unsubscribed: %s (count: %d)", pattern, count);
    }

    /**
     * Handle incoming cluster event message.
     */
    private void handleMessage(String channel, String message) {
        try {
            // Extract task key from channel: "kc:cluster:{taskKey}"
            if (!channel.startsWith(CHANNEL_PREFIX)) {
                return;
            }

            String taskKey = channel.substring(CHANNEL_PREFIX.length());

            // Get registered listener for this task
            ClusterListener listener = listeners.get(taskKey);
            if (listener == null) {
                logger.debugf("No listener registered for task key: %s", taskKey);
                return;
            }

            // Deserialize event
            ClusterEventWrapper wrapper = deserializeWrapper(message);
            if (wrapper == null) {
                logger.warnf("Failed to deserialize cluster event from channel %s", channel);
                return;
            }

            // Don't process events from same node (sender already handled it locally)
            if (nodeId.equals(wrapper.getSenderId())) {
                logger.debugf("Ignoring event from same node %s", nodeId);
                return;
            }

            // Dispatch to listener
            ClusterEvent event = wrapper.getEvent();
            if (event != null) {
                listener.eventReceived(event);
                logger.debugf("Dispatched cluster event %s from node %s to listener for task %s",
                        event.getClass().getSimpleName(), wrapper.getSenderId(), taskKey);
            }

        } catch (Exception e) {
            logger.warnf(e, "Failed to handle cluster event from channel %s", channel);
        }
    }

    /**
     * Serialize wrapper. The wrapper contains sender ID and the ProtoStream-serialized event.
     */
    static String serializeWrapper(ClusterEventWrapper wrapper) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Write sender ID
            dos.writeUTF(wrapper.getSenderId());

            ClusterEvent event = wrapper.getEvent();
            if (event == null) {
                dos.writeUTF(""); // Empty class name = null event
                dos.writeInt(0); // 0 length = null event
                dos.flush();
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }

            // Write event class name for deserialization
            dos.writeUTF(event.getClass().getName());

            // Serialize event with ProtoStream
            SerializationContext ctx = getSerializationContext();
            byte[] eventBytes = ProtobufUtil.toByteArray(ctx, event);

            // Write event bytes length and data
            dos.writeInt(eventBytes.length);
            dos.write(eventBytes);
            dos.flush();

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize cluster event wrapper", e);
        }
    }

    /**
     * Deserialize wrapper from Base64-encoded ProtoStream data.
     */
    private ClusterEventWrapper deserializeWrapper(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 DataInputStream dis = new DataInputStream(bais)) {

                // Read sender ID
                String senderId = dis.readUTF();

                // Read event class name
                String eventClassName = dis.readUTF();

                // Read event bytes length
                int eventBytesLength = dis.readInt();
                if (eventBytesLength == 0 || eventClassName.isEmpty()) {
                    return new ClusterEventWrapper(senderId, null);
                }

                // Read and deserialize event
                byte[] eventBytes = new byte[eventBytesLength];
                dis.readFully(eventBytes);

                SerializationContext ctx = getSerializationContext();

                // Load the event class and deserialize with the concrete type
                Class<?> eventClass = Class.forName(eventClassName);
                if (!ClusterEvent.class.isAssignableFrom(eventClass)) {
                    logger.warnf("Class %s is not a ClusterEvent", eventClassName);
                    return null;
                }

                @SuppressWarnings("unchecked")
                ClusterEvent event = ProtobufUtil.fromByteArray(ctx, eventBytes, (Class<? extends ClusterEvent>) eventClass);

                return new ClusterEventWrapper(senderId, event);
            }
        } catch (Exception e) {
            logger.warnf(e, "Failed to deserialize cluster event wrapper");
            return null;
        }
    }

    /**
     * Unsubscribe and close connection.
     */
    public void close() {
        try {
            if (pubSubConnection != null && pubSubConnection.isOpen()) {
                pubSubConnection.sync().punsubscribe(CHANNEL_PREFIX + "*");
                logger.infof("Redis cluster event subscriber stopped for node %s", nodeId);
            }
        } catch (Exception e) {
            logger.warnf(e, "Error while closing cluster event subscriber");
        }
    }

    /**
     * Wrapper class for cluster events with sender information.
     */
    public static class ClusterEventWrapper {
        private String senderId;
        private ClusterEvent event;

        public ClusterEventWrapper() {
        }

        public ClusterEventWrapper(String senderId, ClusterEvent event) {
            this.senderId = senderId;
            this.event = event;
        }

        public String getSenderId() {
            return senderId;
        }

        public void setSenderId(String senderId) {
            this.senderId = senderId;
        }

        public ClusterEvent getEvent() {
            return event;
        }

        public void setEvent(ClusterEvent event) {
            this.event = event;
        }
    }
}
