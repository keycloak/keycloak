package org.keycloak.events.hooks;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventHookTargetProviderFactoryTest {

    @Test
    public void shouldGenerateGenericTestMessageForProviderTypes() throws Exception {
        RecordingFactory factory = new RecordingFactory();
        EventHookTargetModel target = target();

        EventHookDeliveryResult result = factory.test(null, realm("realm-1", "demo"), target);

        assertTrue(result.isSuccess());
        assertEquals(1, factory.messages.size());
        assertEquals(target.getId(), factory.messages.get(0).getTargetId());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = JsonSerialization.readValue(factory.messages.get(0).getPayload(), Map.class);
        assertEquals(Boolean.TRUE, payload.get("deliveryTest"));
        assertEquals("EVENT_HOOK_TEST", payload.get("eventType"));

        @SuppressWarnings("unchecked")
        Map<String, Object> realm = (Map<String, Object>) payload.get("realm");
        assertEquals("realm-1", realm.get("id"));
        assertEquals("demo", realm.get("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> targetPayload = (Map<String, Object>) payload.get("target");
        assertEquals(target.getId(), targetPayload.get("id"));
        assertEquals(target.getType(), targetPayload.get("type"));
    }

    @Test
    public void shouldReturnGenericFailureResultWhenProviderTestThrows() {
        RecordingFactory factory = new RecordingFactory();
        factory.failure = new IOException("boom");

        EventHookDeliveryResult result = factory.test(null, realm("realm-1", "demo"), target());

        assertFalse(result.isSuccess());
        assertFalse(result.isRetryable());
        assertEquals("TEST_ERROR", result.getStatusCode());
        assertEquals("boom", result.getDetails());
    }

    private EventHookTargetModel target() {
        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-1");
        target.setName("Primary target");
        target.setType("test");
        target.setEnabled(true);
        target.setSettings(Map.of("url", "https://example.org/hooks"));
        return target;
    }

    private RealmModel realm(String id, String name) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "getName" -> name;
                    case "toString" -> "RealmModel[" + id + "]";
                    default -> null;
                });
    }

    private static final class RecordingFactory implements EventHookTargetProviderFactory {

        private List<EventHookMessageModel> messages = List.of();
        private IOException failure;

        @Override
        public EventHookTargetProvider create(KeycloakSession session) {
            return new EventHookTargetProvider() {
                @Override
                public EventHookDeliveryResult deliver(EventHookTargetModel target, EventHookMessageModel message) throws IOException {
                    RecordingFactory.this.messages = List.of(message);
                    if (failure != null) {
                        throw failure;
                    }

                    EventHookDeliveryResult result = new EventHookDeliveryResult();
                    result.setSuccess(true);
                    result.setRetryable(false);
                    return result;
                }

                @Override
                public EventHookDeliveryResult deliverBatch(EventHookTargetModel target, List<EventHookMessageModel> messages) throws IOException {
                    RecordingFactory.this.messages = messages;
                    if (failure != null) {
                        throw failure;
                    }

                    EventHookDeliveryResult result = new EventHookDeliveryResult();
                    result.setSuccess(true);
                    result.setRetryable(false);
                    return result;
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public String getId() {
            return "test";
        }

        @Override
        public List<ProviderConfigProperty> getConfigMetadata() {
            return Collections.emptyList();
        }

        @Override
        public void validateConfig(KeycloakSession session, Map<String, Object> settings) {
        }

        @Override
        public void init(Config.Scope config) {
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
        }

        @Override
        public void close() {
        }
    }
}
