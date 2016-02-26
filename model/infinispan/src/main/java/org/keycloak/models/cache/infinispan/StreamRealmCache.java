/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientTemplate;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRealm;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.stream.ClientQueryPredicate;
import org.keycloak.models.cache.infinispan.stream.ClientTemplateQueryPredicate;
import org.keycloak.models.cache.infinispan.stream.GroupQueryPredicate;
import org.keycloak.models.cache.infinispan.stream.HasRolePredicate;
import org.keycloak.models.cache.infinispan.stream.InClientPredicate;
import org.keycloak.models.cache.infinispan.stream.InRealmPredicate;
import org.keycloak.models.cache.infinispan.stream.RealmQueryPredicate;
import org.keycloak.models.cache.infinispan.stream.RoleQueryPredicate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Listener
public class StreamRealmCache {

    protected static final Logger logger = Logger.getLogger(StreamRealmCache.class);

    protected final Cache<String, Long> revisions;
    protected final Cache<String, Revisioned> cache;

    public StreamRealmCache(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
        this.cache = cache;
        this.cache.addListener(this);
        this.revisions = revisions;
    }

    public Cache<String, Revisioned> getCache() {
        return cache;
    }

    public Cache<String, Long> getRevisions() {
        return revisions;
    }

    public Long getCurrentRevision(String id) {
        Long revision = revisions.get(id);
        if (revision == null) return UpdateCounter.current();
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
            logger.tracev("get() missing rev");
            return null;
        }
        long oRev = o.getRevision() == null ? -1L : o.getRevision().longValue();
        if (rev > oRev) {
            logger.tracev("get() rev: {0} o.rev: {1}", rev.longValue(), oRev);
            return null;
        }
        return o != null && type.isInstance(o) ? type.cast(o) : null;
    }

    public Object invalidateObject(String id) {
        Revisioned removed = (Revisioned)cache.remove(id);
        bumpVersion(id);
        return removed;
    }

    protected void bumpVersion(String id) {
        long next = UpdateCounter.next();
        Object rev = revisions.put(id, next);
    }

    public void addRevisioned(Revisioned object) {
        //startRevisionBatch();
        String id = object.getId();
        try {
            //revisions.getAdvancedCache().lock(id);
            Long rev = revisions.get(id);
            if (rev == null) {
                if (id.endsWith("realm.clients")) logger.trace("addRevisioned rev == null realm.clients");
                rev = UpdateCounter.current();
                revisions.put(id, rev);
            }
            revisions.startBatch();
            if (!revisions.getAdvancedCache().lock(id)) {
                logger.trace("Could not obtain version lock");
            }
            rev = revisions.get(id);
            if (rev == null) {
                if (id.endsWith("realm.clients")) logger.trace("addRevisioned rev2 == null realm.clients");
                return;
            }
            if (rev.equals(object.getRevision())) {
                if (id.endsWith("realm.clients")) logger.tracev("adding Object.revision {0} rev {1}", object.getRevision(), rev);
                cache.putForExternalRead(id, object);
                return;
            }
            if (rev > object.getRevision()) { // revision is ahead, don't cache
                if (id.endsWith("realm.clients")) logger.trace("addRevisioned revision is ahead realm.clients");
                return;
            }
            // revisions cache has a lower value than the object.revision, so update revision and add it to cache
            if (id.endsWith("realm.clients")) logger.tracev("adding Object.revision {0} rev {1}", object.getRevision(), rev);
            revisions.put(id, object.getRevision());
            cache.putForExternalRead(id, object);
        } finally {
            endRevisionBatch();
        }

    }



    public void clear() {
        cache.clear();
    }

    public void realmInvalidation(String id, Set<String> invalidations) {
        Predicate<Map.Entry<String, Revisioned>> predicate = getRealmInvalidationPredicate(id);
        addInvalidations(predicate, invalidations);
    }

    public Predicate<Map.Entry<String, Revisioned>> getRealmInvalidationPredicate(String id) {
        return RealmQueryPredicate.create().realm(id);
    }

    public void clientInvalidation(String id, Set<String> invalidations) {
        addInvalidations(getClientInvalidationPredicate(id), invalidations);
    }

    public Predicate<Map.Entry<String, Revisioned>> getClientInvalidationPredicate(String id) {
        return ClientQueryPredicate.create().client(id);
    }

    public void roleInvalidation(String id, Set<String> invalidations) {
        addInvalidations(getRoleInvalidationPredicate(id), invalidations);

    }

    public Predicate<Map.Entry<String, Revisioned>> getRoleInvalidationPredicate(String id) {
        return HasRolePredicate.create().role(id);
    }

    public void groupInvalidation(String id, Set<String> invalidations) {
        addInvalidations(getGroupInvalidationPredicate(id), invalidations);

    }

    public Predicate<Map.Entry<String, Revisioned>> getGroupInvalidationPredicate(String id) {
        return GroupQueryPredicate.create().group(id);
    }

    public void clientTemplateInvalidation(String id, Set<String> invalidations) {
        addInvalidations(getClientTemplateInvalidationPredicate(id), invalidations);

    }

    public Predicate<Map.Entry<String, Revisioned>> getClientTemplateInvalidationPredicate(String id) {
        return ClientTemplateQueryPredicate.create().template(id);
    }

    public void realmRemoval(String id, Set<String> invalidations) {
        Predicate<Map.Entry<String, Revisioned>> predicate = getRealmRemovalPredicate(id);
        addInvalidations(predicate, invalidations);
    }

    public Predicate<Map.Entry<String, Revisioned>> getRealmRemovalPredicate(String id) {
        Predicate<Map.Entry<String, Revisioned>> predicate = null;
        predicate = RealmQueryPredicate.create().realm(id)
                .or(InRealmPredicate.create().realm(id));
        return predicate;
    }

    public void clientAdded(String realmId, String id, Set<String> invalidations) {
        addInvalidations(getClientAddedPredicate(realmId), invalidations);
    }

    public Predicate<Map.Entry<String, Revisioned>> getClientAddedPredicate(String realmId) {
        return ClientQueryPredicate.create().inRealm(realmId);
    }

    public void clientRemoval(String realmId, String id, Set<String> invalidations) {
        Predicate<Map.Entry<String, Revisioned>> predicate = null;
        predicate = getClientRemovalPredicate(realmId, id);
        addInvalidations(predicate, invalidations);
    }

    public Predicate<Map.Entry<String, Revisioned>> getClientRemovalPredicate(String realmId, String id) {
        Predicate<Map.Entry<String, Revisioned>> predicate;
        predicate = ClientQueryPredicate.create().inRealm(realmId)
                .or(ClientQueryPredicate.create().client(id))
                .or(InClientPredicate.create().client(id));
        return predicate;
    }

    public void roleRemoval(String id, Set<String> invalidations) {
        addInvalidations(getRoleRemovalPredicate(id), invalidations);

    }

    public Predicate<Map.Entry<String, Revisioned>> getRoleRemovalPredicate(String id) {
        return getRoleInvalidationPredicate(id);
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
        if (!event.isPre()) {
            bumpVersion(event.getKey());
            Object object = event.getValue();
            if (object != null) {
                bumpVersion(event.getKey());
                Predicate<Map.Entry<String, Revisioned>> predicate = getInvalidationPredicate(object);
                if (predicate != null) runEvictions(predicate);
                logger.tracev("invalidating: {0}" + object.getClass().getName());
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
            logger.tracev("evicting: {0}" + object.getClass().getName());
            Predicate<Map.Entry<String, Revisioned>> predicate = getInvalidationPredicate(object);
            if (predicate != null) runEvictions(predicate);
        }
    }

    public void runEvictions(Predicate<Map.Entry<String, Revisioned>> current) {
        Set<String> evictions = new HashSet<>();
        addInvalidations(current, evictions);
        logger.tracev("running evictions size: {0}", evictions.size());
        for (String key : evictions) {
            cache.evict(key);
            bumpVersion(key);
        }
    }

    protected Predicate<Map.Entry<String, Revisioned>> getInvalidationPredicate(Object object) {
        if (object instanceof CachedRealm) {
            CachedRealm cached = (CachedRealm)object;
            return getRealmRemovalPredicate(cached.getId());
        } else if (object instanceof CachedClient) {
            CachedClient cached = (CachedClient)object;
            Predicate<Map.Entry<String, Revisioned>> predicate = getClientRemovalPredicate(cached.getRealm(), cached.getId());
            return predicate;
        } else if (object instanceof CachedRole) {
            CachedRole cached = (CachedRole)object;
            return getRoleRemovalPredicate(cached.getId());
        } else if (object instanceof CachedGroup) {
            CachedGroup cached = (CachedGroup)object;
            return getGroupInvalidationPredicate(cached.getId());
        } else if (object instanceof CachedClientTemplate) {
            CachedClientTemplate cached = (CachedClientTemplate)object;
            return getClientTemplateInvalidationPredicate(cached.getId());
        }
        return null;
    }
}
