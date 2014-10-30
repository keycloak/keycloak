package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.cache.CacheUserProviderFactory;
import org.keycloak.models.cache.DefaultCacheUserProvider;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheUserProviderFactory implements CacheUserProviderFactory {

    protected InfinispanUserCache userCache;

    protected final RealmLookup usernameLookup = new RealmLookup();

    protected final RealmLookup emailLookup = new RealmLookup();

    // Method CacheEntryCreatedEvent.getValue is available from ispn 6 (EAP6 and AS7 are on ispn 5)
    private boolean isNewInfinispan;

    @Override
    public CacheUserProvider create(KeycloakSession session) {
        lazyInit(session);
        return new DefaultCacheUserProvider(userCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (userCache == null) {
            synchronized (this) {
                if (userCache == null) {
                    checkIspnVersion();
                    Cache<String, CachedUser> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_CACHE_NAME);
                    cache.addListener(new CacheListener());
                    userCache = new InfinispanUserCache(cache, usernameLookup, emailLookup);
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
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }

    @Listener
    public class CacheListener {

        @CacheEntryCreated
        public void userCreated(CacheEntryCreatedEvent<String, CachedUser> event) {
            if (!event.isPre()) {

                CachedUser cachedUser;

                // Try optimized version if available
                if (isNewInfinispan) {
                    cachedUser = event.getValue();
                } else {
                    String userId = event.getKey();
                    cachedUser = event.getCache().get(userId);
                }

                if (cachedUser != null) {
                    String realm = cachedUser.getRealm();
                    usernameLookup.put(realm, cachedUser.getUsername(), cachedUser.getId());
                    if (cachedUser.getEmail() != null) {
                        emailLookup.put(realm, cachedUser.getEmail(), cachedUser.getId());
                    }
                }
            }
        }

        @CacheEntryRemoved
        public void userRemoved(CacheEntryRemovedEvent<String, CachedUser> event) {
            if (event.isPre() && event.getValue() != null) {
                CachedUser cachedUser = event.getValue();
                String realm = cachedUser.getRealm();
                usernameLookup.remove(realm, cachedUser.getUsername());
                if (cachedUser.getEmail() != null) {
                    emailLookup.remove(realm, cachedUser.getEmail());
                }
            }
        }

    }

    static class RealmLookup {

        protected final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> lookup = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

        public void put(String realm, String key, String value) {
            ConcurrentHashMap<String, String> map = lookup.get(realm);
            if(map == null) {
                map = new ConcurrentHashMap<String, String>();
                ConcurrentHashMap<String, String> p = lookup.putIfAbsent(realm, map);
                if (p != null) {
                    map = p;
                }
            }
            map.put(key, value);
        }

        public String get(String realm, String key) {
            ConcurrentHashMap<String, String> map = lookup.get(realm);
            return map != null ? map.get(key) : null;
        }

        public void remove(String realm, String key) {
            ConcurrentHashMap<String, String> map = lookup.get(realm);
            if (map != null) {
                map.remove(key);
                if (map.isEmpty()) {
                    lookup.remove(realm);
                }
            }
        }

    }

}
