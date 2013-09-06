package org.keycloak.services.models.nosql.keycloak.data;

import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;

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
