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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.fgap.evaluation.partial.PartialEvaluationStorageProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.UserStoragePrivateUtil;

import static java.util.Optional.ofNullable;

import static org.keycloak.common.util.CollectionUtil.collectionEquals;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupAdapter implements GroupModel , JpaModel<GroupEntity> {

    protected final KeycloakSession session;
    protected GroupEntity group;
    protected EntityManager em;
    protected RealmModel realm;

    public GroupAdapter(KeycloakSession session, RealmModel realm, EntityManager em, GroupEntity group) {
        this.session = session;
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
        fireGroupUpdatedEvent();
    }

    @Override
    public String getDescription() {
        return group.getDescription();
    }

    @Override
    public void setDescription(String description) {
        group.setDescription(description);
        fireGroupUpdatedEvent();
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
        fireGroupUpdatedEvent();
    }

    @Override
    public void addChild(GroupModel subGroup) {
        if (subGroup.getId().equals(getId())) {
            return;
        }
        subGroup.setParent(this);
        fireGroupUpdatedEvent();
    }

    @Override
    public void removeChild(GroupModel subGroup) {
        if (subGroup.getId().equals(getId())) {
            return;
        }
        subGroup.setParent(null);
        fireGroupUpdatedEvent();
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream() {
        return getSubGroupsStream("", false, -1, -1);
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream(String search, Boolean exact, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.add(builder.equal(root.get("parentId"), group.getId()));

        search = search == null ? "" : search;

        if (Boolean.TRUE.equals(exact)) {
            predicates.add(builder.like(root.get("name"), search));
        } else {
            predicates.add(builder.like(builder.lower(root.get("name")), builder.lower(builder.literal("%" + search + "%"))));
        }

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, (PartialEvaluationStorageProvider) UserStoragePrivateUtil.userLocalStorage(session), realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return closing(paginateQuery(em.createQuery(queryBuilder), firstResult, maxResults).getResultStream()
                .map(realm::getGroupById)
                // In concurrent tests, the group might be deleted in another thread, therefore, skip those null values.
                .filter(Objects::nonNull)
        );
    }

    @Override
    public Long getSubGroupsCount() {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(builder.count(root.get("id")));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.add(builder.equal(root.get("parentId"), group.getId()));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, (PartialEvaluationStorageProvider) UserStoragePrivateUtil.userLocalStorage(session), realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(queryBuilder).getSingleResult();
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
            fireGroupUpdatedEvent();
            return;
        }

        persistAttributeValue(name, value);
        fireGroupUpdatedEvent();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        List<String> current = getAttributes().getOrDefault(name, List.of());

        if (collectionEquals(current, ofNullable(values).orElse(List.of()))) {
            return;
        }

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
        fireGroupUpdatedEvent();
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
        if (RoleUtils.hasRole(getRoleMappingsStream(), role)) return true;
        GroupModel parent = getParent();
        return parent != null && parent.hasRole(role);
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
        fireGroupUpdatedEvent();
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
        fireGroupUpdatedEvent();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
    }

    @Override
    public Type getType() {
        return Type.valueOf(group.getType());
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
    public boolean escapeSlashesInGroupPath() {
        return KeycloakModelUtils.escapeSlashesInGroupPath(session);
    }

    private void fireGroupUpdatedEvent() {
        GroupUpdatedEvent.fire(this, session);
    }
}
