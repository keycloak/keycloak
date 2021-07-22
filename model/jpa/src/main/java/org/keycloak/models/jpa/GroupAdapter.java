/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.jpa;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.persistence.LockModeType;

import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupAdapter implements GroupModel.Streams , JpaModel<GroupEntity> {

    protected GroupEntity group;
    protected EntityManager em;
    protected RealmModel realm;

    public GroupAdapter(RealmModel realm, EntityManager em, GroupEntity group) {
        this.em = em;
        this.group = group;
        this.realm = realm;
    }

    public GroupEntity getEntity() {
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
        String parentId = this.getParentId();
        return parentId == null? null : realm.getGroupById(parentId);
    }

    @Override
    public String getParentId() {
        return GroupEntity.TOP_PARENT_ID.equals(group.getParentId())? null : group.getParentId();
    }

    public static GroupEntity toEntity(GroupModel model, EntityManager em) {
        if (model instanceof GroupAdapter) {
            return ((GroupAdapter)model).getEntity();
        }
        return em.getReference(GroupEntity.class, model.getId());
    }

    @Override
    public void setParent(GroupModel parent) {
        if (parent == null) {
            group.setParentId(GroupEntity.TOP_PARENT_ID);
        } else if (!parent.getId().equals(getId())) {
            GroupEntity parentEntity = toEntity(parent, em);
            group.setParentId(parentEntity.getId());
        }
    }

    @Override
    public void addChild(GroupModel subGroup) {
        if (subGroup.getId().equals(getId())) {
            return;
        }
        subGroup.setParent(this);
    }

    @Override
    public void removeChild(GroupModel subGroup) {
        if (subGroup.getId().equals(getId())) {
            return;
        }
        subGroup.setParent(null);
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream() {
        TypedQuery<String> query = em.createNamedQuery("getGroupIdsByParent", String.class);
        query.setParameter("parent", group.getId());
        return closing(query.getResultStream().map(realm::getGroupById).filter(Objects::nonNull));
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
    public Stream<String> getAttributeStream(String name) {
        return group.getAttributes().stream()
                .filter(attr -> Objects.equals(attr.getName(), name))
                .map(GroupAttributeEntity::getValue);
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
        return RoleUtils.hasRole(getRoleMappingsStream(), role);
    }

    protected TypedQuery<GroupRoleMappingEntity> getGroupRoleMappingEntityTypedQuery(RoleModel role) {
        TypedQuery<GroupRoleMappingEntity> query = em.createNamedQuery("groupHasRole", GroupRoleMappingEntity.class);
        query.setParameter("group", getEntity());
        query.setParameter("roleId", role.getId());
        return query;
    }

    @Override
    public void grantRole(RoleModel role) {
        if (hasDirectRole(role)) return;
        GroupRoleMappingEntity entity = new GroupRoleMappingEntity();
        entity.setGroup(getEntity());
        entity.setRoleId(role.getId());
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return getRoleMappingsStream().filter(RoleUtils::isRealmRole);
    }


    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        // we query ids only as the role might be cached and following the @ManyToOne will result in a load
        // even if we're getting just the id.
        TypedQuery<String> query = em.createNamedQuery("groupRoleMappingIds", String.class);
        query.setParameter("group", getEntity());
        return closing(query.getResultStream().map(realm::getRoleById).filter(Objects::nonNull));
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (group == null || role == null) return;

        TypedQuery<GroupRoleMappingEntity> query = getGroupRoleMappingEntityTypedQuery(role);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<GroupRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (GroupRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
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



}
