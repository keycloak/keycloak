package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.AbstractMongoEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "socialLinks")
public class SocialLinkEntity extends AbstractMongoEntity {

    private String socialUsername;
    private String socialProvider;
    private String userId;
    // realmId is needed to allow searching as combination socialUsername+socialProvider may not be unique
    // (Same user could have mapped same facebook account to username "foo" in "realm1" and to username "bar" in "realm2")
    private String realmId;

    @MongoField
    public String getSocialUsername() {
        return socialUsername;
    }

    public void setSocialUsername(String socialUsername) {
        this.socialUsername = socialUsername;
    }

    @MongoField
    public String getSocialProvider() {
        return socialProvider;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    @MongoField
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @MongoField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }
}
