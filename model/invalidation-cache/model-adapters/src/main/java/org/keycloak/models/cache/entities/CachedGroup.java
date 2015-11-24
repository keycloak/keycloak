package org.keycloak.models.cache.entities;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedGroup implements Serializable {
    private String id;
    private String realm;
    private String name;
    private String parentId;
    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private Set<String> roleMappings = new HashSet<>();
    private Set<String> subGroups = new HashSet<>();

    public CachedGroup(RealmModel realm, GroupModel group) {
        this.id = group.getId();
        this.realm = realm.getId();
        this.name = group.getName();
        this.parentId = group.getParentId();

        this.attributes.putAll(group.getAttributes());
        for (RoleModel role : group.getRoleMappings()) {
            roleMappings.add(role.getId());
        }
        Set<GroupModel> subGroups1 = group.getSubGroups();
        if (subGroups1 != null) {
            for (GroupModel subGroup : subGroups1) {
                subGroups.add(subGroup.getId());
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getRealm() {
        return realm;
    }

    public MultivaluedHashMap<String, String> getAttributes() {
        return attributes;
    }

    public Set<String> getRoleMappings() {
        return roleMappings;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public Set<String> getSubGroups() {
        return subGroups;
    }
}
