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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.models.cache.infinispan.events.RealmCacheInvalidationEvent;
import org.keycloak.models.cache.infinispan.stream.GroupListPredicate;
import org.keycloak.models.cache.infinispan.stream.HasRolePredicate;
import org.keycloak.models.cache.infinispan.stream.InClientPredicate;
import org.keycloak.models.cache.infinispan.stream.InGroupPredicate;
import org.keycloak.models.cache.infinispan.stream.InRealmPredicate;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmCacheManager extends CacheManager {

    private static final Logger logger = Logger.getLogger(RealmCacheManager.class);

    private final ConcurrentHashMap<String, ReentrantLock> cacheInteractions = new ConcurrentHashMap<>();

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public RealmCacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
        super(cache, revisions);
    }


    public void realmUpdated(String id, String name, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(RealmCacheSession.getRealmByNameCacheKey(name));
    }

    public void realmRemoval(String id, String name, Set<String> invalidations) {
        realmUpdated(id, name, invalidations);

        addInvalidations(InRealmPredicate.create().realm(id), invalidations);
    }

    public void roleAdded(String roleContainerId, String roleName, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getRolesCacheKey(roleContainerId));
        invalidations.add(RealmCacheSession.getRoleByNameCacheKey(roleContainerId, roleName));
    }

    public void roleUpdated(String roleContainerId, String roleName, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getRoleByNameCacheKey(roleContainerId, roleName));
    }

    public void roleRemoval(String id, String roleName, String roleContainerId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getRolesCacheKey(roleContainerId));
        invalidations.add(RealmCacheSession.getRoleByNameCacheKey(roleContainerId, roleName));

        addInvalidations(HasRolePredicate.create().role(id), invalidations);
    }

    public void clientScopeAdded(String realmId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getClientScopesCacheKey(realmId));
    }

    public void clientScopeUpdated(String realmId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getClientScopesCacheKey(realmId));
    }

    public void clientScopeRemoval(String realmId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getClientScopesCacheKey(realmId));
        addInvalidations(InRealmPredicate.create().realm(realmId), invalidations);
    }

    public void groupQueriesInvalidations(String realmId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getGroupsQueryCacheKey(realmId));
        addInvalidations(GroupListPredicate.create().realm(realmId), invalidations);
    }

    public void groupNameInvalidations(String groupId, Set<String> invalidations) {
        addInvalidations(InGroupPredicate.create().group(groupId), invalidations);
    }

    public void clientAdded(String realmId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getRealmClientsQueryCacheKey(realmId));
    }

    public void clientUpdated(String realmId, String clientUuid, String clientId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getClientByClientIdCacheKey(clientId, realmId));
        invalidations.add(RealmCacheSession.getClientScopesCacheKey(clientUuid, true));
        invalidations.add(RealmCacheSession.getClientScopesCacheKey(clientUuid, false));
    }

    // Client roles invalidated separately
    public void clientRemoval(String realmId, String clientUUID, String clientId, Set<String> invalidations) {
        invalidations.add(RealmCacheSession.getRealmClientsQueryCacheKey(realmId));
        invalidations.add(RealmCacheSession.getClientByClientIdCacheKey(clientId, realmId));

        addInvalidations(InClientPredicate.create().client(clientUUID), invalidations);
    }

    @Override
    protected void addInvalidationsFromEvent(InvalidationEvent event, Set<String> invalidations) {
        invalidations.add(event.getId());

        ((RealmCacheInvalidationEvent) event).addInvalidations(this, invalidations);
    }

    /**
     * Compute a cached realm and ensure that this happens only once with the current Keycloak instance.
     * Use this to avoid concurrent preparations of a realm in parallel threads. This helps to break the load on
     * a stampede after a server has started, were a lot of requests come in for the same realm that hasn't been cached yet.
     * Instead of each request loading the realm in parallel, this lets the first request load the realm, and all
     * other requests will use the cached realm, which is much more efficient.
     */
    public <T> T computeSerialized(KeycloakSession session, String id, BiFunction<String, KeycloakSession, T> compute) {
        // this locking is only to ensure that if there is a computation for the same id in the "synchronized" block below,
        // it will have the same object instance to lock the current execution until the other is finished.
        ReentrantLock lock = cacheInteractions.computeIfAbsent(id, s -> new ReentrantLock());
        try {
            lock.lock();
            // in case the previous thread has removed the entry in the finally block
            ReentrantLock existingLock = cacheInteractions.putIfAbsent(id, lock);
            if (existingLock != lock) {
                logger.debugf("Concurrent execution detected for realm '%s'.", id);
            }

            return compute.apply(id, session);
        } finally {
            lock.unlock();
            cacheInteractions.remove(id, lock);
        }
    }
}
