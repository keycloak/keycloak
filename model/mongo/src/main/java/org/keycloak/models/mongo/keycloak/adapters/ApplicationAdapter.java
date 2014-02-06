package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.ApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.keycloak.entities.UserEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationAdapter extends AbstractAdapter implements ApplicationModel {

    private final ApplicationEntity application;
    private UserAdapter resourceUser;

    public ApplicationAdapter(ApplicationEntity applicationEntity, MongoStore mongoStore, MongoStoreInvocationContext invContext) {
        this(applicationEntity, null, mongoStore, invContext);
    }

    public ApplicationAdapter(ApplicationEntity applicationEntity, UserAdapter resourceUser, MongoStore mongoStore, MongoStoreInvocationContext invContext) {
        super(mongoStore, invContext);
        this.application = applicationEntity;
        this.resourceUser = resourceUser;
    }

    @Override
    public void updateApplication() {
        mongoStore.updateObject(application, invocationContext);
    }

    @Override
    public UserAdapter getApplicationUser() {
        // This is not thread-safe. Assumption is that ApplicationAdapter instance is per-client object
        if (resourceUser == null) {
            UserEntity userEntity = mongoStore.loadObject(UserEntity.class, application.getResourceUserId(), invocationContext);
            if (userEntity == null) {
                throw new IllegalStateException("User " + application.getResourceUserId() + " not found");
            }
            resourceUser = new UserAdapter(userEntity, mongoStore, invocationContext);
        }

        return resourceUser;
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
        DBObject query = new QueryBuilder()
                .and("name").is(name)
                .and("applicationId").is(getId())
                .get();
        RoleEntity role = mongoStore.loadSingleObject(RoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, this, mongoStore, invocationContext);
        }
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity role = mongoStore.loadObject(RoleEntity.class, id, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, this, mongoStore, invocationContext);
        }
    }

    @Override
    public RoleAdapter addRole(String name) {
        RoleAdapter existing = getRole(name);
        if (existing != null) {
            return existing;
        }

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(name);
        roleEntity.setApplicationId(getId());

        mongoStore.insertObject(roleEntity, invocationContext);
        return new RoleAdapter(roleEntity, this, mongoStore, invocationContext);
    }

    @Override
    public boolean removeRoleById(String id) {
        return mongoStore.removeObject(RoleEntity.class ,id, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("applicationId").is(getId())
                .get();
        List<RoleEntity> roles = mongoStore.loadObjects(RoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();
        for (RoleEntity role : roles) {
            result.add(new RoleAdapter(role, this, mongoStore, invocationContext));
        }

        return result;
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllRolesOfUser(user, mongoStore, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(role, this, mongoStore, invocationContext));
            }
        }
        return result;
    }

    @Override
    public void addScope(RoleModel role) {
        UserAdapter appUser = getApplicationUser();
        mongoStore.pushItemToList(appUser.getUser(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllScopesOfUser(user, mongoStore, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(role, this, mongoStore, invocationContext));
            }
        }
        return result;
    }

    @Override
    public List<String> getDefaultRoles() {
        return application.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }

        mongoStore.pushItemToList(application, "defaultRoles", name, true, invocationContext);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        List<String> roleNames = new ArrayList<String>();
        for (String roleName : defaultRoles) {
            RoleModel role = getRole(roleName);
            if (role == null) {
                addRole(roleName);
            }

            roleNames.add(roleName);
        }

        application.setDefaultRoles(roleNames);
    }

    @Override
    public AbstractMongoIdentifiableEntity getMongoEntity() {
        return application;
    }
}
