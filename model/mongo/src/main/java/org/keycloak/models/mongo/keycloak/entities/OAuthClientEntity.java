package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "oauthClients")
public class OAuthClientEntity extends AbstractMongoIdentifiableEntity implements MongoEntity {

    private String name;

    private String oauthAgentId;
    private String realmId;

    @MongoField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @MongoField
    public String getOauthAgentId() {
        return oauthAgentId;
    }

    public void setOauthAgentId(String oauthUserId) {
        this.oauthAgentId = oauthUserId;
    }

    @MongoField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
        // Remove user of this oauthClient
        context.getMongoStore().removeEntity(UserEntity.class, oauthAgentId, context);
    }
}
