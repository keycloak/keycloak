package org.keycloak.models.cache.infinispan;

import org.keycloak.models.*;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedClient;

import java.util.*;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientAdapter implements ClientModel {
    protected CacheRealmProvider cacheSession;
    protected RealmModel cachedRealm;
    protected RealmCache cache;

    protected ClientModel updated;
    protected CachedClient cached;

    public ClientAdapter(RealmModel cachedRealm, CachedClient cached, CacheRealmProvider cacheSession, RealmCache cache) {
        this.cachedRealm = cachedRealm;
        this.cache = cache;
        this.cacheSession = cacheSession;
        this.cached = cached;
    }

    private void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerApplicationInvalidation(getId());
            updated = updated = cacheSession.getDelegate().getClientById(getId(), cachedRealm);
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    @Override
    public void updateClient() {
        if (updated != null) updated.updateClient();
    }

    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    public Set<String> getWebOrigins() {
        if (updated != null) return updated.getWebOrigins();
        return cached.getWebOrigins();
    }

    public void setWebOrigins(Set<String> webOrigins) {
        getDelegateForUpdate();
        updated.setWebOrigins(webOrigins);
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
        if (updated != null) return updated.getRedirectUris();
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
        if (updated != null) return updated.isEnabled();
        return cached.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updated.setEnabled(enabled);
    }

    @Override
    public String getClientAuthenticatorType() {
        if (updated != null) return updated.getClientAuthenticatorType();
        return cached.getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        getDelegateForUpdate();
        updated.setClientAuthenticatorType(clientAuthenticatorType);
    }

    public boolean validateSecret(String secret) {
        return secret.equals(getSecret());
    }

    public String getSecret() {
        if (updated != null) return updated.getSecret();
        return cached.getSecret();
    }

    public void setSecret(String secret) {
        getDelegateForUpdate();
        updated.setSecret(secret);
    }

    public boolean isPublicClient() {
        if (updated != null) return updated.isPublicClient();
        return cached.isPublicClient();
    }

    public void setPublicClient(boolean flag) {
        getDelegateForUpdate();
        updated.setPublicClient(flag);
    }

    public boolean isFrontchannelLogout() {
        if (updated != null) return updated.isPublicClient();
        return cached.isFrontchannelLogout();
    }

    public void setFrontchannelLogout(boolean flag) {
        getDelegateForUpdate();
        updated.setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        if (updated != null) return updated.isFullScopeAllowed();
        return cached.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getDelegateForUpdate();
        updated.setFullScopeAllowed(value);

    }

    public boolean isDirectGrantsOnly() {
        if (updated != null) return updated.isDirectGrantsOnly();
        return cached.isDirectGrantsOnly();
    }

    public void setDirectGrantsOnly(boolean flag) {
        getDelegateForUpdate();
        updated.setDirectGrantsOnly(flag);
    }

    public Set<RoleModel> getScopeMappings() {
        if (updated != null) return updated.getScopeMappings();
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

    public RealmModel getRealm() {
        return cachedRealm;
    }

    public int getNotBefore() {
        if (updated != null) return updated.getNotBefore();
        return cached.getNotBefore();
    }

    public void setNotBefore(int notBefore) {
        getDelegateForUpdate();
        updated.setNotBefore(notBefore);
    }

    @Override
    public String getProtocol() {
        if (updated != null) return updated.getProtocol();
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
        if (updated != null) return updated.getAttribute(name);
        return cached.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        if (updated != null) return updated.getAttributes();
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(cached.getAttributes());
        return copy;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        if (updated != null) return updated.getProtocolMappers();
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
        if (updated != null) return updated.getClientId();
        return cached.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        getDelegateForUpdate();
        updated.setClientId(clientId);
        cacheSession.registerRealmInvalidation(cachedRealm.getId());
    }

    @Override
    public String getName() {
        if (updated != null) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        if (updated != null) return updated.isSurrogateAuthRequired();
        return cached.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        getDelegateForUpdate();
        updated.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        if (updated != null) return updated.getManagementUrl();
        return cached.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        getDelegateForUpdate();
        updated.setManagementUrl(url);
    }

    @Override
    public String getBaseUrl() {
        if (updated != null) return updated.getBaseUrl();
        return cached.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        getDelegateForUpdate();
        updated.setBaseUrl(url);
    }

    @Override
    public List<String> getDefaultRoles() {
        if (updated != null) return updated.getDefaultRoles();
        return cached.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        getDelegateForUpdate();
        updated.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        getDelegateForUpdate();
        updated.updateDefaultRoles(defaultRoles);
    }

    @Override
    public Set<RoleModel> getClientScopeMappings(ClientModel client) {
        Set<RoleModel> roleMappings = client.getScopeMappings();

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
            } else {
                ClientModel app = (ClientModel)container;
                if (app.getId().equals(getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }

    @Override
    public boolean isBearerOnly() {
        if (updated != null) return updated.isBearerOnly();
        return cached.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        getDelegateForUpdate();
        updated.setBearerOnly(only);
    }

    @Override
    public boolean isConsentRequired() {
        if (updated != null) return updated.isConsentRequired();
        return cached.isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        getDelegateForUpdate();
        updated.setConsentRequired(consentRequired);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        if (updated != null) return updated.isServiceAccountsEnabled();
        return cached.isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        getDelegateForUpdate();
        updated.setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public RoleModel getRole(String name) {
        if (updated != null) return updated.getRole(name);
        String id = cached.getRoles().get(name);
        if (id == null) return null;
        return cacheSession.getRoleById(id, cachedRealm);
    }

    @Override
    public RoleModel addRole(String name) {
        getDelegateForUpdate();
        RoleModel role = updated.addRole(name);
        cacheSession.registerRoleInvalidation(role.getId());
        return role;
    }

    @Override
    public RoleModel addRole(String id, String name) {
        getDelegateForUpdate();
        RoleModel role =  updated.addRole(id, name);
        cacheSession.registerRoleInvalidation(role.getId());
        return role;
    }

    @Override
    public boolean removeRole(RoleModel role) {
        cacheSession.registerRoleInvalidation(role.getId());
        getDelegateForUpdate();
        return updated.removeRole(role);
    }

    @Override
    public Set<RoleModel> getRoles() {
        if (updated != null) return updated.getRoles();

        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String id : cached.getRoles().values()) {
            RoleModel roleById = cacheSession.getRoleById(id, cachedRealm);
            if (roleById == null) continue;
            roles.add(roleById);
        }
        return roles;
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        if (updated != null) return updated.getNodeReRegistrationTimeout();
        return cached.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        getDelegateForUpdate();
        updated.setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        if (updated != null) return updated.getRegisteredNodes();
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
        if (updated != null) return updated.hasScope(role);
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
        if (o == null || !(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
