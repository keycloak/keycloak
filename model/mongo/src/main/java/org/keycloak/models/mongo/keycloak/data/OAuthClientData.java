package org.keycloak.models.mongo.keycloak.data;

import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.NoSQLCollection;
import org.keycloak.models.mongo.api.NoSQLField;
import org.keycloak.models.mongo.api.NoSQLId;
import org.keycloak.models.mongo.api.NoSQLObject;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "oauthClients")
public class OAuthClientData implements NoSQLObject {

    private String id;
    private String baseUrl;

    private String oauthAgentId;
    private String realmId;

    @NoSQLId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NoSQLField
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @NoSQLField
    public String getOauthAgentId() {
        return oauthAgentId;
    }

    public void setOauthAgentId(String oauthUserId) {
        this.oauthAgentId = oauthUserId;
    }

    @NoSQLField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public void afterRemove(NoSQL noSQL) {
        // Remove user of this oauthClient
        noSQL.removeObject(UserData.class, oauthAgentId);
    }
}
