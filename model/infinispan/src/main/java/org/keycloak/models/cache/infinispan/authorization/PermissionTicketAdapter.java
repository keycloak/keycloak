/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.authorization.model.CachedModel;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedPermissionTicket;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PermissionTicketAdapter implements PermissionTicket, CachedModel<PermissionTicket> {

    protected CachedPermissionTicket cached;
    protected StoreFactoryCacheSession cacheSession;
    protected PermissionTicket updated;

    public PermissionTicketAdapter(CachedPermissionTicket cached, StoreFactoryCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
    }

    @Override
    public PermissionTicket getDelegateForUpdate() {
        if (updated == null) {
            ResourceServer resourceServer = cacheSession.getResourceServerStoreDelegate().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, cached.getResourceServerId());
            updated = cacheSession.getPermissionTicketStoreDelegate().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, resourceServer, cached.getId());
            if (updated == null) throw new IllegalStateException("Not found in database");
            cacheSession.registerPermissionTicketInvalidation(cached.getId(), cached.getOwner(), cached.getRequester(), cached.getResourceId(), updated.getResource().getName(), cached.getScopeId(), cached.getResourceServerId());
        }
        return updated;
    }

    protected boolean invalidated;

    protected void invalidateFlag() {
        invalidated = true;
    }

    @Override
    public void invalidate() {
        invalidated = true;
        getDelegateForUpdate();
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        ResourceServer resourceServer = cacheSession.getResourceServerStoreDelegate().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, cached.getResourceServerId());
        updated = cacheSession.getPermissionTicketStoreDelegate().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, resourceServer, cached.getId());
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getOwner() {
        if (isUpdated()) return updated.getOwner();
        return cached.getOwner();
    }

    @Override
    public String getRequester() {
        if (isUpdated()) return updated.getRequester();
        return cached.getRequester();
    }

    @Override
    public boolean isGranted() {
        if (isUpdated()) return updated.isGranted();
        return cached.isGranted();
    }

    @Override
    public Long getCreatedTimestamp() {
        if (isUpdated()) return updated.getCreatedTimestamp();
        return cached.getCreatedTimestamp();
    }

    @Override
    public Long getGrantedTimestamp() {
        if (isUpdated()) return updated.getGrantedTimestamp();
        return cached.getGrantedTimestamp();
    }

    @Override
    public void setGrantedTimestamp(Long millis) {
        getDelegateForUpdate();
        cacheSession.registerPermissionTicketInvalidation(cached.getId(), cached.getOwner(), cached.getRequester(), cached.getResourceId(), updated.getResource().getName(), cached.getScopeId(), cached.getResourceServerId());
        updated.setGrantedTimestamp(millis);
    }

    @Override
    public ResourceServer getResourceServer() {
        return cacheSession.getResourceServerStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, cached.getResourceServerId());
    }

    @Override
    public Policy getPolicy() {
        if (isUpdated()) return updated.getPolicy();
        return cacheSession.getPolicyStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, cacheSession.getResourceServerStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, cached.getResourceServerId()), cached.getPolicy());
    }

    @Override
    public void setPolicy(Policy policy) {
        getDelegateForUpdate();
        cacheSession.registerPermissionTicketInvalidation(cached.getId(), cached.getOwner(), cached.getRequester(), cached.getResourceId(), updated.getResource().getName(), cached.getScopeId(), cached.getResourceServerId());
        updated.setPolicy(policy);
    }

    @Override
    public Resource getResource() {
        return cacheSession.getResourceStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, getResourceServer(), cached.getResourceId());
    }

    @Override
    public Scope getScope() {
        return cacheSession.getScopeStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, getResourceServer(), cached.getScopeId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
