package org.keycloak.models.cache;

import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.entities.CachedClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class ClientAdapter implements ClientModel {
    protected CachedClient cachedClient;
    protected CacheRealmProvider cacheSession;
    protected ClientModel updatedClient;
    protected RealmModel cachedRealm;
    protected RealmCache cache;

    public ClientAdapter(RealmModel cachedRealm, CachedClient cached, RealmCache cache, CacheRealmProvider cacheSession) {
        this.cachedRealm = cachedRealm;
        this.cache = cache;
        this.cacheSession = cacheSession;
        this.cachedClient = cached;
    }

    protected abstract void getDelegateForUpdate();

    @Override
    public String getId() {
        if (updatedClient != null) return updatedClient.getId();
        return cachedClient.getId();
    }


    @Override
    public abstract String getClientId();

    public long getAllowedClaimsMask() {
        if (updatedClient != null) return updatedClient.getAllowedClaimsMask();
        return cachedClient.getAllowedClaimsMask();
    }

    public void setAllowedClaimsMask(long mask) {
        getDelegateForUpdate();
        updatedClient.setAllowedClaimsMask(mask);
    }

    public Set<String> getWebOrigins() {
        if (updatedClient != null) return updatedClient.getWebOrigins();
        return cachedClient.getWebOrigins();
    }

    public void setWebOrigins(Set<String> webOrigins) {
        getDelegateForUpdate();
        updatedClient.setWebOrigins(webOrigins);
    }

    public void addWebOrigin(String webOrigin) {
        getDelegateForUpdate();
        updatedClient.addWebOrigin(webOrigin);
    }

    public void removeWebOrigin(String webOrigin) {
        getDelegateForUpdate();
        updatedClient.removeWebOrigin(webOrigin);
    }

    public Set<String> getRedirectUris() {
        if (updatedClient != null) return updatedClient.getRedirectUris();
        return cachedClient.getRedirectUris();
    }

    public void setRedirectUris(Set<String> redirectUris) {
        getDelegateForUpdate();
        updatedClient.setRedirectUris(redirectUris);
    }

    public void addRedirectUri(String redirectUri) {
        getDelegateForUpdate();
        updatedClient.addRedirectUri(redirectUri);
    }

    public void removeRedirectUri(String redirectUri) {
        getDelegateForUpdate();
        updatedClient.removeRedirectUri(redirectUri);
    }

    public boolean isEnabled() {
        if (updatedClient != null) return updatedClient.isEnabled();
        return cachedClient.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        getDelegateForUpdate();
        updatedClient.setEnabled(enabled);
    }

    public boolean validateSecret(String secret) {
        return secret.equals(getSecret());
    }

    public String getSecret() {
        if (updatedClient != null) return updatedClient.getSecret();
        return cachedClient.getSecret();
    }

    public void setSecret(String secret) {
        getDelegateForUpdate();
        updatedClient.setSecret(secret);
    }

    public boolean isPublicClient() {
        if (updatedClient != null) return updatedClient.isPublicClient();
        return cachedClient.isPublicClient();
    }

    public void setPublicClient(boolean flag) {
        getDelegateForUpdate();
        updatedClient.setPublicClient(flag);
    }

    public boolean isFrontchannelLogout() {
        if (updatedClient != null) return updatedClient.isPublicClient();
        return cachedClient.isFrontchannelLogout();
    }

    public void setFrontchannelLogout(boolean flag) {
        getDelegateForUpdate();
        updatedClient.setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        if (updatedClient != null) return updatedClient.isFullScopeAllowed();
        return cachedClient.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getDelegateForUpdate();
        updatedClient.setFullScopeAllowed(value);

    }

    public boolean isDirectGrantsOnly() {
        if (updatedClient != null) return updatedClient.isDirectGrantsOnly();
        return cachedClient.isDirectGrantsOnly();
    }

    public void setDirectGrantsOnly(boolean flag) {
        getDelegateForUpdate();
        updatedClient.setDirectGrantsOnly(flag);
    }

    public Set<RoleModel> getScopeMappings() {
        if (updatedClient != null) return updatedClient.getScopeMappings();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String id : cachedClient.getScope()) {
            roles.add(cacheSession.getRoleById(id, getRealm()));

        }
        return roles;
    }

    public void addScopeMapping(RoleModel role) {
        getDelegateForUpdate();
        updatedClient.addScopeMapping(role);
    }

    public void deleteScopeMapping(RoleModel role) {
        getDelegateForUpdate();
        updatedClient.deleteScopeMapping(role);
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

    public boolean hasScope(RoleModel role) {
        if (updatedClient != null) return updatedClient.hasScope(role);
        if (cachedClient.isFullScopeAllowed() || cachedClient.getScope().contains(role.getId())) return true;

        Set<RoleModel> roles = getScopeMappings();

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    public RealmModel getRealm() {
        return cachedRealm;
    }

    public int getNotBefore() {
        if (updatedClient != null) return updatedClient.getNotBefore();
        return cachedClient.getNotBefore();
    }

    public void setNotBefore(int notBefore) {
        getDelegateForUpdate();
        updatedClient.setNotBefore(notBefore);
    }

    @Override
    public String getProtocol() {
        if (updatedClient != null) return updatedClient.getProtocol();
        return cachedClient.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        getDelegateForUpdate();
        updatedClient.setProtocol(protocol);
    }

    @Override
    public void setAttribute(String name, String value) {
        getDelegateForUpdate();
        updatedClient.setAttribute(name, value);

    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updatedClient.removeAttribute(name);

    }

    @Override
    public String getAttribute(String name) {
        if (updatedClient != null) return updatedClient.getAttribute(name);
        return cachedClient.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        if (updatedClient != null) return updatedClient.getAttributes();
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(cachedClient.getAttributes());
        return copy;
    }

    @Override
    public void updateIdentityProviders(List<ClientIdentityProviderMappingModel> identityProviders) {
        getDelegateForUpdate();
        updatedClient.updateIdentityProviders(identityProviders);
    }

    @Override
    public List<ClientIdentityProviderMappingModel> getIdentityProviders() {
        if (updatedClient != null) return updatedClient.getIdentityProviders();
        return cachedClient.getIdentityProviders();
    }

    @Override
    public boolean isAllowedRetrieveTokenFromIdentityProvider(String providerId) {
        if (updatedClient != null) return updatedClient.isAllowedRetrieveTokenFromIdentityProvider(providerId);
        return cachedClient.isAllowedRetrieveTokenFromIdentityProvider(providerId);
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        if (updatedClient != null) return updatedClient.getProtocolMappers();
        return cachedClient.getProtocolMappers();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        getDelegateForUpdate();
        return updatedClient.addProtocolMapper(model);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        getDelegateForUpdate();
        updatedClient.removeProtocolMapper(mapping);

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        getDelegateForUpdate();
        updatedClient.updateProtocolMapper(mapping);

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        for (ProtocolMapperModel mapping : cachedClient.getProtocolMappers()) {
            if (mapping.getId().equals(id)) return mapping;
        }
        return null;
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        for (ProtocolMapperModel mapping : cachedClient.getProtocolMappers()) {
            if (mapping.getProtocol().equals(protocol) && mapping.getName().equals(name)) return mapping;
        }
        return null;
    }
}
