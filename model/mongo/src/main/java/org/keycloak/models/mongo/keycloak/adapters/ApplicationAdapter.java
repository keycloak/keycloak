package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
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
    private final RealmModel realm;

    public ApplicationAdapter(RealmModel realm, ApplicationEntity applicationEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.application = applicationEntity;
        this.realm = realm;
    }

    @Override
    public void updateApplication() {
        getMongoStore().updateEntity(application, invocationContext);
    }

    @Override
    public String getId() {
        return application.getId();
    }

    @Override
    public String getClientId() {
        return getName();
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
    public RealmModel getRealm() {
        return realm;
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
    public long getAllowedClaimsMask() {
        return application.getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        application.setAllowedClaimsMask(mask);
    }

    @Override
    public int getNotBefore() {
        return application.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        application.setNotBefore(notBefore);
    }

    @Override
    public boolean isBearerOnly() {
        return application.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        application.setBearerOnly(only);
    }

    @Override
    public boolean isPublicClient() {
        return application.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        application.setPublicClient(flag);
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
        getMongoStore().pushItemToList(application, "scopeIds", role.getId(), true, invocationContext);
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

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (application.getWebOrigins() != null) {
            result.addAll(application.getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        application.setWebOrigins(result);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getMongoStore().pushItemToList(application, "webOrigins", webOrigin, true, invocationContext);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getMongoStore().pullItemFromList(application, "webOrigins", webOrigin, invocationContext);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (application.getRedirectUris() != null) {
            result.addAll(application.getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        application.setRedirectUris(result);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getMongoStore().pushItemToList(application, "redirectUris", redirectUri, true, invocationContext);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getMongoStore().pullItemFromList(application, "redirectUris", redirectUri, invocationContext);
    }

    @Override
    public String getSecret() {
        return application.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        application.setSecret(secret);
    }


    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(application.getSecret());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationAdapter)) return false;
        if (!super.equals(o)) return false;

        ApplicationAdapter that = (ApplicationAdapter) o;

        if (!application.getId().equals(that.application.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return application.getId().hashCode();
    }
}
