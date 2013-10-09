package org.keycloak.models.mongo.keycloak.data;

import org.keycloak.models.mongo.api.AbstractNoSQLObject;
import org.keycloak.models.mongo.api.NoSQLCollection;
import org.keycloak.models.mongo.api.NoSQLField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "socialLinks")
public class SocialLinkData extends AbstractNoSQLObject {

    private String socialUsername;
    private String socialProvider;
    private String userId;
    // realmId is needed to allow searching as combination socialUsername+socialProvider may not be unique
    // (Same user could have mapped same facebook account to username "foo" in "realm1" and to username "bar" in "realm2")
    private String realmId;

    @NoSQLField
    public String getSocialUsername() {
        return socialUsername;
    }

    public void setSocialUsername(String socialUsername) {
        this.socialUsername = socialUsername;
    }

    @NoSQLField
    public String getSocialProvider() {
        return socialProvider;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    @NoSQLField
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @NoSQLField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }
}
