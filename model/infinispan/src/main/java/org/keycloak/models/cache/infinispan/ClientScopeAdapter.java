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

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedClientScope;
import org.keycloak.models.utils.RoleUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientScopeAdapter implements ClientScopeModel {
    protected RealmCacheSession cacheSession;
    protected RealmModel cachedRealm;

    protected ClientScopeModel updated;
    protected CachedClientScope cached;

    public ClientScopeAdapter(RealmModel cachedRealm, CachedClientScope cached, RealmCacheSession cacheSession) {
        this.cachedRealm = cachedRealm;
        this.cacheSession = cacheSession;
        this.cached = cached;
    }

    private void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerClientScopeInvalidation(cached.getId(), cachedRealm.getId());
            updated = cacheSession.getClientScopeDelegate().getClientScopeById(cachedRealm, cached.getId());
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
        updated = cacheSession.getClientScopeDelegate().getClientScopeById(cachedRealm, cached.getId());
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public RealmModel getRealm() {
        return cachedRealm;
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        if (isUpdated()) return updated.getProtocolMappersStream();
        return cached.getProtocolMappers().stream();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        getDelegateForUpdate();
        return updated.addProtocolMapper(model);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        getDelegateForUpdate();
        updated.removeProtocolMapper(mapping);

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        getDelegateForUpdate();
        updated.updateProtocolMapper(mapping);

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        for (ProtocolMapperModel mapping : cached.getProtocolMappers()) {
            if (mapping.getId().equals(id)) return mapping;
        }
        return null;
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        for (ProtocolMapperModel mapping : cached.getProtocolMappers()) {
            if (mapping.getProtocol().equals(protocol) && mapping.getName().equals(name)) return mapping;
        }
        return null;
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
    public String getProtocol() {
        if (isUpdated()) return updated.getProtocol();
        return cached.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        getDelegateForUpdate();
        updated.setProtocol(protocol);
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        if (isUpdated()) return updated.getScopeMappingsStream();
        return cached.getScope().stream()
          .map(id -> cacheSession.getRoleById(cachedRealm, id));
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.addScopeMapping(role);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.deleteScopeMapping(role);
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return getScopeMappingsStream().filter(r -> RoleUtils.isRealmRole(r, cachedRealm));
    }

    @Override
    public boolean hasDirectScope(RoleModel role) {
        if (isUpdated()) return updated.hasDirectScope(role);

        return cached.getScope().contains(role.getId());
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isUpdated()) return updated.hasScope(role);
        if (cached.getScope().contains(role.getId())) return true;

        return RoleUtils.hasRole(getScopeMappingsStream(), role);
    }


    @Override
    public void setAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setAttribute(name, value);

    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);

    }

    @Override
    public String getAttribute(String name) {
        if (isUpdated()) return updated.getAttribute(name);
        return cached.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        if (isUpdated()) return updated.getAttributes();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(cached.getAttributes());
        return copy;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientScopeModel)) return false;

        ClientScopeModel that = (ClientScopeModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }
}
