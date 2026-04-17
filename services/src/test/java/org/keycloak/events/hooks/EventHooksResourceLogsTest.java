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
import org.keycloak.representations.idm.EventHookMessageRepresentation;
import org.keycloak.services.resources.admin.EventHooksResource;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RealmPermissionEvaluator;
import org.keycloak.services.util.DateUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EventHooksResourceLogsTest {

    @Test
    public void shouldForwardStructuredLogFiltersAndExposeSourceFields() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        EventHookLogModel log = new EventHookLogModel();
        log.setId("log-1");
        log.setExecutionId("exec-1");
        log.setStatus(EventHookLogStatus.SUCCESS);
        log.setMessageStatus(EventHookMessageStatus.SUCCESS);
        log.setAttemptNumber(2);
        log.setStatusCode("200");
        log.setDurationMs(15L);
        log.setDetails("ok");
        log.setCreatedAt(1234L);
        log.setTest(true);
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
        assertEquals("exec-1", representation.getExecutionId());
        assertEquals("SUCCESS", representation.getStatus());
        assertEquals("SUCCESS", representation.getMessageStatus());
        assertEquals(Boolean.TRUE, representation.getTest());
        assertNotNull(representation.getCreatedAt());
    }

    @Test
    public void shouldForwardStructuredMessageFiltersAndExposeMessageFields() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        EventHookMessageModel message = new EventHookMessageModel();
        message.setId("msg-1");
        message.setTargetId("target-1");
        message.setExecutionId("exec-1");
        message.setSourceType(EventHookSourceType.ADMIN);
        message.setSourceEventId("event-1");
        message.setSourceEventName("CREATE");
        message.setUserId("user-1");
        message.setResourcePath("users/123");
        message.setExecutionBatch(true);
        message.setStatus(EventHookMessageStatus.SUCCESS);
        message.setAttemptCount(2);
        message.setNextAttemptAt(100L);
        message.setCreatedAt(1234L);
        message.setUpdatedAt(1235L);
        message.setTest(true);
        message.setPayload("{\"operationType\":\"CREATE\"}");
        store.messages = List.of(message);

        List<EventHookMessageRepresentation> messages = new EventHooksResource(session(realm("realm-1"), store), auth(), null)
                .getMessages("SUCCESS", "target-1", "http", "ADMIN", "CREATE", "security-admin-console",
                        "user-1", "127.0.0.1", "USER", "users/123", "exec-1", "retry", 5, 10);

        assertEquals("realm-1", store.messageRealmId);
        assertEquals("SUCCESS", store.messageQueryStatus);
        assertEquals("target-1", store.messageQueryTargetId);
        assertEquals("http", store.messageQueryTargetType);
        assertEquals("ADMIN", store.messageQuerySourceType);
        assertEquals("CREATE", store.messageQueryEvent);
        assertEquals("security-admin-console", store.messageQueryClient);
        assertEquals("user-1", store.messageQueryUser);
        assertEquals("127.0.0.1", store.messageQueryIpAddress);
        assertEquals("USER", store.messageQueryResourceType);
        assertEquals("users/123", store.messageQueryResourcePath);
        assertEquals("exec-1", store.messageQueryExecutionId);
        assertEquals("retry", store.messageQuerySearch);
        assertEquals(Integer.valueOf(5), store.messageQueryFirst);
        assertEquals(Integer.valueOf(10), store.messageQueryMax);

        assertEquals(1, messages.size());
        EventHookMessageRepresentation representation = messages.get(0);
        assertEquals("exec-1", representation.getExecutionId());
        assertEquals("ADMIN", representation.getSourceType());
        assertEquals("CREATE", representation.getSourceEventName());
        assertEquals("event-1", representation.getSourceEventId());
        assertEquals("user-1", representation.getUserId());
        assertEquals("users/123", representation.getResourcePath());
        assertEquals(Boolean.TRUE, representation.getExecutionBatch());
        assertEquals("SUCCESS", representation.getStatus());
        assertEquals(Boolean.TRUE, representation.getTest());
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
        private List<EventHookMessageModel> messages = List.of();
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
        private String messageRealmId;
        private String messageQueryStatus;
        private String messageQueryTargetId;
        private String messageQueryTargetType;
        private String messageQuerySourceType;
        private String messageQueryEvent;
        private String messageQueryClient;
        private String messageQueryUser;
        private String messageQueryIpAddress;
        private String messageQueryResourceType;
        private String messageQueryResourcePath;
        private String messageQueryExecutionId;
        private String messageQuerySearch;
        private Integer messageQueryFirst;
        private Integer messageQueryMax;

        @Override
        public Stream<EventHookMessageModel> getMessagesStream(String realmId, String status, String targetId, String targetType,
                String sourceType, String event, String client, String user, String ipAddress,
                String resourceType, String resourcePath, String executionId, String search,
                Integer first, Integer max) {
            this.messageRealmId = realmId;
            this.messageQueryStatus = status;
            this.messageQueryTargetId = targetId;
            this.messageQueryTargetType = targetType;
            this.messageQuerySourceType = sourceType;
            this.messageQueryEvent = event;
            this.messageQueryClient = client;
            this.messageQueryUser = user;
            this.messageQueryIpAddress = ipAddress;
            this.messageQueryResourceType = resourceType;
            this.messageQueryResourcePath = resourcePath;
            this.messageQueryExecutionId = executionId;
            this.messageQuerySearch = search;
            this.messageQueryFirst = first;
            this.messageQueryMax = max;
            return messages.stream();
        }

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
            return messages.stream();
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
        public List<EventHookMessageModel> reserveAvailableMessagesForTarget(String realmId, String targetId, int maxResults, long now,
                long executionTimeoutMillis, String executionId, boolean test) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAvailableMessages(String realmId, String targetId, long now, boolean test) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearExpiredMessagesAndLogs(long olderThan) {
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
