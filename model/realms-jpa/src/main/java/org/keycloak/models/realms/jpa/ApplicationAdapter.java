package org.keycloak.models.realms.jpa;

import org.keycloak.models.realms.Application;
import org.keycloak.models.realms.Client;
import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.realms.Role;
import org.keycloak.models.realms.RoleContainer;
import org.keycloak.models.realms.jpa.entities.ScopeMappingEntity;
import org.keycloak.models.realms.jpa.entities.ApplicationEntity;
import org.keycloak.models.realms.jpa.entities.RoleEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationAdapter extends ClientAdapter implements Application {

    protected EntityManager em;
    protected ApplicationEntity applicationEntity;

    public ApplicationAdapter(RealmProvider provider, EntityManager em, ApplicationEntity applicationEntity) {
        super(provider, applicationEntity, em);
        this.em = em;
        this.applicationEntity = applicationEntity;
    }

    @Override
    public void updateApplication() {
        em.flush();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return applicationEntity.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        applicationEntity.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return applicationEntity.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        applicationEntity.setManagementUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return applicationEntity.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        applicationEntity.setBaseUrl(url);
    }

    @Override
    public boolean isBearerOnly() {
        return applicationEntity.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        applicationEntity.setBearerOnly(only);
    }

    @Override
    public boolean isDirectGrantsOnly() {
        return false;  // applications can't be grant only
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        // applications can't be grant only
    }

    @Override
    public Role getRole(String name) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getAppRoleByName", RoleEntity.class);
        query.setParameter("name", name);
        query.setParameter("application", entity);
        List<RoleEntity> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(provider, em, roles.get(0));
    }

    @Override
    public Role addRole(String id, String name) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setApplication(applicationEntity);
        roleEntity.setApplicationRole(true);
        roleEntity.setRealmId(entity.getRealm().getId());
        em.persist(roleEntity);
        applicationEntity.getRoles().add(roleEntity);
        em.flush();
        return new RoleAdapter(provider, em, roleEntity);
    }

    @Override
    public boolean removeRole(Role Role) {
        RoleAdapter roleAdapter = (RoleAdapter) Role;
        if (Role == null) {
            return false;
        }
        if (!roleAdapter.getContainer().equals(this)) return false;

        if (!roleAdapter.getRole().isApplicationRole()) return false;

        RoleEntity role = roleAdapter.getRole();

        applicationEntity.getRoles().remove(role);
        applicationEntity.getDefaultRoles().remove(role);
        em.createNativeQuery("delete from CompositeRole where childRole = :role").setParameter("role", role).executeUpdate();
        em.createQuery("delete from " + ScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        role.setApplication(null);
        em.flush();
        em.remove(role);
        em.flush();

        return true;
    }

    @Override
    public Set<Role> getRoles() {
        Set<Role> list = new HashSet<Role>();
        Collection<RoleEntity> roles = applicationEntity.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(provider, em, entity));
        }
        return list;
    }

    @Override
    public Set<Role> getApplicationScopeMappings(Client client) {
        Set<Role> roleMappings = client.getScopeMappings();

        Set<Role> appRoles = new HashSet<Role>();
        for (Role role : roleMappings) {
            RoleContainer container = role.getContainer();
            if (container instanceof Realm) {
            } else {
                Application app = (Application)container;
                if (app.getId().equals(getId())) {
                    appRoles.add(role);
                }
            }
        }

        return appRoles;
    }




    @Override
    public List<String> getDefaultRoles() {
        Collection<RoleEntity> entities = applicationEntity.getDefaultRoles();
        List<String> roles = new ArrayList<String>();
        if (entities == null) return roles;
        for (RoleEntity entity : entities) {
            roles.add(entity.getName());
        }
        return roles;
    }

    @Override
    public void addDefaultRole(String name) {
        Role role = getRole(name);
        Collection<RoleEntity> entities = applicationEntity.getDefaultRoles();
        for (RoleEntity entity : entities) {
            if (entity.getId().equals(role.getId())) {
                return;
            }
        }
        entities.add(((RoleAdapter) role).getRole());
        em.flush();
    }

    public static boolean contains(String str, String[] array) {
        for (String s : array) {
            if (str.equals(s)) return true;
        }
        return false;
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        Collection<RoleEntity> entities = applicationEntity.getDefaultRoles();
        Set<String> already = new HashSet<String>();
        List<RoleEntity> remove = new ArrayList<RoleEntity>();
        for (RoleEntity rel : entities) {
            if (!contains(rel.getName(), defaultRoles)) {
                remove.add(rel);
            } else {
                already.add(rel.getName());
            }
        }
        for (RoleEntity entity : remove) {
            entities.remove(entity);
        }
        em.flush();
        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(roleName);
            }
        }
        em.flush();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Application)) return false;

        Application that = (Application) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public String toString() {
        return getName();
    }

    ApplicationEntity getJpaEntity() {
        return applicationEntity;
    }
}
