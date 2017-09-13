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

import org.keycloak.authorization.model.CachedModel;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedResource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdapter implements Resource, CachedModel<Resource> {
    protected CachedResource cached;
    protected StoreFactoryCacheSession cacheSession;
    protected Resource updated;

    public ResourceAdapter(CachedResource cached, StoreFactoryCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
    }

    @Override
    public Resource getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), cached.getUri(), cached.getScopesIds(), cached.getResourceServerId(), cached.getOwner());
            updated = cacheSession.getResourceStoreDelegate().findById(cached.getId(), cached.getResourceServerId());
            if (updated == null) throw new IllegalStateException("Not found in database");
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
        updated = cacheSession.getResourceStoreDelegate().findById(cached.getId(), cached.getResourceServerId());
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (isUpdated()) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), name, cached.getType(), cached.getUri(), cached.getScopesIds(), cached.getResourceServerId(), cached.getOwner());
        updated.setName(name);

    }

    @Override
    public String getIconUri() {
        if (isUpdated()) return updated.getIconUri();
        return cached.getIconUri();
    }

    @Override
    public void setIconUri(String iconUri) {
        getDelegateForUpdate();
        updated.setIconUri(iconUri);

    }

    @Override
    public ResourceServer getResourceServer() {
        return cacheSession.getResourceServerStore().findById(cached.getResourceServerId());
    }

    @Override
    public String getUri() {
        if (isUpdated()) return updated.getUri();
        return cached.getUri();
    }

    @Override
    public void setUri(String uri) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), uri, cached.getScopesIds(), cached.getResourceServerId(), cached.getOwner());
        updated.setUri(uri);
    }

    @Override
    public String getType() {
        if (isUpdated()) return updated.getType();
        return cached.getType();
    }

    @Override
    public void setType(String type) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), type, cached.getUri(), cached.getScopesIds(), cached.getResourceServerId(), cached.getOwner());
        updated.setType(type);

    }

    protected List<Scope> scopes;

    @Override
    public List<Scope> getScopes() {
        if (isUpdated()) return updated.getScopes();
        if (scopes != null) return scopes;
        scopes = new LinkedList<>();
        for (String scopeId : cached.getScopesIds()) {
            scopes.add(cacheSession.getScopeStore().findById(scopeId, cached.getResourceServerId()));
        }
        scopes = Collections.unmodifiableList(scopes);
        return scopes;
    }

    @Override
    public String getOwner() {
        if (isUpdated()) return updated.getOwner();
        return cached.getOwner();
    }

    @Override
    public void updateScopes(Set<Scope> scopes) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), cached.getUri(), scopes.stream().map(scope1 -> scope1.getId()).collect(Collectors.toSet()), cached.getResourceServerId(), cached.getOwner());
        updated.updateScopes(scopes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Resource)) return false;

        Resource that = (Resource) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
