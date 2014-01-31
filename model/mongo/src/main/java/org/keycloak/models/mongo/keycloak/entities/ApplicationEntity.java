package org.keycloak.models.mongo.keycloak.entities;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.MongoId;
import org.keycloak.models.mongo.api.MongoStore;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "applications")
public class ApplicationEntity implements MongoEntity {

    private String id;
    private String name;
    private boolean enabled;
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String baseUrl;

    private String resourceUserId;
    private String realmId;

    // We are using names of defaultRoles (not ids)
    private List<String> defaultRoles = new ArrayList<String>();

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
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @MongoField
    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    @MongoField
    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }

    @MongoField
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @MongoField
    public String getResourceUserId() {
        return resourceUserId;
    }

    public void setResourceUserId(String resourceUserId) {
        this.resourceUserId = resourceUserId;
    }

    @MongoField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @MongoField
    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    @Override
    public void afterRemove(MongoStore mongoStore) {
        // Remove resourceUser of this application
        mongoStore.removeObject(UserEntity.class, resourceUserId);

        // Remove all roles, which belongs to this application
        DBObject query = new QueryBuilder()
                .and("applicationId").is(id)
                .get();
        mongoStore.removeObjects(RoleEntity.class, query);
    }
}
