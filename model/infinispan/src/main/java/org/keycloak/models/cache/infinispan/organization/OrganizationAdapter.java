/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.cache.infinispan.organization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.organization.OrganizationProvider;

public class OrganizationAdapter implements OrganizationModel {

    private volatile boolean invalidated;
    private volatile OrganizationModel updated;
    private final Supplier<OrganizationModel> modelSupplier;
    private final CacheRealmProvider realmCache;
    private final CachedOrganization cached;
    private final OrganizationProvider delegate;

    public OrganizationAdapter(CachedOrganization cached, CacheRealmProvider realmCache, OrganizationProvider delegate) {
        this.cached = cached;
        this.realmCache = realmCache;
        this.delegate = delegate;
        this.modelSupplier = this::getOrganizationModel;
    }

    void invalidate() {
        invalidated = true;
    }

    private OrganizationModel getOrganizationModel() {
        return delegate.getById(cached.getId());
    }

    private boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = getOrganizationModel();
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }

    private void getDelegateForUpdate() {
        if (updated == null) {
            realmCache.registerInvalidation(cached.getId());
            updated = modelSupplier.get();
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (isUpdated()) return updated.getName() ;
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public boolean isEnabled() {
        if (isUpdated()) return updated.isEnabled();
        return cached.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
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
    public Map<String, List<String>> getAttributes() {
        if (isUpdated()) return updated.getAttributes();
        return cached.getAttributes(modelSupplier);
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        getDelegateForUpdate();
        updated.setAttributes(attributes);
    }

    @Override
    public Stream<OrganizationDomainModel> getDomains() {
        if (isUpdated()) return updated.getDomains();
        return cached.getDomains();
    }

    @Override
    public void setDomains(Set<OrganizationDomainModel> domains) {
        getDelegateForUpdate();
        updated.setDomains(domains);
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProviders() {
        if (isUpdated()) return updated.getIdentityProviders();
        return cached.getIdentityProviders();
    }

    @Override
    public boolean isManaged(UserModel user) {
        return delegate.isManagedMember(this, user);
    }

}
