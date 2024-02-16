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
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedScope;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ScopeAdapter implements Scope, CachedModel<Scope> {
    protected CachedScope cached;
    protected StoreFactoryCacheSession cacheSession;
    protected Scope updated;

    public ScopeAdapter(CachedScope cached, StoreFactoryCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
    }

    @Override
    public Scope getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerScopeInvalidation(cached.getId(), cached.getName(), cached.getResourceServerId());
            updated = cacheSession.getScopeStoreDelegate().findById(getResourceServer(), cached.getId());
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
        updated = cacheSession.getScopeStoreDelegate().findById(getResourceServer(), cached.getId());
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
        updated.setName(name);

    }

    @Override
    public String getDisplayName() {
        if (isUpdated()) return updated.getDisplayName();
        return cached.getDisplayName();
    }

    @Override
    public void setDisplayName(String name) {
        getDelegateForUpdate();
        updated.setDisplayName(name);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Scope)) return false;

        Scope that = (Scope) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
