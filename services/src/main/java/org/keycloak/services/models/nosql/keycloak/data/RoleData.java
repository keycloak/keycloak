package org.keycloak.services.models.nosql.keycloak.data;

import java.util.List;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "roles")
public class RoleData implements NoSQLObject {

    private static final Logger logger = Logger.getLogger(RoleData.class);

    private String id;
    private String name;
    private String description;

    private String realmId;
    private String applicationId;

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
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NoSQLField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @NoSQLField
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public void afterRemove(NoSQL noSQL) {
        // Remove this role from all users, which has it
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("roleIds", id)
                .build();

        List<UserData> users = noSQL.loadObjects(UserData.class, query);
        for (UserData user : users) {
            logger.info("Removing role " + getName() + " from user " + user.getLoginName());
            noSQL.pullItemFromList(user, "roleIds", getId());
        }

        // Remove this scope from all users, which has it
        query = noSQL.createQueryBuilder()
                .andCondition("scopeIds", id)
                .build();

        users = noSQL.loadObjects(UserData.class, query);
        for (UserData user : users) {
            logger.info("Removing scope " + getName() + " from user " + user.getLoginName());
            noSQL.pullItemFromList(user, "scopeIds", getId());
        }

    }
}
