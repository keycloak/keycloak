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
    public void shouldQueueFailedMessageForManualRetry() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        store.message = message(EventHookMessageStatus.EXHAUSTED, 3);

        EventHookMessageRepresentation retried = new EventHooksResource(session(realm("realm-1"), store), auth(), null)
                .retryMessage("msg-1");

        assertEquals("PENDING", retried.getStatus());
        assertEquals(Integer.valueOf(0), retried.getAttemptCount());
        assertNull(retried.getLastError());

        assertNotNull(store.updatedMessage);
        assertEquals(EventHookMessageStatus.PENDING, store.updatedMessage.getStatus());
        assertEquals(0, store.updatedMessage.getAttemptCount());
        assertNull(store.updatedMessage.getClaimOwner());
        assertNull(store.updatedMessage.getClaimedAt());
        assertNull(store.updatedMessage.getLastError());

        assertNotNull(store.createdLog);
        assertEquals(EventHookLogStatus.PENDING, store.createdLog.getStatus());
        assertEquals(EventHookMessageStatus.PENDING, store.createdLog.getMessageStatus());
        assertEquals(3, store.createdLog.getAttemptNumber());
        assertEquals("MANUAL_RETRY", store.createdLog.getStatusCode());
        assertEquals("Manual retry requested", store.createdLog.getDetails());
    }

    @Test
    public void shouldRejectRetryForSuccessfulMessage() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        store.message = message(EventHookMessageStatus.SUCCESS, 1);

        try {
            new EventHooksResource(session(realm("realm-1"), store), auth(), null).retryMessage("msg-1");
        } catch (ErrorResponseException exception) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
            assertNull(store.updatedMessage);
            assertNull(store.createdLog);
            return;
        }

        throw new AssertionError("Expected retry to fail for successful message");
    }

    @Test(expected = NotFoundException.class)
    public void shouldRejectRetryForUnknownMessage() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        new EventHooksResource(session(realm("realm-1"), store), auth(), null).retryMessage("missing");
    }

    private EventHookMessageModel message(EventHookMessageStatus status, int attemptCount) {
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId("msg-1");
        message.setRealmId("realm-1");
        message.setTargetId("target-1");
        message.setSourceType(EventHookSourceType.USER);
        message.setSourceEventId("event-1");
        message.setStatus(status);
        message.setAttemptCount(attemptCount);
        message.setClaimOwner("worker-1");
        message.setClaimedAt(100L);
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

        private EventHookMessageModel message;
        private EventHookMessageModel updatedMessage;
        private EventHookLogModel createdLog;

        @Override
        public EventHookMessageModel getMessage(String realmId, String messageId) {
            if (message != null && realmId.equals(message.getRealmId()) && messageId.equals(message.getId())) {
                return message;
            }
            return null;
        }

        @Override
        public EventHookMessageModel updateMessage(EventHookMessageModel message) {
            this.message = message;
            this.updatedMessage = message;
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
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
                String executionId, String search, Integer first, Integer max) {
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
        public List<EventHookMessageModel> claimAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                long staleClaimBefore, String claimOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAvailableMessages(String realmId, String targetId, long now, long staleClaimBefore) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
