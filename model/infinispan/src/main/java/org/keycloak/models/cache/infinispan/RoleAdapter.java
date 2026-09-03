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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedClientRole;
import org.keycloak.models.cache.infinispan.entities.CachedOrganizationRole;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel {

    protected RoleModel updated;
    private final KeycloakSession session;
    protected CachedRole cached;
    protected RealmCacheSession cacheSession;
    protected RealmModel realm;
    protected Set<RoleModel> composites;
    private final Supplier<RoleModel> modelSupplier;

    public RoleAdapter(CachedRole cached, RealmCacheSession cacheSession, RealmModel realm) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.session = cacheSession.session;
        this.realm = realm;
        this.modelSupplier = new LazyModel<>(this::getRoleModel);
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerRoleInvalidation(cached.getId(), cached.getName(), getContainerId());
            updated = modelSupplier.get();
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    protected void getDelegateForRename(String newName) {
        if (!Objects.equals(newName, cached.getName())) {
            // New role name might have been cached as non-existent
            String containerId = getContainerId();
            cacheSession.registerRoleInvalidation(cached.getId(), newName, containerId);
        }
        getDelegateForUpdate();
    }

    protected boolean invalidated;

    public void invalidate() {
        invalidated = true;
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = getRoleModel();
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
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public void setName(String name) {
        getDelegateForRename(name);
        updated.setName(name);
    }

    @Override
    public boolean isComposite() {
        if (isUpdated()) return updated.isComposite();
        return cached.isComposite(session, this::getRoleModel);
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
    public Stream<RoleModel> getCompositesStream() {
        if (isUpdated()) return updated.getCompositesStream();

        if (composites == null) {
            composites = new HashSet<>();
            for (String id : cached.getComposites(session, modelSupplier).ids()) {
                RoleModel role = realm.getRoleById(id);
                if (role == null) {
                    // chance that composite role was removed, so invalidate this entry and fallback to delegate
                    getDelegateForUpdate();
                    return updated.getCompositesStream();
                }
                composites.add(role);
            }
        }

        return composites.stream();
    }

    @Override
    public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
        if (isUpdated()) return updated.getCompositesStream(search, first, max);

        return cacheSession.getRoleDelegate().getRolesStream(realm, cached.getComposites(session, modelSupplier).ids().stream(), search, first, max);
    }

    @Override
    public Type getType() {
        if (cached instanceof CachedClientRole) {
            return Type.CLIENT;
        }
        if (cached instanceof CachedOrganizationRole) {
            return Type.ORGANIZATION;
        }
        return Type.REALM;
    }

    @Override
    public String getContainerId() {
        return switch (getType()) {
            case CLIENT -> ((CachedClientRole) cached).getClientId();
            case ORGANIZATION -> ((CachedOrganizationRole) cached).getOrganizationId();
            case REALM -> realm.getId();
        };
    }


    @Override
    public RoleContainerModel getContainer() {
        return switch (getType()) {
            case CLIENT -> realm.getClientById(((CachedClientRole) cached).getClientId());
            case ORGANIZATION -> getOrganizationContainer();
            case REALM -> realm;
        };
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return this.equals(role) || KeycloakModelUtils.searchFor(role, this, new HashSet<>());
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setSingleAttribute(name, value);
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
    public String getFirstAttribute(String name) {
        if (updated != null) {
            return updated.getFirstAttribute(name);
        }

        return cached.getAttributes(session, modelSupplier).getFirst(name);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (updated != null) {
            return updated.getAttributeStream(name);
        }

        List<String> result = cached.getAttributes(session, modelSupplier).get(name);
        if (result == null) {
            return Stream.empty();
        }
        return result.stream();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (updated != null) {
            return updated.getAttributes();
        }

        return cached.getAttributes(session, modelSupplier);
    }

    protected RoleModel getRoleModel() {
        if (cached instanceof CachedOrganizationRole organizationRole) {
            OrganizationModel organization = getOrganizationContainer(organizationRole.getOrganizationId());
            return organization == null ? null : cacheSession.getRoleDelegate().getRoleById(organization, cached.getId());
        }
        return cacheSession.getRoleDelegate().getRoleById(realm, cached.getId());
    }

    private OrganizationModel getOrganizationContainer() {
        return getOrganizationContainer(((CachedOrganizationRole) cached).getOrganizationId());
    }

    private OrganizationModel getOrganizationContainer(String organizationId) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        if (provider == null) return null;
        OrganizationModel organization = provider.getById(organizationId);
        if (organization == null || !realm.getId().equals(organization.getRealm().getId())) return null;
        return organization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
