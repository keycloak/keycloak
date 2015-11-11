package org.keycloak.models.cache.infinispan;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupAdapter implements GroupModel {
    protected GroupModel updated;
    protected CachedGroup cached;
    protected CacheRealmProvider cacheSession;
    protected KeycloakSession keycloakSession;
    protected RealmModel realm;

    public GroupAdapter(CachedGroup cached, CacheRealmProvider cacheSession, KeycloakSession keycloakSession, RealmModel realm) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.keycloakSession = keycloakSession;
        this.realm = realm;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerGroupInvalidation(getId());
            updated = cacheSession.getDelegate().getGroupById(getId(), realm);
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof GroupModel)) return false;

        GroupModel that = (GroupModel) o;

        if (!cached.getId().equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return cached.getId().hashCode();
    }

    @Override
    public String getId() {
        if (updated != null) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (updated != null) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);

    }

    @Override
    public void setSingleAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getDelegateForUpdate();
        updated.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);

    }

    @Override
    public String getFirstAttribute(String name) {
        if (updated != null) return updated.getFirstAttribute(name);
        return cached.getAttributes().getFirst(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        List<String> values = cached.getAttributes().get(name);
        if (values == null) return null;
        return values;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return cached.getAttributes();
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        if (updated != null) return updated.getRealmRoleMappings();
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> realmMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(realm.getId())) {
                    realmMappings.add(role);
                }
            }
        }
        return realmMappings;
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        if (updated != null) return updated.getClientRoleMappings(app);
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> appMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                if (((ClientModel) container).getId().equals(app.getId())) {
                    appMappings.add(role);
                }
            }
        }
        return appMappings;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (updated != null) return updated.hasRole(role);
        if (cached.getRoleMappings().contains(role.getId())) return true;

        Set<RoleModel> mappings = getRoleMappings();
        for (RoleModel mapping: mappings) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        getDelegateForUpdate();
        updated.grantRole(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (updated != null) return updated.getRoleMappings();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String id : cached.getRoleMappings()) {
            RoleModel roleById = keycloakSession.realms().getRoleById(id, realm);
            if (roleById == null) {
                // chance that role was removed, so just delegate to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getRoleMappings();
            }
            roles.add(roleById);

        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.deleteRoleMapping(role);
    }

    @Override
    public GroupModel getParent() {
        if (updated != null) return updated.getParent();
        if (cached.getParentId() == null) return null;
        return keycloakSession.realms().getGroupById(cached.getParentId(), realm);
    }

    @Override
    public String getParentId() {
        if (updated != null) return updated.getParentId();
        return cached.getParentId();
    }

    @Override
    public Set<GroupModel> getSubGroups() {
        if (updated != null) return updated.getSubGroups();
        Set<GroupModel> subGroups = new HashSet<>();
        for (String id : cached.getSubGroups()) {
            GroupModel subGroup = keycloakSession.realms().getGroupById(id, realm);
            if (subGroup == null) {
                // chance that role was removed, so just delegate to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getSubGroups();

            }
            subGroups.add(subGroup);
        }
        return subGroups;
    }



    @Override
    public void setParent(GroupModel group) {
        getDelegateForUpdate();
        updated.setParent(group);

    }

    @Override
    public void addChild(GroupModel subGroup) {
        getDelegateForUpdate();
        updated.addChild(subGroup);

    }

    @Override
    public void removeChild(GroupModel subGroup) {
        getDelegateForUpdate();
        updated.removeChild(subGroup);
    }
}
