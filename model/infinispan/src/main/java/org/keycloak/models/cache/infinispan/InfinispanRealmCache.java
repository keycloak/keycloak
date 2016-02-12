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
import org.jboss.logging.Logger;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedClientTemplate;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanRealmCache implements RealmCache {

    protected static final Logger logger = Logger.getLogger(InfinispanRealmCache.class);

    protected final Cache<String, Object> cache;
    protected final ConcurrentHashMap<String, String> realmLookup;

    public InfinispanRealmCache(Cache<String, Object> cache, ConcurrentHashMap<String, String> realmLookup) {
        this.cache = cache;
        this.realmLookup = realmLookup;
    }

    public Cache<String, Object> getCache() {
        return cache;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public CachedRealm getRealm(String id) {
        return get(id, CachedRealm.class);
    }

    @Override
    public void invalidateRealm(CachedRealm realm) {
        logger.tracev("Invalidating realm {0}", realm.getId());
        cache.remove(realm.getId());
        realmLookup.remove(realm.getName());
    }

    @Override
    public void invalidateRealmById(String id) {
        CachedRealm cached = (CachedRealm) cache.remove(id);
        if (cached != null) realmLookup.remove(cached.getName());
    }

    @Override
    public void addRealm(CachedRealm realm) {
        logger.tracev("Adding realm {0}", realm.getId());
        cache.putForExternalRead(realm.getId(), realm);
        realmLookup.put(realm.getName(), realm.getId());
    }

    @Override
    public CachedRealm getRealmByName(String name) {
        String id = realmLookup.get(name);
        return id != null ? getRealm(id) : null;
    }

    @Override
    public CachedClient getClient(String id) {
        return get(id, CachedClient.class);
    }

    @Override
    public void invalidateClient(CachedClient app) {
        logger.tracev("Removing application {0}", app.getId());
        cache.remove(app.getId());
    }

    @Override
    public void addClient(CachedClient app) {
        logger.tracev("Adding application {0}", app.getId());
        cache.putForExternalRead(app.getId(), app);
    }

    @Override
    public void invalidateClientById(String id) {
        logger.tracev("Removing application {0}", id);
        cache.remove(id);
    }

    @Override
    public void evictClientById(String id) {
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
        cache.remove(role.getId());
    }

    @Override
    public void addGroup(CachedGroup role) {
        logger.tracev("Adding group {0}", role.getId());
        cache.putForExternalRead(role.getId(), role);
    }

    @Override
    public void invalidateGroupById(String id) {
        logger.tracev("Removing group {0}", id);
        cache.remove(id);
    }

    @Override
    public CachedRole getRole(String id) {
        return get(id, CachedRole.class);
    }

    @Override
    public void invalidateRole(CachedRole role) {
        logger.tracev("Removing role {0}", role.getId());
        cache.remove(role.getId());
    }

    @Override
    public void invalidateRoleById(String id) {
        logger.tracev("Removing role {0}", id);
        cache.remove(id);
    }

    @Override
    public void evictRoleById(String id) {
        logger.tracev("Evicting role {0}", id);
        cache.evict(id);
    }

    @Override
    public void addRole(CachedRole role) {
        logger.tracev("Adding role {0}", role.getId());
        cache.putForExternalRead(role.getId(), role);
    }

    private <T> T get(String id, Class<T> type) {
        Object o = cache.get(id);
        return o != null && type.isInstance(o) ? type.cast(o) : null;
    }

    @Override
    public CachedClientTemplate getClientTemplate(String id) {
        return get(id, CachedClientTemplate.class);
    }

    @Override
    public void invalidateClientTemplate(CachedClientTemplate app) {
        logger.tracev("Removing client template {0}", app.getId());
        cache.remove(app.getId());
    }

    @Override
    public void addClientTemplate(CachedClientTemplate app) {
        logger.tracev("Adding client template {0}", app.getId());
        cache.putForExternalRead(app.getId(), app);
    }

    @Override
    public void invalidateClientTemplateById(String id) {
        logger.tracev("Removing client template {0}", id);
        cache.remove(id);
    }

    @Override
    public void evictClientTemplateById(String id) {
        logger.tracev("Evicting client template {0}", id);
        cache.evict(id);
    }


}
