package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
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

    public ApplicationAdapter(ApplicationEntity applicationEntity, MongoStoreInvocationContext invContext) {
        this(applicationEntity, null, invContext);
    }

    public ApplicationAdapter(ApplicationEntity applicationEntity, UserAdapter resourceUser, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.application = applicationEntity;
        this.resourceUser = resourceUser;
    }

    @Override
    public void updateApplication() {
        getMongoStore().updateEntity(application, invocationContext);
    }

    @Override
    public UserAdapter getApplicationUser() {
        // This is not thread-safe. Assumption is that ApplicationAdapter instance is per-client object
        if (resourceUser == null) {
            UserEntity userEntity = getMongoStore().loadEntity(UserEntity.class, application.getResourceUserId(), invocationContext);
            if (userEntity == null) {
                throw new IllegalStateException("User " + application.getResourceUserId() + " not found");
            }
            resourceUser = new UserAdapter(userEntity, invocationContext);
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
        RoleEntity role = getMongoStore().loadSingleEntity(RoleEntity.class, query, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, invocationContext);
        }
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity role = getMongoStore().loadEntity(RoleEntity.class, id, invocationContext);
        if (role == null) {
            return null;
        } else {
            return new RoleAdapter(role, this, invocationContext);
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

        getMongoStore().insertEntity(roleEntity, invocationContext);
        return new RoleAdapter(roleEntity, this, invocationContext);
    }

    @Override
    public boolean removeRoleById(String id) {
        return getMongoStore().removeEntity(RoleEntity.class, id, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("applicationId").is(getId())
                .get();
        List<RoleEntity> roles = getMongoStore().loadEntities(RoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();
        for (RoleEntity role : roles) {
            result.add(new RoleAdapter(role, this, invocationContext));
        }

        return result;
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllRolesOfUser(user, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(role, this, invocationContext));
            }
        }
        return result;
    }

    @Override
    public void addScope(RoleModel role) {
        UserAdapter appUser = getApplicationUser();
        getMongoStore().pushItemToList(appUser.getUser(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllScopesOfUser(user, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(role, this, invocationContext));
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

        getMongoStore().pushItemToList(application, "defaultRoles", name, true, invocationContext);
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
