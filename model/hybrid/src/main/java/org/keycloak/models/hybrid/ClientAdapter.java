package org.keycloak.models.hybrid;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.realms.Client;

import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class ClientAdapter implements ClientModel {

    protected HybridModelProvider provider;

    protected Client client;

    ClientAdapter(HybridModelProvider provider, Client client) {
        this.provider = provider;
        this.client = client;
    }

    @Override
    public String getId() {
        return client.getId();
    }

    @Override
    public String getClientId() {
        return client.getClientId();
    }

    @Override
    public long getAllowedClaimsMask() {
        return client.getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        client.setAllowedClaimsMask(mask);
    }

    @Override
    public Set<String> getWebOrigins() {
        return client.getWebOrigins();
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        client.setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        client.addWebOrigin(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        client.removeWebOrigin(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        return client.getRedirectUris();
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        client.setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        client.addRedirectUri(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        client.removeRedirectUri(redirectUri);
    }

    @Override
    public boolean isEnabled() {
        return client.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        client.setEnabled(enabled);
    }

    @Override
    public boolean validateSecret(String secret) {
        return client.validateSecret(secret);
    }

    @Override
    public String getSecret() {
        return client.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        client.setSecret(secret);
    }

    @Override
    public boolean isPublicClient() {
        return client.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        client.setPublicClient(flag);
    }

    @Override
    public boolean isDirectGrantsOnly() {
        return client.isDirectGrantsOnly();
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        client.setDirectGrantsOnly(flag);
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        return provider.mappings().wrap(client.getScopeMappings());
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        if (!hasScope(role)) {
            client.addScopeMapping(provider.mappings().unwrap(role));
        }
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        client.deleteScopeMapping(provider.mappings().unwrap(role));
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        return provider.mappings().wrap(client.getRealmScopeMappings());
    }

    @Override
    public boolean hasScope(RoleModel role) {
        Set<RoleModel> roles = getScopeMappings();
        if (roles.contains(role)) {
            return true;
        }

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public RealmModel getRealm() {
        return provider.mappings().wrap(client.getRealm());
    }

    @Override
    public int getNotBefore() {
        return client.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        client.setNotBefore(notBefore);
    }

    @Override
    public Set<UserSessionModel> getUserSessions() {
        return provider.mappings().wrapSessions(getRealm(), provider.sessions().getUserSessionsByClient(client.getRealm().getId(), client.getId()));
    }

    @Override
    public int getActiveUserSessions() {
        return provider.sessions().getActiveUserSessions(client.getRealm().getId(), client.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!this.getClass().equals(o.getClass())) return false;

        ClientAdapter that = (ClientAdapter) o;
        return that.getId().equals(getId());
    }

}
