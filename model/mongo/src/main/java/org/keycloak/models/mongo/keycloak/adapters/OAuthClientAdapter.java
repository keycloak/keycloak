package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.keycloak.entities.OAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientAdapter implements OAuthClientModel {

    private final OAuthClientEntity delegate;
    private UserAdapter oauthAgent;
    private final MongoStore mongoStore;

    public OAuthClientAdapter(OAuthClientEntity oauthClientEntity, UserAdapter oauthAgent, MongoStore mongoStore) {
        this.delegate = oauthClientEntity;
        this.oauthAgent = oauthAgent;
        this.mongoStore = mongoStore;
    }

    public OAuthClientAdapter(OAuthClientEntity oauthClientEntity, MongoStore mongoStore) {
        this(oauthClientEntity, null, mongoStore);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public UserModel getOAuthAgent() {
        // This is not thread-safe. Assumption is that OAuthClientAdapter instance is per-client object
        if (oauthAgent == null) {
            UserEntity user = mongoStore.loadObject(UserEntity.class, delegate.getOauthAgentId());
            oauthAgent = user!=null ? new UserAdapter(user, mongoStore) : null;
        }
        return oauthAgent;
    }

}
