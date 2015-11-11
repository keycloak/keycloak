package org.keycloak.models.jpa;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserConsentEntity;
import org.keycloak.models.jpa.entities.UserConsentProtocolMapperEntity;
import org.keycloak.models.jpa.entities.UserConsentRoleEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserRequiredActionEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.utils.Pbkdf2PasswordEncoder.getSalt;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupAdapter implements GroupModel {

    protected GroupEntity group;
    protected EntityManager em;
    protected RealmModel realm;

    public GroupAdapter(RealmModel realm, EntityManager em, GroupEntity group) {
        this.em = em;
        this.group = group;
        this.realm = realm;
    }

    public GroupEntity getGroup() {
        return group;
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
    public GroupModel getParent() {
        GroupEntity parent = group.getParent();
        if (parent == null) return null;
        return realm.getGroupById(parent.getId());
    }

    @Override
    public String getParentId() {
        GroupEntity parent = group.getParent();
        if (parent == null) return null;
        return parent.getId();
    }

    public static GroupEntity toEntity(GroupModel model, EntityManager em) {
        if (model instanceof GroupAdapter) {
            return ((GroupAdapter)model).getGroup();
        }
        return em.getReference(GroupEntity.class, model.getId());
    }

    @Override
    public void setParent(GroupModel parent) {
        if (parent == null) group.setParent(null);
        else {
            GroupEntity parentEntity = toEntity(parent, em);
            group.setParent(parentEntity);
        }
    }

    @Override
    public void addChild(GroupModel subGroup) {
        subGroup.setParent(this);
    }

    @Override
    public void removeChild(GroupModel subGroup) {
        subGroup.setParent(null);
    }

    @Override
    public Set<GroupModel> getSubGroups() {
        TypedQuery<String> query = em.createNamedQuery("getGroupIdsByParent", String.class);
        query.setParameter("parent", group);
        List<String> ids = query.getResultList();
        Set<GroupModel> set = new HashSet<>();
        for (String id : ids) {
            GroupModel subGroup = realm.getGroupById(id);
            if (subGroup == null) continue;
            set.add(subGroup);
        }
        return set;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        boolean found = false;
        List<GroupAttributeEntity> toRemove = new ArrayList<>();
        for (GroupAttributeEntity attr : group.getAttributes()) {
            if (attr.getName().equals(name)) {
                if (!found) {
                    attr.setValue(value);
                    found = true;
                } else {
                    toRemove.add(attr);
                }
            }
        }

        for (GroupAttributeEntity attr : toRemove) {
            em.remove(attr);
            group.getAttributes().remove(attr);
        }

        if (found) {
            return;
        }

        persistAttributeValue(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // Remove all existing
        removeAttribute(name);

        // Put all new
        for (String value : values) {
            persistAttributeValue(name, value);
        }
    }

    private void persistAttributeValue(String name, String value) {
        GroupAttributeEntity attr = new GroupAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setGroup(group);
        em.persist(attr);
        group.getAttributes().add(attr);
    }

    @Override
    public void removeAttribute(String name) {
        Iterator<GroupAttributeEntity> it = group.getAttributes().iterator();
        while (it.hasNext()) {
            GroupAttributeEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        for (GroupAttributeEntity attr : group.getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
    }

    @Override
    public List<String> getAttribute(String name) {
        List<String> result = new ArrayList<>();
        for (GroupAttributeEntity attr : group.getAttributes()) {
            if (attr.getName().equals(name)) {
                result.add(attr.getValue());
            }
        }
        return result;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        for (GroupAttributeEntity attr : group.getAttributes()) {
            result.add(attr.getName(), attr.getValue());
        }
        return result;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return KeycloakModelUtils.hasRole(roles, role);
    }

    protected TypedQuery<GroupRoleMappingEntity> getGroupRoleMappingEntityTypedQuery(RoleModel role) {
        TypedQuery<GroupRoleMappingEntity> query = em.createNamedQuery("groupHasRole", GroupRoleMappingEntity.class);
        query.setParameter("group", getGroup());
        query.setParameter("roleId", role.getId());
        return query;
    }

    @Override
    public void grantRole(RoleModel role) {
        if (hasRole(role)) return;
        GroupRoleMappingEntity entity = new GroupRoleMappingEntity();
        entity.setGroup(getGroup());
        entity.setRoleId(role.getId());
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }


    @Override
    public Set<RoleModel> getRoleMappings() {
        // we query ids only as the role might be cached and following the @ManyToOne will result in a load
        // even if we're getting just the id.
        TypedQuery<String> query = em.createNamedQuery("groupRoleMappingIds", String.class);
        query.setParameter("group", getGroup());
        List<String> ids = query.getResultList();
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (String roleId : ids) {
            RoleModel roleById = realm.getRoleById(roleId);
            if (roleById == null) continue;
            roles.add(roleById);
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (group == null || role == null) return;

        TypedQuery<GroupRoleMappingEntity> query = getGroupRoleMappingEntityTypedQuery(role);
        List<GroupRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (GroupRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                ClientModel appModel = (ClientModel)container;
                if (appModel.getId().equals(app.getId())) {
                   roles.add(role);
                }
            }
        }
        return roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserModel)) return false;

        UserModel that = (UserModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }



}
