package org.keycloak.partialimport;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;
import org.keycloak.events.hooks.EventHookStoreProvider;
import org.keycloak.events.hooks.EventHookTargetModel;
import org.keycloak.events.hooks.EventHookTargetProvider;
import org.keycloak.events.hooks.HttpEventHookTargetProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventHookTargetRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventHookTargetsPartialImportTest {

    @Test
    public void shouldImportEventHookTarget() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        EventHookTargetsPartialImport partialImport = new EventHookTargetsPartialImport();
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setIfResourceExists("FAIL");
        rep.setEventHookTargets(List.of(targetRepresentation("target-1", "Hook A")));

        partialImport.prepare(rep, realm("realm-1"), session(store));
        PartialImportResults results = partialImport.doImport(rep, realm("realm-1"), session(store));

        assertEquals(1, store.created.size());
        assertEquals("target-1", store.created.get(0).getId());
        assertEquals(1, results.getAdded());
        assertEquals(ResourceType.EVENT_HOOK_TARGET, results.getResults().iterator().next().getResourceType());
    }

    @Test
    public void shouldOverwriteExistingTargetMatchedByName() {
        RecordingStoreProvider store = new RecordingStoreProvider();
        EventHookTargetModel existing = new EventHookTargetModel();
        existing.setId("existing-1");
        existing.setRealmId("realm-1");
        existing.setName("Hook A");
        existing.setType(HttpEventHookTargetProviderFactory.ID);
        existing.setEnabled(true);
        existing.setCreatedAt(10L);
        existing.setUpdatedAt(20L);
        existing.setSettings(Map.of("url", "https://old.example.org"));
        store.targets.add(existing);

        EventHookTargetsPartialImport partialImport = new EventHookTargetsPartialImport();
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setIfResourceExists("OVERWRITE");
        rep.setEventHookTargets(List.of(targetRepresentation(null, "Hook A")));

        partialImport.prepare(rep, realm("realm-1"), session(store));
        partialImport.removeOverwrites(realm("realm-1"), session(store));
        PartialImportResults results = partialImport.doImport(rep, realm("realm-1"), session(store));

        assertEquals(List.of("existing-1"), store.deletedIds);
        assertEquals(1, store.created.size());
        assertNotNull(store.created.get(0).getId());
        assertEquals(1, results.getOverwritten());
    }

    @Test
    public void shouldImportUnknownTargetType() {
        RecordingStoreProvider store = new RecordingStoreProvider(false);
        EventHookTargetsPartialImport partialImport = new EventHookTargetsPartialImport();
        PartialImportRepresentation rep = new PartialImportRepresentation();
        rep.setIfResourceExists("FAIL");

        EventHookTargetRepresentation target = new EventHookTargetRepresentation();
        target.setId("target-unknown");
        target.setName("Unknown Hook");
        target.setType("custom-missing");
        target.setEnabled(true);
        target.setSettings(Map.of("apiKey", "value-1"));
        rep.setEventHookTargets(List.of(target));

        partialImport.prepare(rep, realm("realm-1"), session(store));
        partialImport.doImport(rep, realm("realm-1"), session(store));

        assertEquals(1, store.created.size());
        assertEquals("custom-missing", store.created.get(0).getType());
        assertEquals("value-1", store.created.get(0).getSettings().get("apiKey"));
    }

    private EventHookTargetRepresentation targetRepresentation(String id, String name) {
        EventHookTargetRepresentation representation = new EventHookTargetRepresentation();
        representation.setId(id);
        representation.setName(name);
        representation.setType(HttpEventHookTargetProviderFactory.ID);
        representation.setEnabled(true);
        representation.setCreatedAt(100L);
        representation.setUpdatedAt(200L);
        representation.setSettings(Map.of("url", "https://example.org/hooks"));
        return representation;
    }

    private RealmModel realm(String id) {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> {
                    if ("getId".equals(method.getName())) {
                        return id;
                    }
                    return null;
                });
    }

    private KeycloakSession session(RecordingStoreProvider store) {
        HttpEventHookTargetProviderFactory factory = store.withFactory ? new HttpEventHookTargetProviderFactory() : null;
        KeycloakSessionFactory sessionFactory = (KeycloakSessionFactory) Proxy.newProxyInstance(
                KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[] { KeycloakSessionFactory.class },
                (proxy, method, args) -> {
                    if ("getProviderFactory".equals(method.getName())
                            && args.length == 2
                            && args[0] == EventHookTargetProvider.class
                            && factory != null
                            && HttpEventHookTargetProviderFactory.ID.equals(args[1])) {
                        return factory;
                    }
                    return null;
                });

        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
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

        private final boolean withFactory;
        private final List<EventHookTargetModel> targets = new ArrayList<>();
        private final List<EventHookTargetModel> created = new ArrayList<>();
        private final List<String> deletedIds = new ArrayList<>();

        private RecordingStoreProvider() {
            this(true);
        }

        private RecordingStoreProvider(boolean withFactory) {
            this.withFactory = withFactory;
        }

        @Override
        public Stream<EventHookTargetModel> getTargetsStream(String realmId, Boolean enabled) {
            return targets.stream().filter(target -> realmId.equals(target.getRealmId()));
        }

        @Override
        public EventHookTargetModel getTarget(String realmId, String targetId) {
            return targets.stream()
                    .filter(target -> realmId.equals(target.getRealmId()) && targetId.equals(target.getId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public EventHookTargetModel createTarget(EventHookTargetModel target) {
            targets.add(target);
            created.add(target);
            return target;
        }

        @Override
        public EventHookTargetModel updateTarget(EventHookTargetModel target) {
            return target;
        }

        @Override
        public boolean deleteTarget(String realmId, String targetId) {
            deletedIds.add(targetId);
            return targets.removeIf(target -> realmId.equals(target.getRealmId()) && targetId.equals(target.getId()));
        }

        @Override
        public java.util.Map<String, org.keycloak.events.hooks.EventHookTargetStatus> getLatestTargetStatuses(String realmId) {
            return Map.of();
        }

        @Override
        public void close() {
        }

        @Override
        public void createMessages(List<org.keycloak.events.hooks.EventHookMessageModel> messages) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.keycloak.events.hooks.EventHookMessageModel updateMessage(org.keycloak.events.hooks.EventHookMessageModel message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.keycloak.events.hooks.EventHookMessageModel getMessage(String realmId, String messageId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<org.keycloak.events.hooks.EventHookMessageModel> claimAvailableMessages(int maxResults, long now, long staleClaimBefore, String claimOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<org.keycloak.events.hooks.EventHookMessageModel> claimAvailableMessagesForTarget(String realmId, String targetId,
                int maxResults, long now, long staleClaimBefore, String claimOwner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasAvailableMessages(String realmId, String targetId, long now, long staleClaimBefore) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createLog(org.keycloak.events.hooks.EventHookLogModel log) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<org.keycloak.events.hooks.EventHookMessageModel> getMessagesStream(String realmId, String status, String targetId, Integer first, Integer max) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<org.keycloak.events.hooks.EventHookLogModel> getLogsStream(String realmId, String messageId, String targetId,
                String targetType, String sourceType, String event, String client, String user, String ipAddress,
                String resourceType, String resourcePath, String status, String messageStatus,
                Long dateFrom, Long dateTo, String executionId, String search, Integer first, Integer max) {
            throw new UnsupportedOperationException();
        }
    }
}
