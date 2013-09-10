package org.keycloak.services.models.nosql.keycloak.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.services.models.ApplicationModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.nosql.api.NoSQL;
import org.keycloak.services.models.nosql.api.query.NoSQLQuery;
import org.keycloak.services.models.nosql.keycloak.data.ApplicationData;
import org.keycloak.services.models.nosql.keycloak.data.RoleData;
import org.keycloak.services.models.nosql.keycloak.data.UserData;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationAdapter implements ApplicationModel {

    private final ApplicationData application;
    private final NoSQL noSQL;

    private UserData resourceUser;

    public ApplicationAdapter(ApplicationData applicationData, NoSQL noSQL) {
        this.application = applicationData;
        this.noSQL = noSQL;
    }

    @Override
    public void updateResource() {
        noSQL.saveObject(application);
    }

    @Override
    public UserModel getResourceUser() {
        // This is not thread-safe. Assumption is that ApplicationAdapter instance is per-client object
        if (resourceUser == null) {
            resourceUser = noSQL.loadObject(UserData.class, application.getResourceUserId());
        }

        return resourceUser != null ? new UserAdapter(resourceUser, noSQL) : null;
    }

    @Override
    public String getId() {
        return application.getId();
    }

    @Override
    public String getName() {
        return application.getName();
    }

    @Override
    public void setName(String name) {
        application.setName(name);
    }

    @Override
    public boolean isEnabled() {
        return application.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        application.setEnabled(enabled);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return application.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        application.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return application.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        application.setManagementUrl(url);
    }

    @Override
    public RoleAdapter getRole(String name) {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("name", name)
                .andCondition("applicationId", getId())
                .build();
        RoleData role = noSQL.loadSingleObject(RoleData.class, query);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, noSQL);
        }
    }

    @Override
    public RoleAdapter addRole(String name) {
        if (getRole(name) != null) {
            throw new IllegalArgumentException("Role " + name + " already exists");
        }

        RoleData roleData = new RoleData();
        roleData.setName(name);
        roleData.setApplicationId(getId());

        noSQL.saveObject(roleData);
        return new RoleAdapter(roleData, noSQL);
    }

    @Override
    public List<RoleModel> getRoles() {
        NoSQLQuery query = noSQL.createQueryBuilder()
                .andCondition("applicationId", getId())
                .build();
        List<RoleData> roles = noSQL.loadObjects(RoleData.class, query);

        List<RoleModel> result = new ArrayList<RoleModel>();
        for (RoleData role : roles) {
            result.add(new RoleAdapter(role, noSQL));
        }

        return result;
    }

    @Override
    public Set<String> getRoleMappings(UserModel user) {
        UserData userData = ((UserAdapter)user).getUser();
        List<String> roleIds = userData.getRoleIds();

        Set<String> result = new HashSet<String>();

        NoSQLQuery query = noSQL.createQueryBuilder()
                .inCondition("_id", roleIds)
                .build();
        List<RoleData> roles = noSQL.loadObjects(RoleData.class, query);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(role.getName());
            }
        }
        return result;
    }

    @Override
    public void addScope(UserModel agent, String roleName) {
        RoleAdapter role = getRole(roleName);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        addScope(agent, role);
    }

    @Override
    public void addScope(UserModel agent, RoleModel role) {
        UserData userData = ((UserAdapter)agent).getUser();
        noSQL.pushItemToList(userData, "scopeIds", role.getId());
    }

    @Override
    public Set<String> getScope(UserModel agent) {
        UserData userData = ((UserAdapter)agent).getUser();
        List<String> scopeIds = userData.getScopeIds();

        Set<String> result = new HashSet<String>();

        NoSQLQuery query = noSQL.createQueryBuilder()
                .inCondition("_id", scopeIds)
                .build();
        List<RoleData> roles = noSQL.loadObjects(RoleData.class, query);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(role.getName());
            }
        }
        return result;
    }
}
