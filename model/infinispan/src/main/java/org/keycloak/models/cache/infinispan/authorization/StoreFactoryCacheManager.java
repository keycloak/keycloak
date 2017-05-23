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
package org.keycloak.models.cache.infinispan.authorization;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.CacheManager;
import org.keycloak.models.cache.infinispan.RealmCacheManager;
import org.keycloak.models.cache.infinispan.authorization.events.AuthorizationCacheInvalidationEvent;
import org.keycloak.models.cache.infinispan.authorization.stream.InResourcePredicate;
import org.keycloak.models.cache.infinispan.authorization.stream.InResourceServerPredicate;
import org.keycloak.models.cache.infinispan.authorization.stream.InScopePredicate;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StoreFactoryCacheManager extends CacheManager {
    private static final Logger logger = Logger.getLogger(RealmCacheManager.class);

    public StoreFactoryCacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
        super(cache, revisions);
    }
    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void addInvalidationsFromEvent(InvalidationEvent event, Set<String> invalidations) {
        if (event instanceof AuthorizationCacheInvalidationEvent) {
            invalidations.add(event.getId());

            ((AuthorizationCacheInvalidationEvent) event).addInvalidations(this, invalidations);
        }
    }

    public void resourceServerUpdated(String id, String clientId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getResourceServerByClientCacheKey(clientId));
    }

    public void resourceServerRemoval(String id, String name, Set<String> invalidations) {
        resourceServerUpdated(id, name, invalidations);

        addInvalidations(InResourceServerPredicate.create().resourceServer(id), invalidations);
    }

    public void scopeUpdated(String id, String name, String serverId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getScopeByNameCacheKey(name, serverId));
        invalidations.add(StoreFactoryCacheSession.getResourceByScopeCacheKey(id, serverId));
    }

    public void scopeRemoval(String id, String name, String serverId, Set<String> invalidations) {
        scopeUpdated(id, name, serverId, invalidations);
        addInvalidations(InScopePredicate.create().scope(id), invalidations);
    }

    public void resourceUpdated(String id, String name, String type, String uri, Set<String> scopes, String serverId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getResourceByNameCacheKey(name, serverId));

        if (type != null) {
            invalidations.add(StoreFactoryCacheSession.getResourceByTypeCacheKey(type, serverId));
            addInvalidations(InResourcePredicate.create().resource(type), invalidations);
        }

        if (uri != null) {
            invalidations.add(StoreFactoryCacheSession.getResourceByUriCacheKey(uri, serverId));
        }

        if (scopes != null) {
            for (String scope : scopes) {
                invalidations.add(StoreFactoryCacheSession.getResourceByScopeCacheKey(scope, serverId));
                addInvalidations(InScopePredicate.create().scope(scope), invalidations);
            }
        }
    }

    public void resourceRemoval(String id, String name, String type, String uri, String owner, Set<String> scopes, String serverId, Set<String> invalidations) {
        resourceUpdated(id, name, type, uri, scopes, serverId, invalidations);
        invalidations.add(StoreFactoryCacheSession.getResourceByOwnerCacheKey(owner, serverId));
        addInvalidations(InResourcePredicate.create().resource(id), invalidations);
    }

    public void policyUpdated(String id, String name, Set<String> resources, String serverId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getPolicyByNameCacheKey(name, serverId));

        if (resources != null) {
            for (String resource : resources) {
                invalidations.add(StoreFactoryCacheSession.getPolicyByResource(resource, serverId));
            }
        }
    }

    public void policyRemoval(String id, String name, Set<String> resources, String serverId, Set<String> invalidations) {
        policyUpdated(id, name, resources, serverId, invalidations);
    }


}
