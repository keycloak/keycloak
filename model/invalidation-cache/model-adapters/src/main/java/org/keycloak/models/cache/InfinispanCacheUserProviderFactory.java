package org.keycloak.models.cache;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheUserProviderFactory implements CacheUserProviderFactory {

    protected InfinispanUserCache userCache;
    protected final ConcurrentHashMap<String, String> usernameLookup = new ConcurrentHashMap<String, String>();
    protected final ConcurrentHashMap<String, String> emailLookup = new ConcurrentHashMap<String, String>();

    @Override
    public CacheUserProvider create(KeycloakSession session) {
        lazyInit(session);
        return new InfinispanCacheUserProvider(userCache, session);
    }

    private void lazyInit(KeycloakSession session) {
        if (userCache == null) {
            synchronized (this) {
                if (userCache == null) {
                    Cache<String, CachedUser> cache = session.getProvider(InfinispanConnectionProvider.class).getCache("users");
                    cache.addListener(new CacheListener());
                    userCache = new InfinispanUserCache(cache, usernameLookup, emailLookup);
                }
            }
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
    private class CacheListener {

        @CacheEntryCreated
        public void userCreated(CacheEntryCreatedEvent<String, CachedUser> event) {
            if (!event.isPre()) {
                CachedUser cachedUser = event.getValue();
                usernameLookup.put(cachedUser.getUsername(), cachedUser.getId());
                if (cachedUser.getEmail() != null) {
                    emailLookup.put(cachedUser.getEmail(), cachedUser.getId());
                }
            }
        }

        @CacheEntryRemoved
        public void userRemoved(CacheEntryRemovedEvent<String, CachedUser> event) {
            if (event.isPre()) {
                CachedUser cachedUser = event.getValue();
                usernameLookup.remove(cachedUser.getUsername());
                if (cachedUser.getEmail() != null) {
                    emailLookup.remove(cachedUser.getEmail());
                }
            }
        }

    }

}
