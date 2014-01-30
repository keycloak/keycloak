package org.keycloak.models.jpa;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ApplicationRoleEntity;
import org.keycloak.models.jpa.entities.RealmRoleEntity;
import org.keycloak.models.jpa.entities.RoleEntity;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel {
    protected RoleEntity role;
    protected EntityManager em;
    protected RealmModel realm;

    public RoleAdapter(RealmModel realm, EntityManager em, RoleEntity role) {
        this.realm = realm;
        this.role = role;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
    }

    @Override
    public boolean isComposite() {
        return role.isComposite();
    }

    @Override
    public void setComposite(boolean flag) {
        role.setComposite(flag);
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        RoleEntity entity = ((RoleAdapter)role).getRole();
        for (RoleEntity composite : getRole().getCompositeRoles()) {
            if (composite.equals(entity)) return;
        }
        getRole().getCompositeRoles().add(entity);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        RoleEntity entity = ((RoleAdapter)role).getRole();
        Iterator<RoleEntity> it = getRole().getCompositeRoles().iterator();
        while (it.hasNext()) {
            if (it.next().equals(entity)) it.remove();
        }
    }

    @Override
    public Set<RoleModel> getComposites() {
        Set<RoleModel> set = new HashSet<RoleModel>();

        for (RoleEntity composite : getRole().getCompositeRoles()) {
           set.add(new RoleAdapter(realm, em, composite));
        }
        return set;
    }

    @Override
    public RoleContainerModel getContainer() {
        if (role instanceof ApplicationRoleEntity) {
            ApplicationRoleEntity entity = (ApplicationRoleEntity)role;
            return new ApplicationAdapter(realm, em, entity.getApplication());
        } else if (role instanceof RealmRoleEntity) {
            RealmRoleEntity entity = (RealmRoleEntity)role;
            return new RealmAdapter(em, entity.getRealm());

        }
        throw new IllegalStateException("Unknown role entity type");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleAdapter that = (RoleAdapter) o;

        if (!role.equals(that.role)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return role.hashCode();
    }
}
