package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.logging.Logger;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

@Listener
public class InfinispanUserSessionListener {

    public static final Logger logger = Logger.getLogger(InfinispanUserSessionListener.class);

    private final Cache<String, Set<String>> sessionIdsCache;

    public InfinispanUserSessionListener(Cache<String, Set<String>> sessionIdsCache) {
        this.sessionIdsCache = sessionIdsCache;
    }

    @CacheEntryCreated
    public void listen(CacheEntryCreatedEvent<String, SessionEntityWrapper<UserSessionEntity>> event) {
        if (event.isPre()) {
            return;
        }

        final String userId = event.getValue().getEntity().getUser();
        final Set<String> sessionIds = sessionIdsCache.getOrDefault(userId, new HashSet<>());
        sessionIds.add(event.getKey());
        sessionIdsCache.put(userId, sessionIds);
    }

    @CacheEntryRemoved
    public CompletionStage<Void> listen(CacheEntryRemovedEvent<String, SessionEntityWrapper<UserSessionEntity>> event) {
        if (!event.isPre()) {
            return null;
        }

        final String userId = event.getValue().getEntity().getUser();
        final Set<String> sessionIds = sessionIdsCache.get(userId);

        if (sessionIds == null) {
            return null;
        }

        final Set<String> newSessionIds = new HashSet<>(sessionIds);
        newSessionIds.remove(event.getKey());

        if (newSessionIds.isEmpty()) {
            if (!sessionIdsCache.remove(userId, sessionIds)) {
                logger.warnf("Failed to remove session ids for user '%s' in cache '%s'", userId, sessionIdsCache.getName());
            }
        } else if (!sessionIdsCache.replace(userId, sessionIds, newSessionIds)) {
            logger.warnf("Failed to replace session ids for user '%s' in cache '%s'", userId, sessionIdsCache.getName());
        }

        return null;
    }

}
