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

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.infinispan.entities.CachedClientTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplateAdapter implements ClientTemplateModel {
    protected RealmCacheSession cacheSession;
    protected RealmModel cachedRealm;

    protected ClientTemplateModel updated;
    protected CachedClientTemplate cached;

    public ClientTemplateAdapter(RealmModel cachedRealm, CachedClientTemplate cached, RealmCacheSession cacheSession) {
        this.cachedRealm = cachedRealm;
        this.cacheSession = cacheSession;
        this.cached = cached;
    }

    private void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerClientTemplateInvalidation(cached.getId());
            updated = cacheSession.getDelegate().getClientTemplateById(cached.getId(), cachedRealm);
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
        updated = cacheSession.getDelegate().getClientTemplateById(cached.getId(), cachedRealm);
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    public RealmModel getRealm() {
        return cachedRealm;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        if (isUpdated()) return updated.getProtocolMappers();
        return cached.getProtocolMappers();
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
    public boolean isFullScopeAllowed() {
        if (isUpdated()) return updated.isFullScopeAllowed();
        return cached.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getDelegateForUpdate();
        updated.setFullScopeAllowed(value);

    }

    public Set<RoleModel> getScopeMappings() {
        if (isUpdated()) return updated.getScopeMappings();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String id : cached.getScope()) {
            roles.add(cacheSession.getRoleById(id, getRealm()));

        }
        return roles;
    }

    public void addScopeMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.addScopeMapping(role);
    }

    public void deleteScopeMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.deleteScopeMapping(role);
    }

    public Set<RoleModel> getRealmScopeMappings() {
        Set<RoleModel> roleMappings = getScopeMappings();

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(cachedRealm.getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isUpdated()) return updated.hasScope(role);
        if (cached.isFullScopeAllowed() || cached.getScope().contains(role.getId())) return true;

        Set<RoleModel> roles = getScopeMappings();

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
       return false;
    }

    public boolean isPublicClient() {
        if (isUpdated()) return updated.isPublicClient();
        return cached.isPublicClient();
    }

    public void setPublicClient(boolean flag) {
        getDelegateForUpdate();
        updated.setPublicClient(flag);
    }

    public boolean isFrontchannelLogout() {
        if (isUpdated()) return updated.isPublicClient();
        return cached.isFrontchannelLogout();
    }

    public void setFrontchannelLogout(boolean flag) {
        getDelegateForUpdate();
        updated.setFrontchannelLogout(flag);
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
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(cached.getAttributes());
        return copy;
    }

    @Override
    public boolean isBearerOnly() {
        if (isUpdated()) return updated.isBearerOnly();
        return cached.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        getDelegateForUpdate();
        updated.setBearerOnly(only);
    }

    @Override
    public boolean isConsentRequired() {
        if (isUpdated()) return updated.isConsentRequired();
        return cached.isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        getDelegateForUpdate();
        updated.setConsentRequired(consentRequired);
    }

    @Override
    public boolean isStandardFlowEnabled() {
        if (isUpdated()) return updated.isStandardFlowEnabled();
        return cached.isStandardFlowEnabled();
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        getDelegateForUpdate();
        updated.setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        if (isUpdated()) return updated.isImplicitFlowEnabled();
        return cached.isImplicitFlowEnabled();
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        getDelegateForUpdate();
        updated.setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        if (isUpdated()) return updated.isDirectAccessGrantsEnabled();
        return cached.isDirectAccessGrantsEnabled();
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        getDelegateForUpdate();
        updated.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        if (isUpdated()) return updated.isServiceAccountsEnabled();
        return cached.isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        getDelegateForUpdate();
        updated.setServiceAccountsEnabled(serviceAccountsEnabled);
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientModel)) return false;

        ClientTemplateModel that = (ClientTemplateModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
