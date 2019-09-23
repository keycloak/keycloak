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
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.CachedObject;
import org.keycloak.models.cache.infinispan.entities.CachedClient;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientAdapter implements ClientModel, CachedObject {
    protected RealmCacheSession cacheSession;
    protected RealmModel cachedRealm;

    protected ClientModel updated;
    protected CachedClient cached;

    public ClientAdapter(RealmModel cachedRealm, CachedClient cached, RealmCacheSession cacheSession) {
        this.cachedRealm = cachedRealm;
        this.cacheSession = cacheSession;
        this.cached = cached;
    }

    private void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerClientInvalidation(cached.getId(), cached.getClientId(), cachedRealm.getId());
            updated = cacheSession.getRealmDelegate().getClientById(cached.getId(), cachedRealm);
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
        updated = cacheSession.getRealmDelegate().getClientById(cached.getId(), cachedRealm);
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    @Override
    public void updateClient() {
        if (updated != null) updated.updateClient();
    }

    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    public Set<String> getWebOrigins() {
        if (isUpdated()) return updated.getWebOrigins();
        return cached.getWebOrigins();
    }

    public void setWebOrigins(Set<String> webOrigins) {
        getDelegateForUpdate();
        updated.setWebOrigins(webOrigins);
    }

    @Override
    public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        getDelegateForUpdate();
        updated.addClientScope(clientScope, defaultScope);
    }

    @Override
    public void removeClientScope(ClientScopeModel clientScope) {
        getDelegateForUpdate();
        updated.removeClientScope(clientScope);
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope, boolean filterByProtocol) {
        if (isUpdated()) return updated.getClientScopes(defaultScope, filterByProtocol);
        List<String> clientScopeIds = defaultScope ? cached.getDefaultClientScopesIds() : cached.getOptionalClientScopesIds();

        // Defaults to openid-connect
        String clientProtocol = getProtocol() == null ? "openid-connect" : getProtocol();

        Map<String, ClientScopeModel> clientScopes = new HashMap<>();
        for (String scopeId : clientScopeIds) {
            ClientScopeModel clientScope = cacheSession.getClientScopeById(scopeId, cachedRealm);
            if (clientScope != null) {
                if (!filterByProtocol || clientScope.getProtocol().equals(clientProtocol)) {
                    clientScopes.put(clientScope.getName(), clientScope);
                }
            }
        }
        return clientScopes;
    }

    public void addWebOrigin(String webOrigin) {
        getDelegateForUpdate();
        updated.addWebOrigin(webOrigin);
    }

    public void removeWebOrigin(String webOrigin) {
        getDelegateForUpdate();
        updated.removeWebOrigin(webOrigin);
    }

    public Set<String> getRedirectUris() {
        if (isUpdated()) return updated.getRedirectUris();
        return cached.getRedirectUris();
    }

    public void setRedirectUris(Set<String> redirectUris) {
        getDelegateForUpdate();
        updated.setRedirectUris(redirectUris);
    }

    public void addRedirectUri(String redirectUri) {
        getDelegateForUpdate();
        updated.addRedirectUri(redirectUri);
    }

    public void removeRedirectUri(String redirectUri) {
        getDelegateForUpdate();
        updated.removeRedirectUri(redirectUri);
    }

    public boolean isEnabled() {
        if (isUpdated()) return updated.isEnabled();
        return cached.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    public boolean isAlwaysDisplayInConsole() {
        if(isUpdated()) return updated.isAlwaysDisplayInConsole();
        return cached.isAlwaysDisplayInConsole();
    }

    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        getDelegateForUpdate();
        updated.setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public String getClientAuthenticatorType() {
        if (isUpdated()) return updated.getClientAuthenticatorType();
        return cached.getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        getDelegateForUpdate();
        updated.setClientAuthenticatorType(clientAuthenticatorType);
    }

    public boolean validateSecret(String secret) {
        return MessageDigest.isEqual(secret.getBytes(), getSecret().getBytes());
    }

    public String getSecret() {
        if (isUpdated()) return updated.getSecret();
        return cached.getSecret();
    }

    public void setSecret(String secret) {
        getDelegateForUpdate();
        updated.setSecret(secret);
    }
    public String getRegistrationToken() {
        if (isUpdated()) return updated.getRegistrationToken();
        return cached.getRegistrationToken();
    }

    public void setRegistrationToken(String registrationToken) {
        getDelegateForUpdate();
        updated.setRegistrationToken(registrationToken);
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
        Set<RoleModel> roles = new HashSet<>();
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

        Set<RoleModel> appRoles = new HashSet<>();
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

    public RealmModel getRealm() {
        return cachedRealm;
    }

    public int getNotBefore() {
        if (isUpdated()) return updated.getNotBefore();
        return cached.getNotBefore();
    }

    public void setNotBefore(int notBefore) {
        getDelegateForUpdate();
        updated.setNotBefore(notBefore);
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
    public void setAuthenticationFlowBindingOverride(String name, String value) {
        getDelegateForUpdate();
        updated.setAuthenticationFlowBindingOverride(name, value);

    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String name) {
        getDelegateForUpdate();
        updated.removeAuthenticationFlowBindingOverride(name);

    }

    @Override
    public String getAuthenticationFlowBindingOverride(String name) {
        if (isUpdated()) return updated.getAuthenticationFlowBindingOverride(name);
        return cached.getAuthFlowBindings().get(name);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        if (isUpdated()) return updated.getAuthenticationFlowBindingOverrides();
        Map<String, String> copy = new HashMap<>();
        copy.putAll(cached.getAuthFlowBindings());
        return copy;
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
    public String getClientId() {
        if (isUpdated()) return updated.getClientId();
        return cached.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        getDelegateForUpdate();
        updated.setClientId(clientId);
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
    public boolean isSurrogateAuthRequired() {
        if (isUpdated()) return updated.isSurrogateAuthRequired();
        return cached.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        getDelegateForUpdate();
        updated.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        if (isUpdated()) return updated.getManagementUrl();
        return cached.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        getDelegateForUpdate();
        updated.setManagementUrl(url);
    }

    @Override
    public String getRootUrl() {
        if (isUpdated()) return updated.getRootUrl();
        return cached.getRootUrl();
    }

    @Override
    public void setRootUrl(String url) {
        getDelegateForUpdate();
        updated.setRootUrl(url);
    }

    @Override
    public String getBaseUrl() {
        if (isUpdated()) return updated.getBaseUrl();
        return cached.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        getDelegateForUpdate();
        updated.setBaseUrl(url);
    }

    @Override
    public List<String> getDefaultRoles() {
        if (isUpdated()) return updated.getDefaultRoles();
        return cached.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        getDelegateForUpdate();
        updated.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String... defaultRoles) {
        getDelegateForUpdate();
        updated.updateDefaultRoles(defaultRoles);
    }

    @Override
    public void removeDefaultRoles(String... defaultRoles) {
        getDelegateForUpdate();
        updated.removeDefaultRoles(defaultRoles);

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
    public RoleModel getRole(String name) {
        return cacheSession.getClientRole(getRealm(), this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return cacheSession.addClientRole(getRealm(), this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return cacheSession.addClientRole(getRealm(), this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return cacheSession.removeRole(cachedRealm, role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        return cacheSession.getClientRoles(cachedRealm, this);
    }
    
    @Override
    public Set<RoleModel> getRoles(Integer first, Integer max) {
        return cacheSession.getClientRoles(cachedRealm, this, first, max);
    }
    
    @Override
    public Set<RoleModel> searchForRoles(String search, Integer first, Integer max) {
        return cacheSession.searchForClientRoles(cachedRealm, this, search, first, max);
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        if (isUpdated()) return updated.getNodeReRegistrationTimeout();
        return cached.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        getDelegateForUpdate();
        updated.setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        if (isUpdated()) return updated.getRegisteredNodes();
        return cached.getRegisteredNodes();
    }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        getDelegateForUpdate();
        updated.registerNode(nodeHost, registrationTime);
    }

    @Override
    public void unregisterNode(String nodeHost) {
        getDelegateForUpdate();
        updated.unregisterNode(nodeHost);
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isUpdated()) return updated.hasScope(role);
        if (cached.isFullScopeAllowed() || cached.getScope().contains(role.getId())) return true;

        Set<RoleModel> roles = getScopeMappings();

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }

        roles = getRoles();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
