package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheRealmProviderFactory;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheRealmProviderFactory implements CacheRealmProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanCacheRealmProviderFactory.class);

    protected volatile InfinispanRealmCache realmCache;

    protected final ConcurrentHashMap<String, String> realmLookup = new ConcurrentHashMap<>();

    private boolean isNewInfinispan;

    @Override
    public CacheRealmProvider create(KeycloakSession session) {
        lazyInit(session);
        return new DefaultCacheRealmProvider(realmCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (realmCache == null) {
            synchronized (this) {
                if (realmCache == null) {
                    checkIspnVersion();

                    Cache<String, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
                    cache.addListener(new CacheListener());
                    realmCache = new InfinispanRealmCache(cache, realmLookup);
                }
            }
        }
    }

    protected void checkIspnVersion() {
        try {
            CacheEntryCreatedEvent.class.getMethod("getValue");
            isNewInfinispan = true;
        } catch (NoSuchMethodException nsme) {
            isNewInfinispan = false;
        }
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

    @Override
    public String getId() {
        return "infinispan";
    }

    @Listener
    public class CacheListener {

        @CacheEntryCreated
        public void created(CacheEntryCreatedEvent<String, Object> event) {
            if (!event.isPre()) {
                Object object;

                // Try optimized version if available
                if (isNewInfinispan) {
                    object = event.getValue();
                } else {
                    String id = event.getKey();
                    object = event.getCache().get(id);
                }

                if (object != null) {
                    if (object instanceof CachedRealm) {
                        CachedRealm realm = (CachedRealm) object;
                        realmLookup.put(realm.getName(), realm.getId());
                        log.tracev("Realm added realm={0}", realm.getName());
                    }
                }
            }
        }

        @CacheEntryRemoved
        public void removed(CacheEntryRemovedEvent<String, Object> event) {
            if (event.isPre()) {
                Object object = event.getValue();
                if (object != null) {
                    remove(object);
                }
            }
        }

        @CacheEntryInvalidated
        public void removed(CacheEntryInvalidatedEvent<String, Object> event) {
            if (event.isPre()) {
                Object object = event.getValue();
                if (object != null) {
                    remove(object);
                }
            }
        }

        @CacheEntriesEvicted
        public void userEvicted(CacheEntriesEvictedEvent<String, Object> event) {
            for (Object object : event.getEntries().values()) {
                remove(object);
            }
        }

        private void remove(Object object) {
            if (object instanceof CachedRealm) {
                CachedRealm realm = (CachedRealm) object;

                realmLookup.remove(realm.getName());

                for (String c : realm.getClients().values()) {
                    realmCache.evictCachedApplicationById(c);
                }

                log.tracev("Realm removed realm={0}", realm.getName());
            }
        }
    }
}
