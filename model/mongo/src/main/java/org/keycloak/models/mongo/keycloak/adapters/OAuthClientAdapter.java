package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter extends ClientAdapter<OAuthClientEntity> implements OAuthClientModel {

    public OAuthClientAdapter(RealmModel realm, OAuthClientEntity oauthClientEntity, MongoStoreInvocationContext invContext) {
        super(realm, oauthClientEntity, invContext);
    }

    @Override
    public void setClientId(String id) {
        getMongoEntity().setName(id);
        updateMongoEntity();
    }
}
