package org.keycloak.models.mongo.keycloak.adapters;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.api.MongoStore;
import org.keycloak.models.mongo.keycloak.entities.ApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.RealmEntity;
import org.keycloak.models.mongo.keycloak.entities.RoleEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Wrapper around RoleData object, which will persist wrapped object after each set operation (compatibility with picketlink based impl)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleAdapter implements RoleModel {

    private final RoleEntity role;
    private RoleContainerModel roleContainer;
    private final MongoStore mongoStore;

    public RoleAdapter(RoleEntity roleEntity, MongoStore mongoStore) {
        this(roleEntity, null, mongoStore);
    }

    public RoleAdapter(RoleEntity roleEntity, RoleContainerModel roleContainer, MongoStore mongoStore) {
        this.role = roleEntity;
        this.roleContainer = roleContainer;
        this.mongoStore = mongoStore;
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
        return role.isComposite();
    }

    @Override
    public void setComposite(boolean flag) {
        role.setComposite(flag);
        updateRole();
    }

    protected void updateRole() {
        mongoStore.updateObject(role);
    }

    @Override
    public void addCompositeRole(RoleModel childRole) {
        mongoStore.pushItemToList(role, "compositeRoleIds", childRole.getId(), true);
    }

    @Override
    public void removeCompositeRole(RoleModel childRole) {
        mongoStore.pullItemFromList(role, "compositeRoleIds", childRole.getId());
    }

    @Override
    public Set<RoleModel> getComposites() {
        if (role.getCompositeRoleIds() == null || role.getCompositeRoleIds().isEmpty()) {
            return Collections.EMPTY_SET;
        }

        DBObject query = new QueryBuilder()
                .and("_id").in(MongoModelUtils.convertStringsToObjectIds(role.getCompositeRoleIds()))
                .get();
        List<RoleEntity> childRoles = mongoStore.loadObjects(RoleEntity.class, query);

        Set<RoleModel> set = new HashSet<RoleModel>();
        for (RoleEntity childRole : childRoles) {
            set.add(new RoleAdapter(childRole, roleContainer, mongoStore));
        }
        return set;
    }

    @Override
    public RoleContainerModel getContainer() {
        if (roleContainer == null) {
            // Compute it
            if (role.getRealmId() != null) {
                RealmEntity realm = mongoStore.loadObject(RealmEntity.class, role.getRealmId());
                if (realm == null) {
                    throw new IllegalStateException("Realm with id: " + role.getRealmId() + " doesn't exists");
                }
                roleContainer = new RealmAdapter(realm, mongoStore);
            } else if (role.getApplicationId() != null) {
                ApplicationEntity appEntity = mongoStore.loadObject(ApplicationEntity.class, role.getApplicationId());
                if (appEntity == null) {
                    throw new IllegalStateException("Application with id: " + role.getApplicationId() + " doesn't exists");
                }
                roleContainer = new ApplicationAdapter(appEntity, mongoStore);
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

    public RoleEntity getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleAdapter that = (RoleAdapter) o;

        if (!getId().equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
