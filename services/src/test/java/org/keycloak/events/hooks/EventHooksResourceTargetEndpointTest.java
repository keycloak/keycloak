package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;

import org.junit.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.EventHooksResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventHooksResourceTargetEndpointTest {

        @Test
        public void shouldConsumeBufferedPullTestEntriesThroughDedicatedEndpoint() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers("top-secret", null);

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setRealmId("realm-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of("pullSecret", "top-secret", "deliveryMode", "SINGLE"));

                EventHookMessageModel message = new EventHookMessageModel();
                message.setId("msg-test-1");
                message.setRealmId("realm-1");
                message.setTargetId("target-1");
                message.setSourceType(EventHookSourceType.USER);
                message.setSourceEventId("event-test-1");
                message.setAttemptCount(1);
                                message.setStatus(EventHookMessageStatus.WAITING);
                                message.setTest(true);
                message.setPayload("{\"deliveryTest\":true,\"eventType\":\"LOGIN\"}");
                message.setUpdatedAt(100L);

                store.target = target;
                                store.testMessages = List.of(message);
                                store.hasMoreTestMessages = false;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "test");
                EventHookPullRepresentation representation = (EventHookPullRepresentation) ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

                assertNotNull(representation.getEvent());
                assertEquals("LOGIN", ((Map<?, ?>) representation.getEvent()).get("eventType"));
                assertNotNull(representation.getEntry());
                assertEquals(Boolean.TRUE, representation.getEntry().getTest());
                                assertEquals("PULL_TEST_CONSUMED", store.createdLog.getStatusCode());
                assertFalse(representation.isHasMoreEvents());
                                assertNotNull(store.updatedMessage);
                                assertEquals(EventHookMessageStatus.SUCCESS, store.updatedMessage.getStatus());
                                assertNotNull(store.createdLog);
                                assertEquals(Boolean.TRUE, store.createdLog.isTest());
        }

        @Test
        public void shouldConsumeWaitingEntriesWithoutSecretWhenPullTargetHasNoSecret() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers(null, null);

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of("deliveryMode", "SINGLE"));

                EventHookMessageModel message = new EventHookMessageModel();
                message.setId("msg-1");
                message.setRealmId("realm-1");
                message.setTargetId("target-1");
                message.setSourceEventId("event-1");
                message.setAttemptCount(1);
                message.setStatus(EventHookMessageStatus.WAITING);
                message.setPayload("{\"event\":\"LOGIN\"}");
                message.setUpdatedAt(100L);

                store.target = target;
                store.messages = List.of(message);
                store.hasMoreMessages = false;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");
                EventHookPullRepresentation representation = (EventHookPullRepresentation) ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

                assertNotNull(representation.getEvent());
                assertEquals("LOGIN", ((Map<?, ?>) representation.getEvent()).get("event"));
                assertNotNull(store.updatedMessage);
                assertEquals(EventHookMessageStatus.SUCCESS, store.updatedMessage.getStatus());
        }

        @Test
        public void shouldExposePullConsumeEndpointThroughRealmResource() {
                RecordingStoreProvider store = new RecordingStoreProvider();
                RealmModel realm = realm("realm-1");
                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                store.target = target;

                Object endpoint = new EventHooksRealmResource(session, realm).getTargetEndpoint("target-1", "consume");

                assertTrue(endpoint instanceof PullEventHookTargetEndpointResource);
        }

    @Test
    public void shouldConsumeWaitingEntriesThroughTargetEndpoint() {
                RecordingStoreProvider store = new RecordingStoreProvider();
                RealmModel realm = realm("realm-1");
                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                HttpHeaders headers = headers("top-secret", null);

        EventHookTargetModel target = new EventHookTargetModel();
        target.setId("target-1");
        target.setType(PullEventHookTargetProviderFactory.ID);
        target.setEnabled(true);
        target.setSettings(Map.of("pullSecret", "top-secret", "deliveryMode", "SINGLE"));

        EventHookMessageModel message = new EventHookMessageModel();
        message.setId("msg-1");
        message.setRealmId("realm-1");
        message.setTargetId("target-1");
        message.setSourceEventId("event-1");
        message.setAttemptCount(1);
        message.setStatus(EventHookMessageStatus.WAITING);
        message.setPayload("{\"event\":\"LOGIN\"}");
        message.setUpdatedAt(100L);

        EventHookLogModel waitingLog = new EventHookLogModel();
        waitingLog.setId("log-1");
        waitingLog.setExecutionId("exec-1");
        waitingLog.setStatus(EventHookLogStatus.WAITING);
        waitingLog.setAttemptNumber(1);
        waitingLog.setStatusCode("PULL_WAITING");
        waitingLog.setDetails("Waiting for consumption");
        waitingLog.setCreatedAt(200L);

        store.target = target;
        store.messages = List.of(message);
        store.latestLog = waitingLog;
        store.hasMoreMessages = false;

        Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");
        assertTrue(endpoint instanceof PullEventHookTargetEndpointResource);

        EventHookPullRepresentation representation = (EventHookPullRepresentation) ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

        assertNotNull(representation.getEvent());
        assertTrue(representation.getEvent() instanceof Map<?, ?>);
        assertEquals("LOGIN", ((Map<?, ?>) representation.getEvent()).get("event"));
        assertNotNull(representation.getEntry());
        assertEquals("log-1", representation.getEntry().getLogId());
        assertEquals("WAITING", representation.getEntry().getStatus());
        assertEquals("msg-1", representation.getEntry().getMessageId());
        assertTrue(representation.getEntry().getData() instanceof Map<?, ?>);
        assertEquals("LOGIN", ((Map<?, ?>) representation.getEntry().getData()).get("event"));
        assertFalse(representation.isHasMoreEvents());

                assertNotNull(store.updatedMessage);
                assertEquals(EventHookMessageStatus.SUCCESS, store.updatedMessage.getStatus());
                assertEquals(1, store.updatedMessage.getAttemptCount());

                assertNotNull(store.createdLog);
                assertEquals(EventHookLogStatus.SUCCESS, store.createdLog.getStatus());
                assertEquals(1, store.createdLog.getAttemptNumber());
                assertEquals("PULL_CONSUMED", store.createdLog.getStatusCode());
                                assertFalse(store.createdLog.isTest());
    }

        @Test
        public void shouldConsumePullMessagesInFifoOrder() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers("top-secret", null);

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of("pullSecret", "top-secret", "deliveryMode", "BULK", "maxEventsPerBatch", 10));

                EventHookMessageModel newestMessage = message("msg-3", "event-3", 300L);
                EventHookMessageModel oldestMessage = message("msg-1", "event-1", 100L);
                EventHookMessageModel middleMessage = message("msg-2", "event-2", 200L);

                store.target = target;
                store.messages = List.of(newestMessage, oldestMessage, middleMessage);
                store.hasMoreMessages = false;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");
                EventHookPullRepresentation representation = (EventHookPullRepresentation) ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

                assertNotNull(representation.getEvents());
                assertEquals(List.of("event-1", "event-2", "event-3"), representation.getEvents().stream()
                                .map(Map.class::cast)
                                .map(event -> String.valueOf(event.get("event")))
                                .toList());
                assertNotNull(representation.getEntries());
                assertEquals(List.of("msg-1", "msg-2", "msg-3"), representation.getEntries().stream()
                                .map(EventHookPullEntryRepresentation::getMessageId)
                                .toList());
                assertEquals(List.of("event-1", "event-2", "event-3"), representation.getEntries().stream()
                                .map(EventHookPullEntryRepresentation::getData)
                                .map(Map.class::cast)
                                .map(event -> String.valueOf(event.get("event")))
                                .toList());
                assertEquals(List.of("msg-1", "msg-2", "msg-3"), store.updatedMessages.stream()
                                .map(EventHookMessageModel::getId)
                                .toList());
                assertEquals(3, store.createdLogs.size());
                assertTrue(store.createdLogs.stream().allMatch(log -> "PULL_CONSUMED".equals(log.getStatusCode())));
        }

        @Test
        public void shouldParseRepresentationJsonForPullDelivery() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers("top-secret", null);

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of("pullSecret", "top-secret", "deliveryMode", "SINGLE"));

                EventHookMessageModel message = new EventHookMessageModel();
                message.setId("msg-1");
                message.setRealmId("realm-1");
                message.setTargetId("target-1");
                message.setSourceEventId("event-1");
                message.setAttemptCount(1);
                message.setStatus(EventHookMessageStatus.WAITING);
                message.setPayload("{\"event\":\"UPDATE\",\"representation\":\"{\\\"id\\\":\\\"user-1\\\",\\\"enabled\\\":true}\"}");
                message.setUpdatedAt(100L);

                store.target = target;
                store.messages = List.of(message);
                store.hasMoreMessages = false;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");
                EventHookPullRepresentation representation = (EventHookPullRepresentation) ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

        @Test
        public void shouldRenderCustomPullBodyMapping() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers("top-secret", null);

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of(
                        "pullSecret", "top-secret",
                        "deliveryMode", "SINGLE",
                        "customBodyMappingTemplate", "{\"event\": ${event}, \"messageId\": ${entry.messageId?json_string}, \"hasMoreEvents\": ${hasMoreEvents}}"
                ));

                EventHookMessageModel message = new EventHookMessageModel();
                message.setId("msg-1");
                message.setRealmId("realm-1");
                message.setTargetId("target-1");
                message.setSourceEventId("event-1");
                message.setAttemptCount(1);
                message.setStatus(EventHookMessageStatus.WAITING);
                message.setPayload("{\"event\":\"LOGIN\"}");
                message.setUpdatedAt(100L);

                store.target = target;
                store.messages = List.of(message);
                store.hasMoreMessages = true;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");
                Object response = ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

                assertTrue(response instanceof Map<?, ?>);
                assertEquals("LOGIN", ((Map<?, ?>) ((Map<?, ?>) response).get("event")).get("event"));
                assertEquals("msg-1", ((Map<?, ?>) response).get("messageId"));
                assertEquals(Boolean.TRUE, ((Map<?, ?>) response).get("hasMoreEvents"));
                assertEquals(EventHookMessageStatus.SUCCESS, store.updatedMessage.getStatus());
        }

        @Test(expected = InternalServerErrorException.class)
        public void shouldMarkPullMessagesAsParseFailedWhenCustomMappingRendersInvalidJson() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers("top-secret", null);

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of(
                        "pullSecret", "top-secret",
                        "deliveryMode", "SINGLE",
                        "customBodyMappingTemplate", "{\"messageId\": ${entry.messageId}}"
                ));

                EventHookMessageModel message = new EventHookMessageModel();
                message.setId("msg-parse-failed");
                message.setRealmId("realm-1");
                message.setTargetId("target-1");
                message.setSourceEventId("event-1");
                message.setAttemptCount(1);
                message.setStatus(EventHookMessageStatus.WAITING);
                message.setPayload("{\"event\":\"LOGIN\"}");
                message.setUpdatedAt(100L);

                store.target = target;
                store.messages = List.of(message);
                store.hasMoreMessages = false;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");

                try {
                        ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);
                } finally {
                        assertEquals(EventHookMessageStatus.PARSE_FAILED, store.updatedMessage.getStatus());
                        assertEquals("PARSE_FAILED", store.createdLog.getStatusCode());
                        assertEquals(EventHookMessageStatus.PARSE_FAILED, store.createdLog.getMessageStatus());
                }
        }

                assertTrue(representation.getEvent() instanceof Map<?, ?>);
                assertTrue(((Map<?, ?>) representation.getEvent()).get("representation") instanceof Map<?, ?>);
                assertEquals("user-1", ((Map<?, ?>) ((Map<?, ?>) representation.getEvent()).get("representation")).get("id"));
                assertTrue(representation.getEntry().getData() instanceof Map<?, ?>);
                assertTrue(((Map<?, ?>) representation.getEntry().getData()).get("representation") instanceof Map<?, ?>);
        }

        @Test(expected = NotAuthorizedException.class)
        public void shouldRejectBearerAuthorizationForPullEndpoint() {
                                RecordingStoreProvider store = new RecordingStoreProvider();
                                RealmModel realm = realm("realm-1");
                                KeycloakSession session = session(realm, store, new PullEventHookTargetProviderFactory());
                                HttpHeaders headers = headers(null, "Bearer top-secret");

                EventHookTargetModel target = new EventHookTargetModel();
                target.setId("target-1");
                target.setType(PullEventHookTargetProviderFactory.ID);
                target.setEnabled(true);
                target.setSettings(Map.of("pullSecret", "top-secret", "deliveryMode", "SINGLE"));

                store.target = target;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");

                ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);
        }

        private RealmModel realm(String realmId) {
                return (RealmModel) Proxy.newProxyInstance(
                                RealmModel.class.getClassLoader(),
                                new Class<?>[] { RealmModel.class },
                                (proxy, method, args) -> switch (method.getName()) {
                                        case "getId" -> realmId;
                                        default -> null;
                                });
        }

        private HttpHeaders headers(String secretHeader, String authorization) {
                return (HttpHeaders) Proxy.newProxyInstance(
                                HttpHeaders.class.getClassLoader(),
                                new Class<?>[] { HttpHeaders.class },
                                (proxy, method, args) -> {
                                        if ("getHeaderString".equals(method.getName()) && HttpHeaders.AUTHORIZATION.equals(args[0])) {
                                                return authorization;
                                        }
                                        if ("getHeaderString".equals(method.getName()) && "X-Keycloak-Event-Hook-Secret".equals(args[0])) {
                                                return secretHeader;
                                        }
                                        return null;
                                });
        }

        private KeycloakSession session(RealmModel realm, RecordingStoreProvider store, EventHookTargetProviderFactory factory) {
                KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                                KeycloakContext.class.getClassLoader(),
                                new Class<?>[] { KeycloakContext.class },
                                (proxy, method, args) -> "getRealm".equals(method.getName()) ? realm : null);

                KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(
                                KeycloakSessionFactory.class.getClassLoader(),
                                new Class<?>[] { KeycloakSessionFactory.class },
                                (proxy, method, args) -> {
                                        if ("getProviderFactory".equals(method.getName()) && args.length == 2 && args[0] == EventHookTargetProvider.class
                                                        && PullEventHookTargetProviderFactory.ID.equals(args[1])) {
                                                return factory;
                                        }
                                        return null;
                                });

                return (KeycloakSession) Proxy.newProxyInstance(
                                KeycloakSession.class.getClassLoader(),
                                new Class<?>[] { KeycloakSession.class },
                                (proxy, method, args) -> {
                                        if ("getContext".equals(method.getName())) {
                                                return context;
                                        }
                                        if ("getProvider".equals(method.getName()) && args.length == 1 && args[0] == EventHookStoreProvider.class) {
                                                return store;
                                        }
                                        if ("getKeycloakSessionFactory".equals(method.getName())) {
                                                return sessionFactory;
                                        }
                                        return null;
                                });
        }

                        private EventHookMessageModel message(String id, String event, long createdAt) {
                                EventHookMessageModel message = new EventHookMessageModel();
                                message.setId(id);
                                message.setRealmId("realm-1");
                                message.setTargetId("target-1");
                                message.setSourceEventId(event);
                                message.setAttemptCount(1);
                                message.setStatus(EventHookMessageStatus.WAITING);
                                message.setPayload("{\"event\":\"" + event + "\"}");
                                message.setCreatedAt(createdAt);
                                message.setUpdatedAt(createdAt);
                                return message;
                        }

        private static final class RecordingStoreProvider implements EventHookStoreProvider {

                private EventHookTargetModel target;
                private List<EventHookMessageModel> messages = List.of();
                                private List<EventHookMessageModel> testMessages = List.of();
                private EventHookLogModel latestLog;
                private boolean hasMoreMessages;
                                private boolean hasMoreTestMessages;
                private EventHookMessageModel updatedMessage;
                                private final List<EventHookMessageModel> updatedMessages = new ArrayList<>();
                private EventHookLogModel createdLog;
                                private final List<EventHookLogModel> createdLogs = new ArrayList<>();

                @Override
                public EventHookTargetModel getTarget(String realmId, String targetId) {
                        return target;
                }

                @Override
                public List<EventHookMessageModel> reserveAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                                long executionTimeoutMillis, String executionId) {
                        return messages;
                }

                                @Override
                                public List<EventHookMessageModel> reserveAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                                                                long executionTimeoutMillis, String executionId, boolean test) {
                                                return test ? testMessages : messages;
                                }

                @Override
                public Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
                                String sourceType, String event, String client, String user, String ipAddress,
                                String resourceType, String resourcePath, String status, String messageStatus,
                                Long dateFrom, Long dateTo, String executionId, String search, Integer first, Integer max) {
                        return latestLog == null ? Stream.empty() : Stream.of(latestLog);
                }

                @Override
                public boolean hasAvailableMessages(String realmId, String targetId, long now) {
                        return hasMoreMessages;
                }

                                @Override
                                public boolean hasAvailableMessages(String realmId, String targetId, long now, boolean test) {
                                                return test ? hasMoreTestMessages : hasMoreMessages;
                                }

                @Override
                public EventHookMessageModel updateMessage(EventHookMessageModel message) {
                        updatedMessage = message;
                        updatedMessages.add(message);
                        return message;
                }

                @Override
                public void createLog(EventHookLogModel log) {
                        createdLog = log;
                        createdLogs.add(log);
                }

                @Override
                public Stream<EventHookTargetModel> getTargetsStream(String realmId, Boolean enabled) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public EventHookTargetModel createTarget(EventHookTargetModel target) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public EventHookTargetModel updateTarget(EventHookTargetModel target) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public boolean deleteTarget(String realmId, String targetId) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public void createMessages(List<EventHookMessageModel> messages) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public Stream<EventHookMessageModel> getMessagesStream(String realmId, String status, String targetId, Integer first, Integer max) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public EventHookMessageModel getMessage(String realmId, String messageId) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public Map<String, EventHookTargetStatus> getLatestTargetStatuses(String realmId) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public List<EventHookMessageModel> reserveAvailableMessages(int maxResults, long now, long executionTimeoutMillis) {
                        throw new UnsupportedOperationException();
                }

                                @Override
                                public void clearExpiredMessagesAndLogs(long olderThan) {
                                                throw new UnsupportedOperationException();
                                }

                @Override
                public void close() {
                }
        }
}
