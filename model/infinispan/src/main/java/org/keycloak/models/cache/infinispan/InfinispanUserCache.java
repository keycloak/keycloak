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
import org.keycloak.models.cache.infinispan.entities.CachedUser;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserCache implements UserCache {

    protected static final Logger logger = Logger.getLogger(InfinispanUserCache.class);

    protected volatile boolean enabled = true;

    protected final Cache<String, CachedUser> cache;

    protected final InfinispanCacheUserProviderFactory.RealmLookup usernameLookup;

    protected final InfinispanCacheUserProviderFactory.RealmLookup emailLookup;

    public InfinispanUserCache(Cache<String, CachedUser> cache, InfinispanCacheUserProviderFactory.RealmLookup usernameLookup, InfinispanCacheUserProviderFactory.RealmLookup emailLookup) {
        this.cache = cache;
        this.usernameLookup = usernameLookup;
        this.emailLookup = emailLookup;
    }

    @Override
    public CachedUser getCachedUser(String realmId, String id) {
        if (realmId == null || id == null) return null;
        CachedUser user = cache.get(id);
        return user != null && realmId.equals(user.getRealm()) ? user : null;
    }

    @Override
    public void invalidateCachedUserById(String realmId, String id) {
        logger.tracev("Invalidating user {0}", id);
        cache.remove(id);
    }

    @Override
    public void addCachedUser(String realmId, CachedUser user) {
        logger.tracev("Adding user {0}", user.getId());
        cache.putForExternalRead(user.getId(), user);
    }

    @Override
    public CachedUser getCachedUserByUsername(String realmId, String name) {
        String id = usernameLookup.get(realmId, name);
        return id != null ? getCachedUser(realmId, id) : null;
    }

    @Override
    public CachedUser getCachedUserByEmail(String realmId, String email) {
        String id = emailLookup.get(realmId, email);
        return id != null ? getCachedUser(realmId, id) : null;
    }

    @Override
    public void invalidateRealmUsers(String realmId) {
        logger.tracev("Invalidating users for realm {0}", realmId);

        cache.clear();
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
