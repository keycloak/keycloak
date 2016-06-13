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

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.entities.CachedClientRole;
import org.keycloak.models.cache.infinispan.entities.CachedRealmRole;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel {

    protected RoleModel updated;
    protected CachedRole cached;
    protected RealmCacheSession cacheSession;
    protected RealmModel realm;

    public RoleAdapter(CachedRole cached, RealmCacheSession session, RealmModel realm) {
        this.cached = cached;
        this.cacheSession = session;
        this.realm = realm;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRoleInvalidation(cached.getId());
            updated = cacheSession.getDelegate().getRoleById(cached.getId(), realm);
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    protected boolean invalidated;
    public void invalidate() {
        invalidated = true;
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = cacheSession.getDelegate().getRoleById(cached.getId(), realm);
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }




    @Override
    public String getName() {
        if (isUpdated()) return updated.getName();
        return cached.getName();
    }

    @Override
    public String getDescription() {
        if (isUpdated()) return updated.getDescription();
        return cached.getDescription();
    }

    @Override
    public void setDescription(String description) {
        getDelegateForUpdate();
        updated.setDescription(description);
    }

    @Override
    public boolean isScopeParamRequired() {
        if (isUpdated()) return updated.isScopeParamRequired();
        return cached.isScopeParamRequired();
    }

    @Override
    public void setScopeParamRequired(boolean scopeParamRequired) {
        getDelegateForUpdate();
        updated.setScopeParamRequired(scopeParamRequired);
    }

    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public boolean isComposite() {
        if (isUpdated()) return updated.isComposite();
        return cached.isComposite();
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        getDelegateForUpdate();
        updated.addCompositeRole(role);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        getDelegateForUpdate();
        updated.removeCompositeRole(role);
    }

    @Override
    public Set<RoleModel> getComposites() {
        if (isUpdated()) return updated.getComposites();
        Set<RoleModel> set = new HashSet<RoleModel>();
        for (String id : cached.getComposites()) {
            RoleModel role = realm.getRoleById(id);
            if (role == null) {
                throw new IllegalStateException("Could not find composite in role " + getName() + ": " + id);
            }
            set.add(role);
        }
        return set;
    }

    @Override
    public boolean isClientRole() {
        return cached instanceof CachedClientRole;
    }

    @Override
    public String getContainerId() {
        if (isClientRole()) {
            CachedClientRole appRole = (CachedClientRole)cached;
            return appRole.getClientId();
        } else {
            return realm.getId();
        }
    }


    @Override
    public RoleContainerModel getContainer() {
        if (cached instanceof CachedRealmRole) {
            return realm;
        } else {
            CachedClientRole appRole = (CachedClientRole)cached;
            return realm.getClientById(appRole.getClientId());
        }
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) return true;
        if (!isComposite()) return false;

        Set<RoleModel> visited = new HashSet<RoleModel>();
        return KeycloakModelUtils.searchFor(role, this, visited);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
