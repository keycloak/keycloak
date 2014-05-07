package org.keycloak.models.mongo.keycloak.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAdapter<T extends MongoIdentifiableEntity> extends AbstractMongoAdapter<T> implements ClientModel {

    protected final T clientEntity;
    private final RealmModel realm;

    public ClientAdapter(RealmModel realm, T clientEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.clientEntity = clientEntity;
        this.realm = realm;
    }

    @Override
    public T getMongoEntity() {
        return clientEntity;
    }

    // ClientEntity doesn't extend MongoIdentifiableEntity
    public ClientEntity getMongoEntityAsClient() {
        return (ClientEntity)getMongoEntity();
    }

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getClientId() {
        return getMongoEntityAsClient().getName();
    }

    @Override
    public long getAllowedClaimsMask() {
        return getMongoEntityAsClient().getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        getMongoEntityAsClient().setAllowedClaimsMask(mask);
        updateMongoEntity();
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntityAsClient().getWebOrigins() != null) {
            result.addAll(getMongoEntityAsClient().getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        getMongoEntityAsClient().setWebOrigins(result);
        updateMongoEntity();
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getMongoStore().pushItemToList(clientEntity, "webOrigins", webOrigin, true, invocationContext);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getMongoStore().pullItemFromList(clientEntity, "webOrigins", webOrigin, invocationContext);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntityAsClient().getRedirectUris() != null) {
            result.addAll(getMongoEntityAsClient().getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        getMongoEntityAsClient().setRedirectUris(result);
        updateMongoEntity();
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getMongoStore().pushItemToList(clientEntity, "redirectUris", redirectUri, true, invocationContext);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getMongoStore().pullItemFromList(clientEntity, "redirectUris", redirectUri, invocationContext);
    }

    @Override
    public boolean isEnabled() {
        return getMongoEntityAsClient().isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getMongoEntityAsClient().setEnabled(enabled);
        updateMongoEntity();
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(getMongoEntityAsClient().getSecret());
    }

    @Override
    public String getSecret() {
        return getMongoEntityAsClient().getSecret();
    }

    @Override
    public void setSecret(String secret) {
        getMongoEntityAsClient().setSecret(secret);
        updateMongoEntity();
    }

    @Override
    public boolean isPublicClient() {
        return getMongoEntityAsClient().isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        getMongoEntityAsClient().setPublicClient(flag);
        updateMongoEntity();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        return getMongoEntityAsClient().getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        getMongoEntityAsClient().setNotBefore(notBefore);
        updateMongoEntity();
    }

}
