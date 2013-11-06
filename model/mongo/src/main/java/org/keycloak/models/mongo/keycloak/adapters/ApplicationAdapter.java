package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.NoSQL;
import org.keycloak.models.mongo.api.query.NoSQLQuery;
import org.keycloak.models.mongo.keycloak.data.ApplicationData;
import org.keycloak.models.mongo.keycloak.data.RoleData;
import org.keycloak.models.mongo.keycloak.data.UserData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void updateApplication() {
        noSQL.saveObject(application);
    }

    @Override
    public UserModel getApplicationUser() {
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
    public void setBaseUrl(String url) {
        application.setBaseUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return application.getBaseUrl();
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
    public RoleModel getRoleById(String id) {
        RoleData role = noSQL.loadObject(RoleData.class, id);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, noSQL);
        }
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();
        noSQL.pushItemToList(userData, "roleIds", role.getId());
    }

    @Override
    public boolean hasRole(UserModel user, String role) {
        RoleModel roleModel = getRole(role);
        return hasRole(user, roleModel);
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();

        List<String> roleIds = userData.getRoleIds();
        String roleId = role.getId();
        if (roleIds != null) {
            for (String currentId : roleIds) {
                if (roleId.equals(currentId)) {
                    return true;
                }
            }
        }
        return false;
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

    // Static so that it can be used from RealmAdapter as well
    static List<RoleData> getAllRolesOfUser(UserModel user, NoSQL noSQL) {
        UserData userData = ((UserAdapter)user).getUser();
        List<String> roleIds = userData.getRoleIds();

        NoSQLQuery query = noSQL.createQueryBuilder()
                .inCondition("_id", roleIds)
                .build();
        return noSQL.loadObjects(RoleData.class, query);
    }

    @Override
    public Set<String> getRoleMappingValues(UserModel user) {
        Set<String> result = new HashSet<String>();
        List<RoleData> roles = getAllRolesOfUser(user, noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(role.getName());
            }
        }
        return result;
    }

    @Override
    public List<RoleModel> getRoleMappings(UserModel user) {
        List<RoleModel> result = new ArrayList<RoleModel>();
        List<RoleData> roles = getAllRolesOfUser(user, noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(role, noSQL));
            }
        }
        return result;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();
        noSQL.pullItemFromList(userData, "roleIds", role.getId());
    }

    @Override
    public void addScopeMapping(UserModel agent, String roleName) {
        RoleAdapter role = getRole(roleName);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        addScopeMapping(agent, role);
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        UserData userData = ((UserAdapter)agent).getUser();
        noSQL.pushItemToList(userData, "scopeIds", role.getId());
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        UserData userData = ((UserAdapter)user).getUser();
        noSQL.pullItemFromList(userData, "scopeIds", role.getId());
    }

    // Static so that it can be used from RealmAdapter as well
    static List<RoleData> getAllScopesOfUser(UserModel user, NoSQL noSQL) {
        UserData userData = ((UserAdapter)user).getUser();
        List<String> roleIds = userData.getScopeIds();

        NoSQLQuery query = noSQL.createQueryBuilder()
                .inCondition("_id", roleIds)
                .build();
        return noSQL.loadObjects(RoleData.class, query);
    }

    @Override
    public Set<String> getScopeMappingValues(UserModel agent) {
        Set<String> result = new HashSet<String>();
        List<RoleData> roles = getAllScopesOfUser(agent,  noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(role.getName());
            }
        }
        return result;
    }

    @Override
    public List<RoleModel> getScopeMappings(UserModel agent) {
        List<RoleModel> result = new ArrayList<RoleModel>();
        List<RoleData> roles = getAllScopesOfUser(agent,  noSQL);
        // TODO: Maybe improve as currently we need to obtain all roles and then filter programmatically...
        for (RoleData role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(role, noSQL));
            }
        }
        return result;
    }

    @Override
    public List<String> getDefaultRoles() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addDefaultRole(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
