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

import java.util.Objects;
import java.util.Set;

import org.keycloak.models.cache.infinispan.CacheManager;
import org.keycloak.models.cache.infinispan.authorization.events.AuthorizationCacheInvalidationEvent;
import org.keycloak.models.cache.infinispan.authorization.stream.InResourcePredicate;
import org.keycloak.models.cache.infinispan.authorization.stream.InResourceServerPredicate;
import org.keycloak.models.cache.infinispan.authorization.stream.InScopePredicate;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StoreFactoryCacheManager extends CacheManager {
    private static final Logger logger = Logger.getLogger(StoreFactoryCacheManager.class);

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

    public void resourceServerUpdated(String id, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getResourceServerByClientCacheKey(id));
    }

    public void resourceServerRemoval(String id, Set<String> invalidations) {
        resourceServerUpdated(id, invalidations);

        addInvalidations(InResourceServerPredicate.create(id), invalidations);
    }

    public void scopeUpdated(String id, String name, String serverId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getScopeByNameCacheKey(name, serverId));
        invalidations.add(StoreFactoryCacheSession.getResourceByScopeCacheKey(id, serverId));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByScope(id, serverId));
    }

    public void scopeRemoval(String id, String name, String serverId, Set<String> invalidations) {
        scopeUpdated(id, name, serverId, invalidations);
        addInvalidations(InScopePredicate.create(id), invalidations);
    }

    public void resourceUpdated(String id, String name, String type, Set<String> uris, Set<String> scopes, String serverId, String owner, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getResourceByNameCacheKey(name, owner, serverId));
        invalidations.add(StoreFactoryCacheSession.getResourceByOwnerCacheKey(owner, serverId));
        invalidations.add(StoreFactoryCacheSession.getResourceByOwnerCacheKey(owner, null));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByResource(id, serverId));
        addInvalidations(InResourcePredicate.create(name), invalidations);

        if (type != null) {
            invalidations.add(StoreFactoryCacheSession.getResourceByTypeCacheKey(type, serverId));
            invalidations.add(StoreFactoryCacheSession.getResourceByTypeCacheKey(type, owner, serverId));
            invalidations.add(StoreFactoryCacheSession.getResourceByTypeCacheKey(type, null, serverId));
            invalidations.add(StoreFactoryCacheSession.getResourceByTypeInstanceCacheKey(type, serverId));
            addInvalidations(InResourcePredicate.create(type), invalidations);
        }

        if (uris != null) {
            for (String uri: uris) {
                invalidations.add(StoreFactoryCacheSession.getResourceByUriCacheKey(uri, serverId));
            }
        }

        if (scopes != null) {
            for (String scope : scopes) {
                invalidations.add(StoreFactoryCacheSession.getResourceByScopeCacheKey(scope, serverId));
                addInvalidations(InScopePredicate.create(scope), invalidations);
            }
        }
    }

    public void resourceRemoval(String id, String name, String type, Set<String> uris, String owner, Set<String> scopes, String serverId, Set<String> invalidations) {
        resourceUpdated(id, name, type, uris, scopes, serverId, owner, invalidations);
        addInvalidations(InResourcePredicate.create(id), invalidations);
    }

    public void policyUpdated(String id, String name, Set<String> resources, Set<String> resourceTypes, Set<String> scopes, String serverId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getPolicyByNameCacheKey(name, serverId));

        if (resources != null) {
            for (String resource : resources) {
                invalidations.add(StoreFactoryCacheSession.getPolicyByResource(resource, serverId));
                if (Objects.nonNull(scopes)) {
                    for (String scope : scopes) {
                        invalidations.add(StoreFactoryCacheSession.getPolicyByResourceScope(scope, resource, serverId));
                    }
                }
            }
        }

        if (resourceTypes != null) {
            for (String type : resourceTypes) {
                invalidations.add(StoreFactoryCacheSession.getPolicyByResourceType(type, serverId));
            }
        }

        if (scopes != null) {
            for (String scope : scopes) {
                invalidations.add(StoreFactoryCacheSession.getPolicyByScope(scope, serverId));
                invalidations.add(StoreFactoryCacheSession.getPolicyByResourceScope(scope, null, serverId));
            }
        }
    }

    public void permissionTicketUpdated(String id, String owner, String requester, String resource, String resourceName, String scope, String serverId, Set<String> invalidations) {
        invalidations.add(id);
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByOwner(owner, serverId));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByResource(resource, serverId));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByGranted(requester, serverId));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByGranted(requester, null));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByResourceNameAndGranted(resourceName, requester, serverId));
        invalidations.add(StoreFactoryCacheSession.getPermissionTicketByResourceNameAndGranted(resourceName, requester, null));
        if (scope != null) {
            invalidations.add(StoreFactoryCacheSession.getPermissionTicketByScope(scope, serverId));
        }
    }

    public void policyRemoval(String id, String name, Set<String> resources, Set<String> resourceTypes, Set<String> scopes, String serverId, Set<String> invalidations) {
        policyUpdated(id, name, resources, resourceTypes, scopes, serverId, invalidations);
    }

    public void permissionTicketRemoval(String id, String owner, String requester, String resource, String resourceName, String scope, String serverId, Set<String> invalidations) {
        permissionTicketUpdated(id, owner, requester, resource, resourceName, scope, serverId, invalidations);
    }

}
