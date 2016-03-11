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

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Listener
public class RealmCacheManager extends CacheManager {

    protected static final Logger logger = Logger.getLogger(RealmCacheManager.class);

    public RealmCacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
        super(cache, revisions);
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

    @Override
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
