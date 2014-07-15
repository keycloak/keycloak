package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter extends ClientAdapter<MongoOAuthClientEntity> implements OAuthClientModel {

    public OAuthClientAdapter(KeycloakSession session, RealmModel realm, MongoOAuthClientEntity oauthClientEntity, MongoStoreInvocationContext invContext) {
        super(session, realm, oauthClientEntity, invContext);
    }

    @Override
    public void setClientId(String id) {
        getMongoEntity().setName(id);
        updateMongoEntity();
    }

    @Override
    public boolean isDirectGrantsOnly() {
        return getMongoEntity().isDirectGrantsOnly();
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        getMongoEntity().setDirectGrantsOnly(flag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OAuthClientModel)) return false;

        OAuthClientModel that = (OAuthClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
