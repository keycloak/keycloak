package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventHookMessageRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.EventHooksResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RealmPermissionEvaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EventHooksResourceRetryTest {

    @Test
    public void shouldQueueFailedExecutionForManualRetry() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        store.messages = List.of(
                message("msg-1", "exec-1", EventHookMessageStatus.EXHAUSTED, 3),
                message("msg-2", "exec-1", EventHookMessageStatus.FAILED, 2));

        List<EventHookMessageRepresentation> retried = new EventHooksResource(session(realm("realm-1"), store), auth(), null)
                .retryExecution("exec-1");

        assertEquals(2, retried.size());
        retried.forEach(message -> {
            assertEquals("PENDING", message.getStatus());
            assertEquals(Integer.valueOf(0), message.getAttemptCount());
            assertNull(message.getExecutionStartedAt());
            assertNull(message.getLastError());
            assertNotNull(message.getExecutionId());
            assertEquals(Boolean.TRUE, message.getExecutionBatch());
        });

        assertEquals(2, store.updatedMessages.size());
        store.updatedMessages.forEach(message -> {
            assertEquals(EventHookMessageStatus.PENDING, message.getStatus());
            assertEquals(0, message.getAttemptCount());
            assertNull(message.getExecutionStartedAt());
            assertNull(message.getLastError());
            assertNotNull(message.getExecutionId());
            assertEquals(true, message.isExecutionBatch());
        });
        assertEquals(store.updatedMessages.get(0).getExecutionId(), store.updatedMessages.get(1).getExecutionId());

        assertNotNull(store.createdLog);
        assertEquals(EventHookLogStatus.PENDING, store.createdLog.getStatus());
        assertEquals(EventHookMessageStatus.PENDING, store.createdLog.getMessageStatus());
        assertEquals(3, store.createdLog.getAttemptNumber());
        assertEquals("MANUAL_RETRY", store.createdLog.getStatusCode());
        assertEquals("Manual retry requested", store.createdLog.getDetails());
    }

    @Test
    public void shouldRejectRetryForExecutionWithoutRetryableMessages() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        store.messages = List.of(message("msg-1", "exec-1", EventHookMessageStatus.SUCCESS, 1));

        try {
            new EventHooksResource(session(realm("realm-1"), store), auth(), null).retryExecution("exec-1");
        } catch (ErrorResponseException exception) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
            assertEquals(0, store.updatedMessages.size());
            assertNull(store.createdLog);
            return;
        }

        throw new AssertionError("Expected retry to fail for execution without retryable messages");
    }

    @Test(expected = NotFoundException.class)
    public void shouldRejectRetryForUnknownExecution() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        new EventHooksResource(session(realm("realm-1"), store), auth(), null).retryExecution("missing");
    }

    private EventHookMessageModel message(String id, String executionId, EventHookMessageStatus status, int attemptCount) {
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId(id);
        message.setRealmId("realm-1");
        message.setTargetId("target-1");
        message.setExecutionId(executionId);
        message.setSourceType(EventHookSourceType.USER);
        message.setSourceEventId("event-1");
        message.setStatus(status);
        message.setAttemptCount(attemptCount);
        message.setNextAttemptAt(200L);
        message.setCreatedAt(1L);
        message.setUpdatedAt(2L);
        message.setLastError("boom");
        return message;
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

    private AdminPermissionEvaluator auth() {
        RealmPermissionEvaluator realmPermissionEvaluator = (RealmPermissionEvaluator) Proxy.newProxyInstance(
                RealmPermissionEvaluator.class.getClassLoader(),
                new Class<?>[] { RealmPermissionEvaluator.class },
                (proxy, method, args) -> null);

        return (AdminPermissionEvaluator) Proxy.newProxyInstance(
                AdminPermissionEvaluator.class.getClassLoader(),
                new Class<?>[] { AdminPermissionEvaluator.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "realm" -> realmPermissionEvaluator;
                    default -> null;
                });
    }

    private KeycloakSession session(RealmModel realm, RecordingStoreProvider store) {
        KeycloakContext context = (KeycloakContext) Proxy.newProxyInstance(
                KeycloakContext.class.getClassLoader(),
                new Class<?>[] { KeycloakContext.class },
                (proxy, method, args) -> "getRealm".equals(method.getName()) ? realm : null);

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
                    return null;
                });
    }

    private static final class RecordingStoreProvider implements EventHookStoreProvider {

        private List<EventHookMessageModel> messages = List.of();
        private List<EventHookMessageModel> updatedMessages = new java.util.ArrayList<>();
        private EventHookLogModel createdLog;

        @Override
        public EventHookMessageModel getMessage(String realmId, String messageId) {
            return messages.stream()
                    .filter(message -> realmId.equals(message.getRealmId()) && messageId.equals(message.getId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public EventHookMessageModel updateMessage(EventHookMessageModel message) {
            this.updatedMessages.add(message);
            return message;
        }

        @Override
        public void createLog(EventHookLogModel log) {
            this.createdLog = log;
        }

        @Override
        public Stream<EventHookTargetModel> getTargetsStream(String realmId, Boolean enabled) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventHookTargetModel getTarget(String realmId, String targetId) {
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
            return getMessagesStream(realmId, status, targetId, null, null, null, null, null, null, null, null, null, null, first, max);
        }

        @Override
        public Stream<EventHookMessageModel> getMessagesStream(String realmId, String status, String targetId, String targetType,
                String sourceType, String event, String client, String user, String ipAddress,
                String resourceType, String resourcePath, String executionId, String search,
                Integer first, Integer max) {
            return messages.stream()
                    .filter(message -> realmId.equals(message.getRealmId()))
                    .filter(message -> status == null || message.getStatus().name().equals(status))
                    .filter(message -> targetId == null || targetId.equals(message.getTargetId()))
                    .filter(message -> executionId == null || executionId.equals(message.getExecutionId()));
        }

        @Override
        public Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
                String sourceType, String event, String client, String user, String ipAddress,
                String resourceType, String resourcePath, String status, String messageStatus,
                Long dateFrom, Long dateTo, String executionId, String search, Integer first, Integer max) {
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
        public List<EventHookMessageModel> reserveAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                long executionTimeoutMillis, String executionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAvailableMessages(String realmId, String targetId, long now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
