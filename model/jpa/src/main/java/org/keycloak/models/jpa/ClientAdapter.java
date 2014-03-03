package org.keycloak.models.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.OAuthClientEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientAdapter implements ClientModel {
    protected ClientEntity entity;
    protected RealmModel realm;

    public ClientAdapter(RealmModel realm, ClientEntity entity) {
        this.realm = realm;
        this.entity = entity;
    }

    public ClientEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return entity.getName();
    }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public long getAllowedClaimsMask() {
        return entity.getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        entity.setAllowedClaimsMask(mask);
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        result.addAll(entity.getWebOrigins());
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        entity.setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        entity.getWebOrigins().add(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        entity.getWebOrigins().remove(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        result.addAll(entity.getRedirectUris());
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        entity.setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        entity.getRedirectUris().add(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        entity.getRedirectUris().remove(redirectUri);
    }

    @Override
    public String getSecret() {
        return entity.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        entity.setSecret(secret);
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(entity.getSecret());
    }

    @Override
    public int getNotBefore() {
        return entity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(notBefore);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!this.getClass().equals(o.getClass())) return false;

        ClientAdapter that = (ClientAdapter) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return entity.getId().hashCode();
    }
}
