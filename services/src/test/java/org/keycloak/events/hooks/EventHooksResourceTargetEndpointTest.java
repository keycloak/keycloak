package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
                message.setStatus(EventHookMessageStatus.CLAIMED);
                message.setPayload("{\"event\":\"LOGIN\"}");
                message.setUpdatedAt(100L);

                store.target = target;
                store.messages = List.of(message);
                store.hasMoreMessages = false;

                Object endpoint = new EventHooksResource(session, null, null).getTargetEndpoint("target-1", "consume");
                EventHookPullRepresentation representation = ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

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
        message.setStatus(EventHookMessageStatus.CLAIMED);
        message.setPayload("{\"event\":\"LOGIN\"}");
        message.setUpdatedAt(100L);

        EventHookLogModel waitingLog = new EventHookLogModel();
        waitingLog.setId("log-1");
        waitingLog.setExecutionId("exec-1");
        waitingLog.setBatchExecution(false);
        waitingLog.setMessageId("msg-1");
        waitingLog.setTargetId("target-1");
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

        EventHookPullRepresentation representation = ((PullEventHookTargetEndpointResource) endpoint).consumeGet(headers);

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
                assertNull(store.updatedMessage.getClaimOwner());
                assertNull(store.updatedMessage.getClaimedAt());

                assertNotNull(store.createdLog);
                assertEquals(EventHookLogStatus.SUCCESS, store.createdLog.getStatus());
                assertEquals(1, store.createdLog.getAttemptNumber());
                assertEquals("PULL_CONSUMED", store.createdLog.getStatusCode());
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

        private static final class RecordingStoreProvider implements EventHookStoreProvider {

                private EventHookTargetModel target;
                private List<EventHookMessageModel> messages = List.of();
                private EventHookLogModel latestLog;
                private boolean hasMoreMessages;
                private EventHookMessageModel updatedMessage;
                private EventHookLogModel createdLog;

                @Override
                public EventHookTargetModel getTarget(String realmId, String targetId) {
                        return target;
                }

                @Override
                public List<EventHookMessageModel> claimAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                                long staleClaimBefore, String claimOwner) {
                        return messages;
                }

                @Override
                public Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
                                String sourceType, String event, String client, String user, String ipAddress,
                                String resourceType, String resourcePath, String status, String messageStatus,
                                Long dateFrom, Long dateTo, String executionId, String search, Integer first, Integer max) {
                        return latestLog == null ? Stream.empty() : Stream.of(latestLog);
                }

                @Override
                public boolean hasAvailableMessages(String realmId, String targetId, long now, long staleClaimBefore) {
                        return hasMoreMessages;
                }

                @Override
                public EventHookMessageModel updateMessage(EventHookMessageModel message) {
                        updatedMessage = message;
                        return message;
                }

                @Override
                public void createLog(EventHookLogModel log) {
                        createdLog = log;
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
                public List<EventHookMessageModel> claimAvailableMessages(int maxResults, long now, long staleClaimBefore, String claimOwner) {
                        throw new UnsupportedOperationException();
                }

                @Override
                public void close() {
                }
        }
}
