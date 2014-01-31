package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.representations.idm.ApplicationMappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationAdapter implements ApplicationModel {

    protected EntityManager em;
    protected ApplicationEntity application;
    protected RealmModel realm;

    public ApplicationAdapter(RealmModel realm, EntityManager em, ApplicationEntity application) {
        this.realm = realm;
        this.em = em;
        this.application = application;
    }

    @Override
    public void updateApplication() {
        em.flush();
    }

    @Override
    public UserModel getApplicationUser() {
        return new UserAdapter(application.getApplicationUser());
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
    public String getBaseUrl() {
        return application.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        application.setBaseUrl(url);
    }

    @Override
    public RoleModel getRole(String name) {
        TypedQuery<ApplicationRoleEntity> query = em.createNamedQuery("getAppRoleByName", ApplicationRoleEntity.class);
        query.setParameter("name", name);
        query.setParameter("application", application);
        List<ApplicationRoleEntity> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(realm, em, roles.get(0));
    }

    @Override
    public RoleModel addRole(String name) {
        RoleModel role = getRole(name);
        if (role != null) return role;
        ApplicationRoleEntity entity = new ApplicationRoleEntity();
        entity.setName(name);
        entity.setApplication(application);
        em.persist(entity);
        application.getRoles().add(entity);
        em.flush();
        return new RoleAdapter(realm, em, entity);
    }

    @Override
    public boolean removeRoleById(String id) {
        ApplicationRoleEntity role = em.find(ApplicationRoleEntity.class, id);
        if (role == null) {
            return false;
        }

        application.getRoles().remove(role);
        application.getDefaultRoles().remove(role);

        em.createQuery("delete from " + UserScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        em.createQuery("delete from " + UserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        role.setApplication(null);
        em.flush();
        em.remove(role);

        return true;
    }

    @Override
    public Set<RoleModel> getRoles() {
        Set<RoleModel> list = new HashSet<RoleModel>();
        Collection<ApplicationRoleEntity> roles = application.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(realm, em, entity));
        }
        return list;
    }

    @Override
    public RoleModel getRoleById(String id) {
        return realm.getRoleById(id);
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
    public Set<RoleModel> getApplicationScopeMappings(UserModel user) {
        Set<RoleModel> roleMappings = realm.getScopeMappings(user);

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
        Collection<RoleEntity> entities = application.getDefaultRoles();
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
        Collection<RoleEntity> entities = application.getDefaultRoles();
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
        Collection<RoleEntity> entities = application.getDefaultRoles();
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
        realm.addScopeMapping(getApplicationUser(), role);
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof ApplicationAdapter)) return false;
        ApplicationAdapter app = (ApplicationAdapter)o;
        return app.getId().equals(getId());
    }
}
