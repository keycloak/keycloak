package org.keycloak.models.sessions.infinispan.changes;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.infinispan.Cache;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Binary storage isolates application objects without entity cloning or lifecycle guards. */
public class SessionOwnershipTest {
    private DefaultCacheManager manager;
    private Cache<String, SessionEntityWrapper<SessionEntity>> cache;

    @Before
    public void createCache() {
        var global = new GlobalConfigurationBuilder();
        Marshalling.configure(global);
        manager = new DefaultCacheManager(global.build());
        var configuration = new ConfigurationBuilder();
        configuration.encoding().value().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);
        manager.defineConfiguration("sessions", configuration.build());
        cache = manager.getCache("sessions");
    }

    @After
    public void stopCache() {
        manager.stop();
    }

    @Test
    public void putAndGetIsolateEntitiesCollectionsAndMetadata() {
        for (var entity : List.of(new UserSessionEntity("session"), new AuthenticatedClientSessionEntity())) {
            entity.setRealmId("realm");
            var input = new SessionEntityWrapper<SessionEntity>(entity);
            input.getLocalMetadata().put("metadata", "original");
            cache.put("session", input);
            mutate(input);
            var first = cache.get("session");
            var second = cache.get("session");
            assertNotSame(first.getEntity(), second.getEntity());
            assertUnchanged(first);
            mutate(first);
            assertUnchanged(second);
            assertUnchanged(cache.get("session"));
        }
    }

    @Test
    public void asynchronousWritesAndReturnedPreviousValuesAreIsolated() {
        var input = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity("session"));
        input.getEntity().setRealmId("realm");
        input.getLocalMetadata().put("metadata", "original");
        var pending = cache.putAsync("session", input);
        mutate(input);
        pending.join();
        assertUnchanged(cache.get("session"));
        var existing = cache.putIfAbsentAsync("session", input).join();
        mutate(existing);
        assertUnchanged(cache.get("session"));
    }

    @Test
    public void conditionalReplacementAndRemovalUseUuidDespiteChangedExpectedValue() {
        var marker = SessionEntityWrapper.<SessionEntity>createLoadingMarker(new UserSessionEntity("session"));
        cache.put("session", marker);
        marker.getEntity().setRealmId("private-change");
        var value = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity("session"));
        assertTrue(SessionCache.replace(cache, "session", marker, value, 30, TimeUnit.SECONDS));
        assertFalse(SessionCache.remove(cache, "session", marker));
        value.getEntity().setRealmId("private-change");
        assertTrue(SessionCache.remove(cache, "session", value));
        assertFalse(SessionCache.remove(cache, "session", value));
    }

    @Test
    public void failedCasPreservesValueAndExpiration() {
        var value = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity("session"));
        cache.put("session", value, 30, TimeUnit.SECONDS, 20, TimeUnit.SECONDS);
        var before = cache.getAdvancedCache().getCacheEntry("session");
        var wrong = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity("session"));
        assertFalse(SessionCache.replace(cache, "session", wrong, wrong, 300, TimeUnit.SECONDS, 200, TimeUnit.SECONDS));
        assertFalse(SessionCache.remove(cache, "session", wrong));
        var after = cache.getAdvancedCache().getCacheEntry("session");
        assertEquals(value.getVersion(), after.getValue().getVersion());
        assertEquals(before.getLifespan(), after.getLifespan());
        assertEquals(before.getMaxIdle(), after.getMaxIdle());
        assertEquals(before.getCreated(), after.getCreated());
        cache.remove("session");
        assertFalse(SessionCache.replace(cache, "session", value, wrong, 30, TimeUnit.SECONDS));
        assertNull(cache.get("session"));
    }

    @Test
    public void computeReturnsPrivateValues() {
        var old = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity("session"));
        var replacement = new SessionEntityWrapper<SessionEntity>(new UserSessionEntity("session"));
        cache.put("session", old);
        var result = cache.computeIfPresent("session", new ReplaceFunction<>(old.getVersion(), replacement));
        result.getEntity().setRealmId("returned-value-change");
        replacement.getEntity().setRealmId("input-change");
        assertNull(cache.get("session").getEntity().getRealmId());
    }

    private static void mutate(SessionEntityWrapper<SessionEntity> value) {
        value.getEntity().setRealmId("changed");
        value.getLocalMetadata().put("metadata", "changed");
        if (value.getEntity() instanceof UserSessionEntity user) {
            user.getNotes().put("note", "changed");
            user.getClientSessions().add("client");
        } else {
            ((AuthenticatedClientSessionEntity) value.getEntity()).getNotes().put("note", "changed");
        }
    }

    private static void assertUnchanged(SessionEntityWrapper<SessionEntity> value) {
        assertEquals("realm", value.getEntity().getRealmId());
        assertEquals("original", value.getLocalMetadataNote("metadata"));
        if (value.getEntity() instanceof UserSessionEntity user) {
            assertTrue(user.getNotes().isEmpty());
            assertTrue(user.getClientSessions().isEmpty());
        } else {
            assertTrue(((AuthenticatedClientSessionEntity) value.getEntity()).getNotes().isEmpty());
        }
    }
}
