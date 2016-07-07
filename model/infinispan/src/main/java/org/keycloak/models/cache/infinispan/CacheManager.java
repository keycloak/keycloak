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
 *
 * Some notes on how this works:

 * This implementation manages optimistic locking and version checks itself.  The reason is Infinispan just does behave
 * the way we need it to.  Not saying Infinispan is bad, just that we have specific caching requirements!
 *
 * This is an invalidation cache implementation and requires to caches:
 * Cache 1 is an Invalidation Cache
 * Cache 2 is a local-only revision number cache.
 *
 *
 * Each node in the cluster maintains its own revision number cache for each entry in the main invalidation cache.  This revision
 * cache holds the version counter for each cached entity.
 *
 * Cache listeners do not receive a @CacheEntryInvalidated event if that node does not have an entry for that item.  So, consider the following.

 1. Node 1 gets current counter for user.  There currently isn't one as this user isn't cached.
 2. Node 1 reads user from DB
 3. Node 2 updates user
 4. Node 2 calls cache.remove(user).  This does not result in an invalidation listener event to node 1!
 5. node 1 checks version counter, checks pass. Stale entry is cached.

 The issue is that Node 1 doesn't have an entry for the user, so it never receives an invalidation listener event from Node 2 thus it can't bump the version.  So, when node 1 goes to cache the user it is stale as the version number was never bumped.

 So how is this issue fixed?  here is pseudo code:

 1. Node 1 calls cacheManager.getCurrentRevision() to get the current local version counter of that User
 2. Node 1 getCurrentRevision() pulls current counter for that user
 3. Node 1 getCurrentRevision() adds a "invalidation.key.userid" to invalidation cache.  Its just a marker. nothing else
 4. Node 2 update user
 5. Node 2 does a cache.remove(user) cache.remove(invalidation.key.userid)
 6. Node 1 receives invalidation event for invalidation.key.userid. Bumps the version counter for that user
 7. node 1 version check fails, it doesn't cache the user
 *
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
        revisions.clear();
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
