package org.keycloak.models.mongo.keycloak.data;

import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.NoSQLCollection;
import org.keycloak.models.mongo.api.NoSQLField;
import org.keycloak.models.mongo.api.NoSQLId;
import org.keycloak.models.mongo.api.NoSQLObject;
import org.keycloak.models.mongo.api.query.NoSQLQuery;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "applications")
public class ApplicationData implements NoSQLObject {

    private String id;
    private String name;
    private boolean enabled;
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String baseUrl;

    private String resourceUserId;
    private String realmId;

    @NoSQLId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NoSQLField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NoSQLField
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NoSQLField
    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    @NoSQLField
    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    @NoSQLField
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @NoSQLField
    public String getResourceUserId() {
        return resourceUserId;
    }

    public void setResourceUserId(String resourceUserId) {
        this.resourceUserId = resourceUserId;
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
        // Remove resourceUser of this application
        noSQL.removeObject(UserData.class, resourceUserId);

        // Remove all roles, which belongs to this application
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("applicationId", id)
                .build();
        noSQL.removeObjects(RoleData.class, query);
    }
}
