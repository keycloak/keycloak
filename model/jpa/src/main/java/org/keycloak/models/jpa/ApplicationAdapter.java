package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.*;

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
public class ApplicationAdapter extends ClientAdapter implements ApplicationModel {

    protected EntityManager em;
    protected ApplicationEntity applicationEntity;
    protected RealmModel realm;

    public ApplicationAdapter(RealmModel realm, EntityManager em, ApplicationEntity applicationEntity) {
        super(applicationEntity);
        this.realm = realm;
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
    public RoleModel getRole(String name) {
        TypedQuery<ApplicationRoleEntity> query = em.createNamedQuery("getAppRoleByName", ApplicationRoleEntity.class);
        query.setParameter("name", name);
        query.setParameter("application", entity);
        List<ApplicationRoleEntity> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(realm, em, roles.get(0));
    }

    @Override
    public RoleModel addRole(String name) {
        RoleModel role = getRole(name);
        if (role != null) return role;
        ApplicationRoleEntity roleEntity = new ApplicationRoleEntity();
        roleEntity.setName(name);
        roleEntity.setApplication(applicationEntity);
        em.persist(roleEntity);
        applicationEntity.getRoles().add(roleEntity);
        em.flush();
        return new RoleAdapter(realm, em, roleEntity);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleAdapter roleAdapter = getRoleById(id);
        if (roleAdapter == null) {
            return false;
        }

        ApplicationRoleEntity role = (ApplicationRoleEntity)roleAdapter.getRole();

        applicationEntity.getRoles().remove(role);
        applicationEntity.getDefaultRoles().remove(role);

        em.createQuery("delete from " + ScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        role.setApplication(null);
        em.flush();
        em.remove(role);

        return true;
    }

    @Override
    public Set<RoleModel> getRoles() {
        Set<RoleModel> list = new HashSet<RoleModel>();
        Collection<ApplicationRoleEntity> roles = applicationEntity.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(realm, em, entity));
        }
        return list;
    }

    @Override
    public RoleAdapter getRoleById(String id) {
        RoleEntity entity = em.find(RoleEntity.class, id);

        // Check if it's application role and belongs to this application
        if (entity == null || !(entity instanceof ApplicationRoleEntity)) return null;
        ApplicationRoleEntity appRoleEntity = (ApplicationRoleEntity)entity;
        return (appRoleEntity.getApplication().equals(this.entity)) ? new RoleAdapter(this.realm, em, appRoleEntity) : null;
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(UserModel user) {
        Set<RoleModel> roleMappings = realm.getRoleMappings(user);

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
            } else {
                ApplicationModel app = (ApplicationModel)container;
                if (app.getId().equals(getId())) {
                   appRoles.add(role);
                }
            }
        }

        return appRoles;
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(ClientModel client) {
        Set<RoleModel> roleMappings = realm.getScopeMappings(client);

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
            } else {
                ApplicationModel app = (ApplicationModel)container;
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
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }
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
    public void addScope(RoleModel role) {
        realm.addScopeMapping(this, role);
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ApplicationAdapter)) return false;
        ApplicationAdapter app = (ApplicationAdapter)o;
        return app.getId().equals(getId());
    }

    public String toString() {
        return getName();
    }
}
