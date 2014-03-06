package org.keycloak.models.mongo.keycloak.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.ClientEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAdapter<T extends ClientEntity> extends AbstractMongoAdapter<T> implements ClientModel {

    private final T clientEntity;
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

    @Override
    public String getId() {
        return getMongoEntity().getId();
    }

    @Override
    public String getClientId() {
        return getMongoEntity().getName();
    }

    @Override
    public long getAllowedClaimsMask() {
        return getMongoEntity().getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        getMongoEntity().setAllowedClaimsMask(mask);
        updateMongoEntity();
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (getMongoEntity().getWebOrigins() != null) {
            result.addAll(clientEntity.getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        clientEntity.setWebOrigins(result);
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
        if (clientEntity.getRedirectUris() != null) {
            result.addAll(clientEntity.getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        clientEntity.setRedirectUris(result);
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
        return clientEntity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        clientEntity.setEnabled(enabled);
        updateMongoEntity();
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(clientEntity.getSecret());
    }

    @Override
    public String getSecret() {
        return clientEntity.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        clientEntity.setSecret(secret);
        updateMongoEntity();
    }

    @Override
    public boolean isPublicClient() {
        return clientEntity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        clientEntity.setPublicClient(flag);
        updateMongoEntity();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        return clientEntity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        clientEntity.setNotBefore(notBefore);
        updateMongoEntity();
    }
}
