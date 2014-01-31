package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoId;
import org.keycloak.models.mongo.api.MongoStore;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "oauthClients")
public class OAuthClientEntity implements MongoEntity {

    private String id;
    private String name;

    private String oauthAgentId;
    private String realmId;

    @MongoId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
    public void afterRemove(MongoStore mongoStore) {
        // Remove user of this oauthClient
        mongoStore.removeObject(UserEntity.class, oauthAgentId);
    }
}
