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
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedResource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceAdapter implements Resource, CachedModel<Resource> {

    private final Supplier<Resource> modelSupplier;
    protected final CachedResource cached;
    protected final StoreFactoryCacheSession cacheSession;
    protected Resource updated;

    public ResourceAdapter(CachedResource cached, StoreFactoryCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.modelSupplier = this::getResourceModel;
    }

    @Override
    public Resource getDelegateForUpdate() {
        if (updated == null) {
            updated = modelSupplier.get();
            cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), cached.getUris(modelSupplier), cached.getScopesIds(modelSupplier), cached.getResourceServerId(), cached.getOwner());
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
        updated = cacheSession.getResourceStoreDelegate().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, getResourceServer(), cached.getId());
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
        cacheSession.registerResourceInvalidation(cached.getId(), name, cached.getType(), cached.getUris(modelSupplier), cached.getScopesIds(modelSupplier), cached.getResourceServerId(), cached.getOwner());
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
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), cached.getUris(modelSupplier), cached.getScopesIds(modelSupplier), cached.getResourceServerId(), cached.getOwner());
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
        return cacheSession.getResourceServerStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, cached.getResourceServerId());
    }

    @Override
    public Set<String> getUris() {
        if (isUpdated()) return updated.getUris();
        return cached.getUris(modelSupplier);
    }

    @Override
    public void updateUris(Set<String> uris) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), uris, cached.getScopesIds(modelSupplier), cached.getResourceServerId(), cached.getOwner());
        updated.updateUris(uris);
    }

    @Override
    public String getType() {
        if (isUpdated()) return updated.getType();
        return cached.getType();
    }

    @Override
    public void setType(String type) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), type, cached.getUris(modelSupplier), cached.getScopesIds(modelSupplier), cached.getResourceServerId(), cached.getOwner());
        updated.setType(type);

    }

    protected List<Scope> scopes;

    @Override
    public List<Scope> getScopes() {
        if (isUpdated()) return updated.getScopes();
        if (scopes != null) return scopes;
        scopes = new LinkedList<>();
        for (String scopeId : cached.getScopesIds(modelSupplier)) {
            scopes.add(cacheSession.getScopeStore().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, getResourceServer(), scopeId));
        }
        return scopes = Collections.unmodifiableList(scopes);
    }

    @Override
    public String getOwner() {
        if (isUpdated()) return updated.getOwner();
        return cached.getOwner();
    }

    @Override
    public boolean isOwnerManagedAccess() {
        if (isUpdated()) return updated.isOwnerManagedAccess();
        return cached.isOwnerManagedAccess();
    }

    @Override
    public void setOwnerManagedAccess(boolean ownerManagedAccess) {
        getDelegateForUpdate();
        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), cached.getUris(modelSupplier), cached.getScopesIds(modelSupplier), cached.getResourceServerId(), cached.getOwner());
        updated.setOwnerManagedAccess(ownerManagedAccess);
    }

    @Override
    public void updateScopes(Set<Scope> scopes) {
        Resource updated = getDelegateForUpdate();

        for (Scope scope : updated.getScopes()) {
            if (!scopes.contains(scope)) {
                PermissionTicketStore permissionStore = cacheSession.getPermissionTicketStore();
                List<PermissionTicket> permissions = permissionStore.findByScope(getResourceServer(), scope);

                for (PermissionTicket permission : permissions) {
                    permissionStore.delete(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, permission.getId());
                }
            }
        }

        PolicyStore policyStore = cacheSession.getPolicyStore();

        for (Scope scope : updated.getScopes()) {
            if (!scopes.contains(scope)) {
                policyStore.findByResource(getResourceServer(), this, policy -> policy.removeScope(scope));
            }
        }

        cacheSession.registerResourceInvalidation(cached.getId(), cached.getName(), cached.getType(), cached.getUris(modelSupplier), scopes.stream().map(scope1 -> scope1.getId()).collect(Collectors.toSet()), cached.getResourceServerId(), cached.getOwner());
        updated.updateScopes(scopes);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (updated != null) return updated.getAttributes();
        return cached.getAttributes(modelSupplier);
    }

    @Override
    public String getSingleAttribute(String name) {
        if (updated != null) return updated.getSingleAttribute(name);

        List<String> values = cached.getAttributes(modelSupplier).getOrDefault(name, Collections.emptyList());

        if (values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (updated != null) return updated.getAttribute(name);

        List<String> values = cached.getAttributes(modelSupplier).getOrDefault(name, Collections.emptyList());

        if (values.isEmpty()) {
            return null;
        }

        return Collections.unmodifiableList(values);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getDelegateForUpdate();
        updated.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;

        Resource that = (Resource) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private Resource getResourceModel() {
        return cacheSession.getResourceStoreDelegate().findById(InfinispanCacheStoreFactoryProviderFactory.NULL_REALM, getResourceServer(), cached.getId());
    }
}
