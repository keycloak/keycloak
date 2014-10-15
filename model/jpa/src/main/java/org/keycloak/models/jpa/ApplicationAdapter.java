package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ApplicationEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.Time;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationAdapter extends ClientAdapter implements ApplicationModel {

    protected EntityManager em;
    protected KeycloakSession session;
    protected ApplicationEntity applicationEntity;

    public ApplicationAdapter(RealmModel realm, EntityManager em, KeycloakSession session, ApplicationEntity applicationEntity) {
        super(realm, applicationEntity, em);
        this.session = session;
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
    public RoleModel getRole(String name) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getAppRoleByName", RoleEntity.class);
        query.setParameter("name", name);
        query.setParameter("application", entity);
        List<RoleEntity> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(realm, em, roles.get(0));
    }

    @Override
    public RoleModel addRole(String name) {
        return this.addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setApplication(applicationEntity);
        roleEntity.setApplicationRole(true);
        roleEntity.setRealmId(realm.getId());
        em.persist(roleEntity);
        applicationEntity.getRoles().add(roleEntity);
        em.flush();
        return new RoleAdapter(realm, em, roleEntity);
    }

    @Override
    public boolean removeRole(RoleModel roleModel) {
        if (roleModel == null) {
            return false;
        }
        if (!roleModel.getContainer().equals(this)) return false;

        session.users().preRemove(getRealm(), roleModel);
        RoleEntity role = RoleAdapter.toRoleEntity(roleModel, em);
        if (!role.isApplicationRole()) return false;


        applicationEntity.getRoles().remove(role);
        applicationEntity.getDefaultRoles().remove(role);
        em.createNativeQuery("delete from COMPOSITE_ROLE where CHILD_ROLE = :role").setParameter("role", role).executeUpdate();
        em.createNamedQuery("deleteScopeMappingByRole").setParameter("role", role).executeUpdate();
        role.setApplication(null);
        em.flush();
        em.remove(role);
        em.flush();

        return true;
    }

    @Override
    public Set<RoleModel> getRoles() {
        Set<RoleModel> list = new HashSet<RoleModel>();
        Collection<RoleEntity> roles = applicationEntity.getRoles();
        if (roles == null) return list;
        for (RoleEntity entity : roles) {
            list.add(new RoleAdapter(realm, em, entity));
        }
        return list;
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (super.hasScope(role)) {
            return true;
        }
        Set<RoleModel> roles = getRoles();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(ClientModel client) {
        Set<RoleModel> roleMappings = client.getScopeMappings();

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
        RoleEntity roleEntity = RoleAdapter.toRoleEntity(role, em);
        entities.add(roleEntity);
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
    public int getNodeReRegistrationTimeout() {
        return applicationEntity.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        applicationEntity.setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return applicationEntity.getRegisteredNodes();
    }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        Map<String, Integer> currentNodes = getRegisteredNodes();
        currentNodes.put(nodeHost, registrationTime);
        em.flush();
    }

    @Override
    public void unregisterNode(String nodeHost) {
        Map<String, Integer> currentNodes = getRegisteredNodes();
        currentNodes.remove(nodeHost);
        em.flush();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ApplicationModel)) return false;

        ApplicationModel that = (ApplicationModel) o;
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
