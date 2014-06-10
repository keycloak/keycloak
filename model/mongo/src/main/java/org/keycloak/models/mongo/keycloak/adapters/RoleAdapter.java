package org.keycloak.models.mongo.keycloak.adapters;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRealmEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Wrapper around RoleData object, which will persist wrapped object after each set operation (compatibility with picketlink based idm)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleAdapter extends AbstractMongoAdapter<MongoRoleEntity> implements RoleModel {

    private final MongoRoleEntity role;
    private RoleContainerModel roleContainer;
    private RealmModel realm;

    public RoleAdapter(RealmModel realm, MongoRoleEntity roleEntity, MongoStoreInvocationContext invContext) {
        this(realm, roleEntity, null, invContext);
    }

    public RoleAdapter(RealmModel realm, MongoRoleEntity roleEntity, RoleContainerModel roleContainer, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.role = roleEntity;
        this.roleContainer = roleContainer;
        this.realm = realm;
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
        updateRole();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
        updateRole();
    }

    @Override
    public boolean isComposite() {
        return role.getCompositeRoleIds() != null && role.getCompositeRoleIds().size() > 0;
    }

    protected void updateRole() {
        super.updateMongoEntity();
    }

    @Override
    public void addCompositeRole(RoleModel childRole) {
        getMongoStore().pushItemToList(role, "compositeRoleIds", childRole.getId(), true, invocationContext);
    }

    @Override
    public void removeCompositeRole(RoleModel childRole) {
        getMongoStore().pullItemFromList(role, "compositeRoleIds", childRole.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getComposites() {
        if (role.getCompositeRoleIds() == null || role.getCompositeRoleIds().isEmpty()) {
            return Collections.EMPTY_SET;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(role.getCompositeRoleIds())
                .get();
        List<MongoRoleEntity> childRoles = getMongoStore().loadEntities(MongoRoleEntity.class, query, invocationContext);

        Set<RoleModel> set = new HashSet<RoleModel>();
        for (MongoRoleEntity childRole : childRoles) {
            set.add(new RoleAdapter(realm, childRole, invocationContext));
        }
        return set;
    }

    @Override
    public RoleContainerModel getContainer() {
        if (roleContainer == null) {
            // Compute it
            if (role.getRealmId() != null) {
                MongoRealmEntity realm = getMongoStore().loadEntity(MongoRealmEntity.class, role.getRealmId(), invocationContext);
                if (realm == null) {
                    throw new IllegalStateException("Realm with id: " + role.getRealmId() + " doesn't exists");
                }
                roleContainer = new RealmAdapter(realm, invocationContext);
            } else if (role.getApplicationId() != null) {
                MongoApplicationEntity appEntity = getMongoStore().loadEntity(MongoApplicationEntity.class, role.getApplicationId(), invocationContext);
                if (appEntity == null) {
                    throw new IllegalStateException("Application with id: " + role.getApplicationId() + " doesn't exists");
                }
                roleContainer = new ApplicationAdapter(realm, appEntity, invocationContext);
            } else {
                throw new IllegalStateException("Both realmId and applicationId are null for role: " + this);
            }
        }
        return roleContainer;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) return true;
        if (!isComposite()) return false;

        Set<RoleModel> visited = new HashSet<RoleModel>();
        return KeycloakModelUtils.searchFor(role, this, visited);
    }

    public MongoRoleEntity getRole() {
        return role;
    }

    @Override
    public MongoRoleEntity getMongoEntity() {
        return role;
    }
}
