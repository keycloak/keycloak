package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.ApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ApplicationAdapter extends ClientAdapter<ApplicationEntity> implements ApplicationModel {

    public ApplicationAdapter(RealmModel realm, ApplicationEntity applicationEntity, MongoStoreInvocationContext invContext) {
        super(realm, applicationEntity, invContext);
    }

    @Override
    public void updateApplication() {
        updateMongoEntity();
    }

    @Override
    public String getName() {
        return getMongoEntity().getName();
    }

    @Override
    public void setName(String name) {
        getMongoEntity().setName(name);
        updateMongoEntity();
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return getMongoEntity().isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        getMongoEntity().setSurrogateAuthRequired(surrogateAuthRequired);
        updateMongoEntity();
    }

    @Override
    public String getManagementUrl() {
        return getMongoEntity().getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        getMongoEntity().setManagementUrl(url);
        updateMongoEntity();
    }

    @Override
    public void setBaseUrl(String url) {
        getMongoEntity().setBaseUrl(url);
        updateMongoEntity();
    }

    @Override
    public String getBaseUrl() {
        return getMongoEntity().getBaseUrl();
    }

    @Override
    public boolean isBearerOnly() {
        return getMongoEntity().isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        getMongoEntity().setBearerOnly(only);
        updateMongoEntity();
    }

    @Override
    public boolean isPublicClient() {
        return getMongoEntity().isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        getMongoEntity().setPublicClient(flag);
        updateMongoEntity();
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
            return new RoleAdapter(getRealm(), role, invocationContext);
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
        return new RoleAdapter(getRealm(), roleEntity, this, invocationContext);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return getMongoStore().removeEntity(RoleEntity.class, role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getRoles() {
        DBObject query = new QueryBuilder()
                .and("applicationId").is(getId())
                .get();
        List<RoleEntity> roles = getMongoStore().loadEntities(RoleEntity.class, query, invocationContext);

        Set<RoleModel> result = new HashSet<RoleModel>();
        for (RoleEntity role : roles) {
            result.add(new RoleAdapter(getRealm(), role, this, invocationContext));
        }

        return result;
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(UserModel user) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllRolesOfUser(user, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(getRealm(), role, this, invocationContext));
            }
        }
        return result;
    }

    @Override
    public void addScope(RoleModel role) {
        getMongoStore().pushItemToList(getMongoEntity(), "scopeIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(ClientModel client) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleEntity> roles = MongoModelUtils.getAllScopesOfClient(client, invocationContext);

        for (RoleEntity role : roles) {
            if (getId().equals(role.getApplicationId())) {
                result.add(new RoleAdapter(getRealm(), role, this, invocationContext));
            }
        }
        return result;
    }

    @Override
    public List<String> getDefaultRoles() {
        return getMongoEntity().getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }

        getMongoStore().pushItemToList(getMongoEntity(), "defaultRoles", name, true, invocationContext);
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

        getMongoEntity().setDefaultRoles(roleNames);
        updateMongoEntity();
    }
}
