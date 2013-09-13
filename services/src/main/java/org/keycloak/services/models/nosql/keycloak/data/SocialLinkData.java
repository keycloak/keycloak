package org.keycloak.services.models.nosql.keycloak.data;

import org.keycloak.services.models.nosql.api.AbstractNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;

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
