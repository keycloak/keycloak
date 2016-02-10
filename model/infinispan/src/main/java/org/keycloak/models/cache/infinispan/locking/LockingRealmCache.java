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

package org.keycloak.models.cache.infinispan.locking;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedClientTemplate;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;
import org.keycloak.models.cache.infinispan.counter.Revisioned;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LockingRealmCache implements RealmCache {

    protected static final Logger logger = Logger.getLogger(LockingRealmCache.class);

    protected final Cache<String, Long> revisions;
    protected final Cache<String, Object> cache;
    final AtomicLong realmCounter = new AtomicLong();
    final AtomicLong clientCounter = new AtomicLong();
    final AtomicLong clientTemplateCounter = new AtomicLong();
    final AtomicLong roleCounter = new AtomicLong();
    final AtomicLong groupCounter = new AtomicLong();

    protected final ConcurrentHashMap<String, String> realmLookup;

    public LockingRealmCache(Cache<String, Object> cache, Cache<String, Long> revisions, ConcurrentHashMap<String, String> realmLookup) {
        this.cache = cache;
        this.realmLookup = realmLookup;
        this.revisions = revisions;
    }

    public Cache<String, Object> getCache() {
        return cache;
    }

    public Cache<String, Long> getRevisions() {
        return revisions;
    }

    public void startRevisionBatch() {
        revisions.startBatch();
    }

    public void endRevisionBatch() {
        try {
            revisions.endBatch(true);
        } catch (Exception e) {
        }

    }

    private <T> T get(String id, Class<T> type) {
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

    protected Object invalidateObject(String id, AtomicLong counter) {
        Object removed = cache.remove(id);
        revisions.put(id, counter.incrementAndGet());
        return removed;
    }

    protected void addRevisioned(String id, Revisioned object, AtomicLong counter) {
        //startRevisionBatch();
        try {
            //revisions.getAdvancedCache().lock(id);
            Long rev = revisions.get(id);
            if (rev == null) {
                rev = counter.incrementAndGet();
                revisions.put(id, rev);
                return;
            }
            revisions.startBatch();
            revisions.getAdvancedCache().lock(id);
            rev = revisions.get(id);
            if (rev == null) {
                rev = counter.incrementAndGet();
                revisions.put(id, rev);
                return;
            }
            if (rev.equals(object.getRevision())) {
                cache.putForExternalRead(id, object);
            }
        } finally {
            endRevisionBatch();
        }

    }




    public Long getCurrentRevision(String id) {
        return revisions.get(id);
    }
    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public CachedRealm getCachedRealm(String id) {
        return get(id, CachedRealm.class);
    }

    @Override
    public void invalidateCachedRealm(CachedRealm realm) {
        logger.tracev("Invalidating realm {0}", realm.getId());
        invalidateObject(realm.getId(), realmCounter);
        realmLookup.remove(realm.getName());
    }

    @Override
    public void invalidateCachedRealmById(String id) {
        CachedRealm cached = (CachedRealm) invalidateObject(id, realmCounter);
        if (cached != null) realmLookup.remove(cached.getName());
    }

    @Override
    public void addCachedRealm(CachedRealm realm) {
        logger.tracev("Adding realm {0}", realm.getId());
        addRevisioned(realm.getId(), (Revisioned) realm, realmCounter);
        realmLookup.put(realm.getName(), realm.getId());
    }


    @Override
    public CachedRealm getCachedRealmByName(String name) {
        String id = realmLookup.get(name);
        return id != null ? getCachedRealm(id) : null;
    }

    @Override
    public CachedClient getApplication(String id) {
        return get(id, CachedClient.class);
    }

    @Override
    public void invalidateApplication(CachedClient app) {
        logger.tracev("Removing application {0}", app.getId());
        invalidateObject(app.getId(), clientCounter);
    }

    @Override
    public void addCachedClient(CachedClient app) {
        logger.tracev("Adding application {0}", app.getId());
        addRevisioned(app.getId(), (Revisioned) app, clientCounter);
    }

    @Override
    public void invalidateCachedApplicationById(String id) {
        CachedClient client = (CachedClient)invalidateObject(id, clientCounter);
        if (client != null) logger.tracev("Removing application {0}", client.getClientId());
    }

    @Override
    public void evictCachedApplicationById(String id) {
        logger.tracev("Evicting application {0}", id);
        cache.evict(id);
    }

    @Override
    public CachedGroup getGroup(String id) {
        return get(id, CachedGroup.class);
    }

    @Override
    public void invalidateGroup(CachedGroup role) {
        logger.tracev("Removing group {0}", role.getId());
        invalidateObject(role.getId(), groupCounter);
    }

    @Override
    public void addCachedGroup(CachedGroup role) {
        logger.tracev("Adding group {0}", role.getId());
        addRevisioned(role.getId(), (Revisioned) role, groupCounter);
    }

    @Override
    public void invalidateCachedGroupById(String id) {
        logger.tracev("Removing group {0}", id);
        invalidateObject(id, groupCounter);

    }

    @Override
    public void invalidateGroupById(String id) {
        logger.tracev("Removing group {0}", id);
        invalidateObject(id, groupCounter);
    }

    @Override
    public CachedRole getRole(String id) {
        return get(id, CachedRole.class);
    }

    @Override
    public void invalidateRole(CachedRole role) {
        logger.tracev("Removing role {0}", role.getId());
        invalidateObject(role.getId(), roleCounter);
    }

    @Override
    public void invalidateRoleById(String id) {
        logger.tracev("Removing role {0}", id);
        invalidateObject(id, roleCounter);
    }

    @Override
    public void evictCachedRoleById(String id) {
        logger.tracev("Evicting role {0}", id);
        cache.evict(id);
    }

    @Override
    public void addCachedRole(CachedRole role) {
        logger.tracev("Adding role {0}", role.getId());
        addRevisioned(role.getId(), (Revisioned) role, roleCounter);
    }

    @Override
    public void invalidateCachedRoleById(String id) {
        logger.tracev("Removing role {0}", id);
        invalidateObject(id, roleCounter);
    }

    @Override
    public CachedClientTemplate getClientTemplate(String id) {
        return get(id, CachedClientTemplate.class);
    }

    @Override
    public void invalidateClientTemplate(CachedClientTemplate app) {
        logger.tracev("Removing client template {0}", app.getId());
        invalidateObject(app.getId(), clientTemplateCounter);
    }

    @Override
    public void addCachedClientTemplate(CachedClientTemplate app) {
        logger.tracev("Adding client template {0}", app.getId());
        addRevisioned(app.getId(), (Revisioned) app, clientTemplateCounter);
    }

    @Override
    public void invalidateCachedClientTemplateById(String id) {
        logger.tracev("Removing client template {0}", id);
        invalidateObject(id, clientTemplateCounter);
    }

    @Override
    public void evictCachedClientTemplateById(String id) {
        logger.tracev("Evicting client template {0}", id);
        cache.evict(id);
    }


}
