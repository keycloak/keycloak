package org.keycloak.models.file.adapter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.GroupEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GroupAdapter implements GroupModel {

    private final GroupEntity group;
    private RealmModel realm;
    private KeycloakSession session;

    public GroupAdapter(KeycloakSession session, RealmModel realm, GroupEntity group) {
        this.group = group;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public String getId() {
        return group.getId();
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public void setName(String name) {
        group.setName(name);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof GroupModel)) return false;

        GroupModel that = (GroupModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (group.getAttributes() == null) {
            group.setAttributes(new HashMap<String, List<String>>());
        }

        List<String> attrValues = new ArrayList<>();
        attrValues.add(value);
        group.getAttributes().put(name, attrValues);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (group.getAttributes() == null) {
            group.setAttributes(new HashMap<String, List<String>>());
        }

        group.getAttributes().put(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        if (group.getAttributes() == null) return;

        group.getAttributes().remove(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (group.getAttributes()==null) return null;

        List<String> attrValues = group.getAttributes().get(name);
        return (attrValues==null || attrValues.isEmpty()) ? null : attrValues.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (group.getAttributes()==null) return Collections.<String>emptyList();
        List<String> attrValues = group.getAttributes().get(name);
        return (attrValues == null) ? Collections.<String>emptyList() : Collections.unmodifiableList(attrValues);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return group.getAttributes()==null ? Collections.<String, List<String>>emptyMap() : Collections.unmodifiableMap((Map) group.getAttributes());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return KeycloakModelUtils.hasRole(roles, role);
    }

    @Override
    public void grantRole(RoleModel role) {
        if (group.getRoleIds() == null) {
            group.setRoleIds(new LinkedList<String>());
        }
        if (group.getRoleIds().contains(role.getId())) {
            return;
        }
        group.getRoleIds().add(role.getId());
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (group.getRoleIds() == null || group.getRoleIds().isEmpty()) return Collections.EMPTY_SET;
        Set<RoleModel> roles = new HashSet<>();
        for (String id : group.getRoleIds()) {
            roles.add(realm.getRoleById(id));
        }
        return roles;
     }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> allRoles = getRoleMappings();

        // Filter to retrieve just realm roles
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allRoles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (group == null || role == null) return;
        if (group.getRoleIds() == null) return;
        group.getRoleIds().remove(role.getId());
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        Set<RoleModel> roles = getRoleMappings();

        for (RoleModel role : roles) {
            if (app.equals(role.getContainer())) {
                result.add(role);
            }
        }
        return result;
    }

    @Override
    public GroupModel getParent() {
        if (group.getParentId() == null) return null;
        return realm.getGroupById(group.getParentId());
    }

    @Override
    public String getParentId() {
        return group.getParentId();
    }

    @Override
    public Set<GroupModel> getSubGroups() {
        Set<GroupModel> subGroups = new HashSet<>();
        for (GroupModel groupModel : realm.getGroups()) {
            if (groupModel.getParent().equals(this)) {
                subGroups.add(groupModel);
            }
        }
        return subGroups;
    }

    @Override
    public void setParent(GroupModel group) {
        this.group.setParentId(group.getId());

    }

    @Override
    public void addChild(GroupModel subGroup) {
        subGroup.setParent(this);

    }

    @Override
    public void removeChild(GroupModel subGroup) {
        subGroup.setParent(null);

    }
}
