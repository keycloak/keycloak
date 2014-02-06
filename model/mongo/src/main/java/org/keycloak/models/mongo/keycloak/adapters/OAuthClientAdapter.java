package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter extends AbstractAdapter implements OAuthClientModel {

    private final OAuthClientEntity delegate;
    private UserAdapter oauthAgent;

    public OAuthClientAdapter(OAuthClientEntity oauthClientEntity, UserAdapter oauthAgent, MongoStore mongoStore, MongoStoreInvocationContext invContext) {
        super(mongoStore, invContext);
        this.delegate = oauthClientEntity;
        this.oauthAgent = oauthAgent;
    }

    public OAuthClientAdapter(OAuthClientEntity oauthClientEntity, MongoStore mongoStore, MongoStoreInvocationContext invContext) {
        this(oauthClientEntity, null, mongoStore, invContext);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public UserModel getOAuthAgent() {
        // This is not thread-safe. Assumption is that OAuthClientAdapter instance is per-client object
        if (oauthAgent == null) {
            UserEntity user = mongoStore.loadObject(UserEntity.class, delegate.getOauthAgentId(), invocationContext);
            oauthAgent = user!=null ? new UserAdapter(user, mongoStore, invocationContext) : null;
        }
        return oauthAgent;
    }

    @Override
    public AbstractMongoIdentifiableEntity getMongoEntity() {
        return delegate;
    }
}
