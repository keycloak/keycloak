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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.infinispan.LazyModel;
import org.keycloak.organization.OrganizationProvider;

public class OrganizationAdapter implements OrganizationModel {

    private volatile boolean invalidated;
    private volatile OrganizationModel updated;
    private final Supplier<OrganizationModel> modelSupplier;
    private final KeycloakSession session;
    private final CachedOrganization cached;
    private final Supplier<OrganizationProvider> delegate;
    private final InfinispanOrganizationProvider organizationCache;

    public OrganizationAdapter(KeycloakSession session, CachedOrganization cached, Supplier<OrganizationProvider> delegate, InfinispanOrganizationProvider organizationCache) {
        this.session = session;
        this.cached = cached;
        this.delegate = delegate;
        this.organizationCache = organizationCache;
        this.modelSupplier = new LazyModel<>(this::getOrganizationModel);
    }

    void invalidate() {
        invalidated = true;
    }

    private OrganizationModel getOrganizationModel() {
        return delegate.get().getById(cached.getId());
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
            updated = modelSupplier.get();
            organizationCache.registerOrganizationInvalidation(updated);
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
    public String getAlias() {
        if (isUpdated()) return updated.getAlias() ;
        return cached.getAlias();
    }

    @Override
    public void setAlias(String alias) {
        getDelegateForUpdate();
        updated.setAlias(alias);
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
    public String getRedirectUrl() {
        if (isUpdated()) return updated.getRedirectUrl();
        return cached.getRedirectUrl();
    }

    @Override
    public void setRedirectUrl(String redirectUrl) {
        getDelegateForUpdate();
        updated.setRedirectUrl(redirectUrl);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (isUpdated()) return updated.getAttributes();
        return cached.getAttributes(session, modelSupplier);
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
        if (isUpdated()) delegate.get().isManagedMember(this, user);
        return organizationCache.isManagedMember(this, user);
    }

    @Override
    public boolean isMember(UserModel user) {
        if (isUpdated()) delegate.get().isMember(this, user);
        return organizationCache.isMember(this, user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationModel)) return false;

        OrganizationModel that = (OrganizationModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
