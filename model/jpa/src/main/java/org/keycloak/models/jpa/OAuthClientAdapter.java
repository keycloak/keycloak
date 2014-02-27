package org.keycloak.models.jpa;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.OAuthClientEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientAdapter implements OAuthClientModel {
    protected OAuthClientEntity entity;

    public OAuthClientAdapter(OAuthClientEntity entity) {
        this.entity = entity;
    }

    public OAuthClientEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getClientId() {
        return getAgent().getLoginName();
    }

    @Override
    public boolean isEnabled() {
        return getAgent().isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getAgent().setEnabled(enabled);
    }

    @Override
    public UserModel getAgent() {
        return new UserAdapter(entity.getAgent());
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






}
