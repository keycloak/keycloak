package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
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
public class ApplicationAdapter implements ApplicationModel {

    protected EntityManager em;
    protected ApplicationEntity application;

    public ApplicationAdapter(EntityManager em, ApplicationEntity application) {
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
        Collection<RoleEntity> roles = application.getRoles();
        if (roles == null) return null;
        for (RoleEntity role : roles) {
            if (role.getName().equals(name)) {
                return new RoleAdapter(role);
            }
        }
        return null;
    }

    @Override
    public RoleModel addRole(String name) {
        RoleModel role = getRole(name);
        if (role != null) return role;
        RoleEntity entity = new RoleEntity();
        entity.setName(name);
        em.persist(entity);
        application.getRoles().add(entity);
        em.flush();
        return new RoleAdapter(entity);
    }

    @Override
    public boolean removeRoleById(String id) {
        RoleEntity role = em.find(RoleEntity.class, id);
        if (role == null) {
            return false;
        }

        application.getRoles().remove(role);
        application.getDefaultRoles().remove(role);

        em.createQuery("delete from " + ApplicationScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        em.createQuery("delete from " + ApplicationUserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        em.createQuery("delete from " + RealmScopeMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();
        em.createQuery("delete from " + RealmUserRoleMappingEntity.class.getSimpleName() + " where role = :role").setParameter("role", role).executeUpdate();

        em.remove(role);

        return true;
    }

    @Override
    public List<RoleModel> getRoles() {
        ArrayList<RoleModel> list = new ArrayList<RoleModel>();
        Collection<RoleEntity> roles = application.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(entity));
        }
        return list;
    }

    @Override
    public RoleModel getRoleById(String id) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        return new RoleAdapter(entity);
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        TypedQuery<ApplicationUserRoleMappingEntity> query = getApplicationUserRoleMappingEntityTypedQuery((UserAdapter) user, (RoleAdapter) role);
        return query.getResultList().size() > 0;
    }

    protected TypedQuery<ApplicationUserRoleMappingEntity> getApplicationUserRoleMappingEntityTypedQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<ApplicationUserRoleMappingEntity> query = em.createNamedQuery("userHasApplicationRole", ApplicationUserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("role", ((RoleAdapter)role).getRole());
        query.setParameter("application", application);
        return query;
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        if (hasRole(user, role)) return;
        ApplicationUserRoleMappingEntity entity = new ApplicationUserRoleMappingEntity();
        entity.setApplication(application);
        entity.setUser(((UserAdapter) user).getUser());
        entity.setRole(((RoleAdapter)role).getRole());
        em.persist(entity);
        em.flush();
    }

    @Override
    public List<RoleModel> getRoleMappings(UserModel user) {
        TypedQuery<ApplicationUserRoleMappingEntity> query = em.createNamedQuery("userApplicationMappings", ApplicationUserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("application", application);
        List<ApplicationUserRoleMappingEntity> entities = query.getResultList();
        List<RoleModel> roles = new ArrayList<RoleModel>();
        for (ApplicationUserRoleMappingEntity entity : entities) {
            roles.add(new RoleAdapter(entity.getRole()));
        }
        return roles;
    }

    @Override
    public Set<String> getRoleMappingValues(UserModel user) {
        TypedQuery<ApplicationUserRoleMappingEntity> query = em.createNamedQuery("userApplicationMappings", ApplicationUserRoleMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("application", application);
        List<ApplicationUserRoleMappingEntity> entities = query.getResultList();
        Set<String> roles = new HashSet<String>();
        for (ApplicationUserRoleMappingEntity entity : entities) {
            roles.add(entity.getRole().getName());
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        TypedQuery<ApplicationUserRoleMappingEntity> query = getApplicationUserRoleMappingEntityTypedQuery((UserAdapter) user, (RoleAdapter) role);
        List<ApplicationUserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (ApplicationUserRoleMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    @Override
    public boolean hasRole(UserModel user, String roleName) {
        RoleModel role = getRole(roleName);
        if (role == null) return false;
        return hasRole(user, role);
    }

    @Override
    public void addScopeMapping(UserModel agent, String roleName) {
        RoleModel role = getRole(roleName);
        if (role == null) throw new RuntimeException("role does not exist");
        addScopeMapping(agent, role);
    }

    @Override
    public Set<String> getScopeMappingValues(UserModel agent) {
        TypedQuery<ApplicationScopeMappingEntity> query = em.createNamedQuery("userApplicationScopeMappings", ApplicationScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)agent).getUser());
        query.setParameter("application", application);
        List<ApplicationScopeMappingEntity> entities = query.getResultList();
        Set<String> roles = new HashSet<String>();
        for (ApplicationScopeMappingEntity entity : entities) {
            roles.add(entity.getRole().getName());
        }
        return roles;
    }

    @Override
    public List<RoleModel> getScopeMappings(UserModel agent) {
        TypedQuery<ApplicationScopeMappingEntity> query = em.createNamedQuery("userApplicationScopeMappings", ApplicationScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)agent).getUser());
        query.setParameter("application", application);
        List<ApplicationScopeMappingEntity> entities = query.getResultList();
        List<RoleModel> roles = new ArrayList<RoleModel>();
        for (ApplicationScopeMappingEntity entity : entities) {
            roles.add(new RoleAdapter(entity.getRole()));
        }
        return roles;
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        if (hasScope(agent, role)) return;
        ApplicationScopeMappingEntity entity = new ApplicationScopeMappingEntity();
        entity.setApplication(application);
        entity.setUser(((UserAdapter) agent).getUser());
        entity.setRole(((RoleAdapter)role).getRole());
        em.persist(entity);
        em.flush();
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        TypedQuery<ApplicationScopeMappingEntity> query = getApplicationScopeMappingQuery((UserAdapter) user, (RoleAdapter) role);
        List<ApplicationScopeMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (ApplicationScopeMappingEntity entity : results) {
            em.remove(entity);
        }
    }

    public boolean hasScope(UserModel user, RoleModel role) {
        TypedQuery<ApplicationScopeMappingEntity> query = getApplicationScopeMappingQuery((UserAdapter) user, (RoleAdapter) role);
        return query.getResultList().size() > 0;
    }


    protected TypedQuery<ApplicationScopeMappingEntity> getApplicationScopeMappingQuery(UserAdapter user, RoleAdapter role) {
        TypedQuery<ApplicationScopeMappingEntity> query = em.createNamedQuery("userHasApplicationScope", ApplicationScopeMappingEntity.class);
        query.setParameter("user", ((UserAdapter)user).getUser());
        query.setParameter("role", ((RoleAdapter)role).getRole());
        query.setParameter("application", application);
        return query;
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
}
