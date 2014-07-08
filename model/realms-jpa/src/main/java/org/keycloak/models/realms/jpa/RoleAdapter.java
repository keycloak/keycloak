package org.keycloak.models.realms.jpa;

import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.realms.Role;
import org.keycloak.models.realms.RoleContainer;
import org.keycloak.models.realms.jpa.entities.RoleEntity;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements Role {
    protected RoleEntity role;
    protected RealmProvider provider;
    protected EntityManager em;

    public RoleAdapter(RealmProvider provider, EntityManager em, RoleEntity role) {
        this.provider = provider;
        this.em = em;
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
        return getComposites().size() > 0;
    }

    @Override
    public void addCompositeRole(Role role) {
        RoleEntity entity = ((RoleAdapter)role).getRole();
        for (RoleEntity composite : getRole().getCompositeRoles()) {
            if (composite.equals(entity)) return;
        }
        getRole().getCompositeRoles().add(entity);
        em.flush();
    }

    @Override
    public void removeCompositeRole(Role role) {
        RoleEntity entity = ((RoleAdapter)role).getRole();
        Iterator<RoleEntity> it = getRole().getCompositeRoles().iterator();
        while (it.hasNext()) {
            if (it.next().equals(entity)) it.remove();
        }
    }

    @Override
    public Set<Role> getComposites() {
        Set<Role> set = new HashSet<Role>();

        for (RoleEntity composite : getRole().getCompositeRoles()) {
           set.add(new RoleAdapter(provider, em, composite));
        }
        return set;
    }

    @Override
    public RoleContainer getContainer() {
        if (role.isApplicationRole()) {
            return provider.getApplicationById(role.getApplication().getId(), role.getApplication().getRealm().getId());
        } else {
            return provider.getRealm(role.getRealm().getId());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Role)) return false;

        Role that = (Role) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
