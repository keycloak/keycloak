package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class CacheManager {
    protected static final Logger logger = Logger.getLogger(CacheManager.class);
    protected final Cache<String, Long> revisions;
    protected final Cache<String, Revisioned> cache;
    protected final UpdateCounter counter = new UpdateCounter();

    public CacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
        this.cache = cache;
        this.revisions = revisions;
        this.cache.addListener(this);
    }

    public Cache<String, Revisioned> getCache() {
        return cache;
    }

    public long getCurrentCounter() {
        return counter.current();
    }

    public Long getCurrentRevision(String id) {
        Long revision = revisions.get(id);
        if (revision == null) {
            revision = counter.current();
        }
        // if you do cache.remove() on node 1 and the entry doesn't exist on node 2, node 2 never receives a invalidation event
        // so, we do this to force this.
        String invalidationKey = "invalidation.key" + id;
        cache.putForExternalRead(invalidationKey, new AbstractRevisioned(-1L, invalidationKey));
        return revision;
    }

    public void endRevisionBatch() {
        try {
            revisions.endBatch(true);
        } catch (Exception e) {
        }

    }

    public <T> T get(String id, Class<T> type) {
        Revisioned o = (Revisioned)cache.get(id);
        if (o == null) {
            return null;
        }
        Long rev = revisions.get(id);
        if (rev == null) {
            RealmCacheManager.logger.tracev("get() missing rev");
            return null;
        }
        long oRev = o.getRevision() == null ? -1L : o.getRevision().longValue();
        if (rev > oRev) {
            RealmCacheManager.logger.tracev("get() rev: {0} o.rev: {1}", rev.longValue(), oRev);
            return null;
        }
        return o != null && type.isInstance(o) ? type.cast(o) : null;
    }

    public Object invalidateObject(String id) {
        Revisioned removed = (Revisioned)cache.remove(id);
        // if you do cache.remove() on node 1 and the entry doesn't exist on node 2, node 2 never receives a invalidation event
        // so, we do this to force the event.
        cache.remove("invalidation.key" + id);
        bumpVersion(id);
        return removed;
    }

    protected void bumpVersion(String id) {
        long next = counter.next();
        Object rev = revisions.put(id, next);
    }

    public void addRevisioned(Revisioned object, long startupRevision) {
        //startRevisionBatch();
        String id = object.getId();
        try {
            //revisions.getAdvancedCache().lock(id);
            Long rev = revisions.get(id);
            if (rev == null) {
                if (id.endsWith("realm.clients")) RealmCacheManager.logger.trace("addRevisioned rev == null realm.clients");
                rev = counter.current();
                revisions.put(id, rev);
            }
            revisions.startBatch();
            if (!revisions.getAdvancedCache().lock(id)) {
                RealmCacheManager.logger.trace("Could not obtain version lock");
                return;
            }
            rev = revisions.get(id);
            if (rev == null) {
                if (id.endsWith("realm.clients")) RealmCacheManager.logger.trace("addRevisioned rev2 == null realm.clients");
                return;
            }
            if (rev > startupRevision) { // revision is ahead transaction start. Other transaction updated in the meantime. Don't cache
                if (RealmCacheManager.logger.isTraceEnabled()) {
                    RealmCacheManager.logger.tracev("Skipped cache. Current revision {0}, Transaction start revision {1}", object.getRevision(), startupRevision);
                }
                return;
            }
            if (rev.equals(object.getRevision())) {
                if (id.endsWith("realm.clients")) RealmCacheManager.logger.tracev("adding Object.revision {0} rev {1}", object.getRevision(), rev);
                cache.putForExternalRead(id, object);
                return;
            }
            if (rev > object.getRevision()) { // revision is ahead, don't cache
                if (id.endsWith("realm.clients")) RealmCacheManager.logger.trace("addRevisioned revision is ahead realm.clients");
                return;
            }
            // revisions cache has a lower value than the object.revision, so update revision and add it to cache
            if (id.endsWith("realm.clients")) RealmCacheManager.logger.tracev("adding Object.revision {0} rev {1}", object.getRevision(), rev);
            revisions.put(id, object.getRevision());
            cache.putForExternalRead(id, object);
        } finally {
            endRevisionBatch();
        }

    }

    public void clear() {
        cache.clear();
    }

    public void addInvalidations(Predicate<Map.Entry<String, Revisioned>> predicate, Set<String> invalidations) {
        Iterator<Map.Entry<String, Revisioned>> it = getEntryIterator(predicate);
        while (it.hasNext()) {
            invalidations.add(it.next().getKey());
        }
    }

    private Iterator<Map.Entry<String, Revisioned>> getEntryIterator(Predicate<Map.Entry<String, Revisioned>> predicate) {
        return cache
                .entrySet()
                .stream()
                .filter(predicate).iterator();
    }

    @CacheEntryInvalidated
    public void cacheInvalidated(CacheEntryInvalidatedEvent<String, Object> event) {
        if (event.isPre()) {
            String key = event.getKey();
            if (key.startsWith("invalidation.key")) {
                // if you do cache.remove() on node 1 and the entry doesn't exist on node 2, node 2 never receives a invalidation event
                // so, we do this to force this.
                String bump = key.substring("invalidation.key".length());
                RealmCacheManager.logger.tracev("bumping invalidation key {0}", bump);
                bumpVersion(bump);
                return;
            }

        } else {
        //if (!event.isPre()) {
            String key = event.getKey();
            if (key.startsWith("invalidation.key")) {
                // if you do cache.remove() on node 1 and the entry doesn't exist on node 2, node 2 never receives a invalidation event
                // so, we do this to force this.
                String bump = key.substring("invalidation.key".length());
                bumpVersion(bump);
                RealmCacheManager.logger.tracev("bumping invalidation key {0}", bump);
                return;
            }
            bumpVersion(key);
            Object object = event.getValue();
            if (object != null) {
                bumpVersion(key);
                Predicate<Map.Entry<String, Revisioned>> predicate = getInvalidationPredicate(object);
                if (predicate != null) runEvictions(predicate);
                RealmCacheManager.logger.tracev("invalidating: {0}" + object.getClass().getName());
            }
        }
    }

    @CacheEntriesEvicted
    public void cacheEvicted(CacheEntriesEvictedEvent<String, Object> event) {
        if (!event.isPre())
        for (Map.Entry<String, Object> entry : event.getEntries().entrySet()) {
            Object object = entry.getValue();
            bumpVersion(entry.getKey());
            if (object == null) continue;
            RealmCacheManager.logger.tracev("evicting: {0}" + object.getClass().getName());
            Predicate<Map.Entry<String, Revisioned>> predicate = getInvalidationPredicate(object);
            if (predicate != null) runEvictions(predicate);
        }
    }

    public void runEvictions(Predicate<Map.Entry<String, Revisioned>> current) {
        Set<String> evictions = new HashSet<>();
        addInvalidations(current, evictions);
        RealmCacheManager.logger.tracev("running evictions size: {0}", evictions.size());
        for (String key : evictions) {
            cache.evict(key);
            bumpVersion(key);
        }
    }

    protected abstract Predicate<Map.Entry<String, Revisioned>> getInvalidationPredicate(Object object);
}
