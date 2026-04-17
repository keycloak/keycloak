package org.keycloak.events.jpa;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.junit.Assert.assertEquals;

public class JpaEventStoreProviderTest {

    @Test
    public void shouldDeleteOrphanedEventHookLogsWhenClearingUserEventsForRealm() {
        RecordingEntityManager recordingEntityManager = new RecordingEntityManager();
        JpaEventStoreProvider provider = new JpaEventStoreProvider(session(), recordingEntityManager.entityManager());

        provider.clear(realm("realm-1"));

        assertEquals(3, recordingEntityManager.queries.size());
        assertEquals(
                "delete from EventHookMessageEntity where realmId = :realmId and sourceType = :sourceType",
                recordingEntityManager.queries.get(0).query);
        assertEquals("realm-1", recordingEntityManager.queries.get(0).parameters.get("realmId"));
        assertEquals("USER", recordingEntityManager.queries.get(0).parameters.get("sourceType"));
        assertEquals(
                "delete from EventHookLogEntity log where not exists (select 1 from EventHookMessageEntity message where message.executionId = log.executionId)",
                recordingEntityManager.queries.get(1).query);
        assertEquals(
                "delete from EventEntity where realmId = :realmId",
                recordingEntityManager.queries.get(2).query);
        assertEquals("realm-1", recordingEntityManager.queries.get(2).parameters.get("realmId"));
    }

    @Test
    public void shouldDeleteOrphanedEventHookLogsWhenClearingExpiredAdminEventsForRealm() {
        RecordingEntityManager recordingEntityManager = new RecordingEntityManager();
        JpaEventStoreProvider provider = new JpaEventStoreProvider(session(), recordingEntityManager.entityManager());

        provider.clearAdmin(realm("realm-1"), 123L);

        assertEquals(3, recordingEntityManager.queries.size());
        assertEquals(
                "delete from EventHookMessageEntity where realmId = :realmId and sourceType = :sourceType and sourceEventId in (select event.id from AdminEventEntity event where event.realmId = :realmId and event.time < :time)",
                recordingEntityManager.queries.get(0).query);
        assertEquals("realm-1", recordingEntityManager.queries.get(0).parameters.get("realmId"));
        assertEquals("ADMIN", recordingEntityManager.queries.get(0).parameters.get("sourceType"));
        assertEquals(Long.valueOf(123L), recordingEntityManager.queries.get(0).parameters.get("time"));
        assertEquals(
                "delete from EventHookLogEntity log where not exists (select 1 from EventHookMessageEntity message where message.executionId = log.executionId)",
                recordingEntityManager.queries.get(1).query);
        assertEquals(
                "delete from AdminEventEntity where realmId = :realmId and time < :time",
                recordingEntityManager.queries.get(2).query);
        assertEquals("realm-1", recordingEntityManager.queries.get(2).parameters.get("realmId"));
        assertEquals(Long.valueOf(123L), recordingEntityManager.queries.get(2).parameters.get("time"));
    }

    private KeycloakSession session() {
        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> null);
    }

    private RealmModel realm(String realmId) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> "getId".equals(method.getName()) ? realmId : null);
    }

    private static final class RecordingEntityManager {
        private final List<RecordedQuery> queries = new ArrayList<>();

        private EntityManager entityManager() {
            return (EntityManager) Proxy.newProxyInstance(
                    EntityManager.class.getClassLoader(),
                    new Class<?>[] { EntityManager.class },
                    (proxy, method, args) -> {
                        if ("createQuery".equals(method.getName()) && args.length >= 1 && args[0] instanceof String queryString) {
                            RecordedQuery recordedQuery = new RecordedQuery(queryString);
                            queries.add(recordedQuery);
                            return recordedQuery.queryProxy();
                        }

                        return null;
                    });
        }
    }

    private static final class RecordedQuery {
        private final String query;
        private final Map<String, Object> parameters = new LinkedHashMap<>();

        private RecordedQuery(String query) {
            this.query = query;
        }

        private Query queryProxy() {
            return (Query) Proxy.newProxyInstance(
                    Query.class.getClassLoader(),
                    new Class<?>[] { Query.class },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setParameter":
                                parameters.put((String) args[0], args[1]);
                                return proxy;
                            case "executeUpdate":
                                return 0;
                            default:
                                return proxy;
                        }
                    });
        }
    }
}
