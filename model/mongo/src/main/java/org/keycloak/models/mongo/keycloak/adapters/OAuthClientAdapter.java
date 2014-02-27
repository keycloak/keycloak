package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter extends AbstractAdapter implements OAuthClientModel {

    private final OAuthClientEntity delegate;
    private UserAdapter oauthAgent;

    public OAuthClientAdapter(OAuthClientEntity oauthClientEntity, UserAdapter oauthAgent, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.delegate = oauthClientEntity;
        this.oauthAgent = oauthAgent;
    }

    public OAuthClientAdapter(OAuthClientEntity oauthClientEntity, MongoStoreInvocationContext invContext) {
        this(oauthClientEntity, null, invContext);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getClientId() {
        return getAgent().getLoginName();
    }

    @Override
    public long getAllowedClaimsMask() {
        return delegate.getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        delegate.setAllowedClaimsMask(mask);
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
        // This is not thread-safe. Assumption is that OAuthClientAdapter instance is per-client object
        if (oauthAgent == null) {
            UserEntity user = getMongoStore().loadEntity(UserEntity.class, delegate.getOauthAgentId(), invocationContext);
            oauthAgent = user!=null ? new UserAdapter(user, invocationContext) : null;
        }
        return oauthAgent;
    }

    @Override
    public AbstractMongoIdentifiableEntity getMongoEntity() {
        return delegate;
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (delegate.getWebOrigins() != null) {
            result.addAll(delegate.getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        delegate.setWebOrigins(result);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getMongoStore().pushItemToList(delegate, "webOrigins", webOrigin, true, invocationContext);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getMongoStore().pullItemFromList(delegate, "webOrigins", webOrigin, invocationContext);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (delegate.getRedirectUris() != null) {
            result.addAll(delegate.getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        delegate.setRedirectUris(result);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getMongoStore().pushItemToList(delegate, "redirectUris", redirectUri, true, invocationContext);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getMongoStore().pullItemFromList(delegate, "redirectUris", redirectUri, invocationContext);
    }

    @Override
    public String getSecret() {
        return delegate.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        delegate.setSecret(secret);
    }


    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(delegate.getSecret());
    }


}
