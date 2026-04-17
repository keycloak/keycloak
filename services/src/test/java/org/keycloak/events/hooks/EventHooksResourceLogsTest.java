package org.keycloak.events.hooks;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventHookLogRepresentation;
import org.keycloak.services.resources.admin.EventHooksResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RealmPermissionEvaluator;
import org.keycloak.services.util.DateUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventHooksResourceLogsTest {

    @Test
    public void shouldForwardStructuredLogFiltersAndExposeSourceFields() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        EventHookLogModel log = new EventHookLogModel();
        log.setId("log-1");
        log.setExecutionId("exec-1");
        log.setBatchExecution(false);
        log.setMessageId("msg-1");
        log.setTargetId("target-1");
        log.setSourceType(EventHookSourceType.ADMIN);
        log.setSourceEventId("event-1");
        log.setSourceEventName("CREATE");
        log.setStatus(EventHookLogStatus.SUCCESS);
        log.setMessageStatus(EventHookMessageStatus.SUCCESS);
        log.setAttemptNumber(2);
        log.setStatusCode("200");
        log.setDurationMs(15L);
        log.setDetails("ok");
        log.setCreatedAt(1234L);
        store.logs = List.of(log);

        List<EventHookLogRepresentation> logs = new EventHooksResource(session(realm("realm-1"), store), auth(), null)
                .getLogs("target-1", "http", "ADMIN", "CREATE", "security-admin-console", "user-1", "127.0.0.1",
                        "USER", "users/123", "SUCCESS", "SUCCESS", "2026-04-01", "2026-04-16",
                        "exec-1", "retry", "msg-1", 5, 10);

        assertEquals("realm-1", store.realmId);
        assertEquals("msg-1", store.messageId);
        assertEquals("target-1", store.targetId);
        assertEquals("http", store.targetType);
        assertEquals("ADMIN", store.sourceType);
        assertEquals("CREATE", store.event);
        assertEquals("security-admin-console", store.client);
        assertEquals("user-1", store.user);
        assertEquals("127.0.0.1", store.ipAddress);
        assertEquals("USER", store.resourceType);
        assertEquals("users/123", store.resourcePath);
        assertEquals("SUCCESS", store.status);
        assertEquals("SUCCESS", store.messageStatus);
        assertEquals(Long.valueOf(DateUtil.toStartOfDay("2026-04-01")), store.dateFrom);
        assertEquals(Long.valueOf(DateUtil.toEndOfDay("2026-04-16")), store.dateTo);
        assertEquals("exec-1", store.executionId);
        assertEquals("retry", store.search);
        assertEquals(Integer.valueOf(5), store.first);
        assertEquals(Integer.valueOf(10), store.max);

        assertEquals(1, logs.size());
        EventHookLogRepresentation representation = logs.get(0);
        assertEquals("ADMIN", representation.getSourceType());
        assertEquals("CREATE", representation.getSourceEventName());
        assertEquals("event-1", representation.getSourceEventId());
        assertEquals("SUCCESS", representation.getStatus());
        assertEquals("SUCCESS", representation.getMessageStatus());
        assertNotNull(representation.getCreatedAt());
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

        private List<EventHookLogModel> logs = List.of();
        private String realmId;
        private String messageId;
        private String targetId;
        private String targetType;
        private String sourceType;
        private String event;
        private String client;
        private String user;
        private String ipAddress;
        private String resourceType;
        private String resourcePath;
        private String status;
        private String messageStatus;
        private Long dateFrom;
        private Long dateTo;
        private String executionId;
        private String search;
        private Integer first;
        private Integer max;

        @Override
        public Stream<EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId, String targetType,
                String sourceType, String event, String client, String user, String ipAddress,
                String resourceType, String resourcePath, String status, String messageStatus,
                Long dateFrom, Long dateTo, String executionId, String search, Integer first, Integer max) {
            this.realmId = realmId;
            this.messageId = messageId;
            this.targetId = targetId;
            this.targetType = targetType;
            this.sourceType = sourceType;
            this.event = event;
            this.client = client;
            this.user = user;
            this.ipAddress = ipAddress;
            this.resourceType = resourceType;
            this.resourcePath = resourcePath;
            this.status = status;
            this.messageStatus = messageStatus;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.executionId = executionId;
            this.search = search;
            this.first = first;
            this.max = max;
            return logs.stream();
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
        public List<EventHookMessageModel> claimAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                long staleClaimBefore, String claimOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAvailableMessages(String realmId, String targetId, long now, long staleClaimBefore) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventHookMessageModel updateMessage(EventHookMessageModel message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createLog(EventHookLogModel log) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
