package org.keycloak.models.jpa;

import org.keycloak.models.ClientModel;
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

    public ClientAdapter(ClientEntity entity) {
        this.entity = entity;
    }

    public ClientEntity getEntity() {
        return entity;
    }

    public String getId() {
        return entity.getId();
    }

    public String getClientId() {
        return entity.getName();
    }

    public boolean isEnabled() {
        return entity.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    public long getAllowedClaimsMask() {
        return entity.getAllowedClaimsMask();
    }

    public void setAllowedClaimsMask(long mask) {
        entity.setAllowedClaimsMask(mask);
    }

    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        result.addAll(entity.getWebOrigins());
        return result;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        entity.setWebOrigins(webOrigins);
    }

    public void addWebOrigin(String webOrigin) {
        entity.getWebOrigins().add(webOrigin);
    }

    public void removeWebOrigin(String webOrigin) {
        entity.getWebOrigins().remove(webOrigin);
    }

    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        result.addAll(entity.getRedirectUris());
        return result;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        entity.setRedirectUris(redirectUris);
    }

    public void addRedirectUri(String redirectUri) {
        entity.getRedirectUris().add(redirectUri);
    }

    public void removeRedirectUri(String redirectUri) {
        entity.getRedirectUris().remove(redirectUri);
    }

    public String getSecret() {
        return entity.getSecret();
    }

    public void setSecret(String secret) {
        entity.setSecret(secret);
    }

    public boolean validateSecret(String secret) {
        return secret.equals(entity.getSecret());
    }
}
