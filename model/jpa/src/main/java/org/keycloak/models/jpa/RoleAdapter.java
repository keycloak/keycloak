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

import org.hibernate.Hibernate;
import org.hibernate.SynchronizeableQuery;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.CompositeRoleEntity;
import org.keycloak.models.jpa.entities.CompositeRoleEntityKey;
import org.keycloak.models.jpa.entities.RoleAttributeEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAdapter implements RoleModel, JpaModel<RoleEntity> {
            
    protected RoleEntity role;
    protected EntityManager em;
    protected RealmModel realm;
    protected KeycloakSession session;

    public RoleAdapter(KeycloakSession session, RealmModel realm, EntityManager em, RoleEntity role) {
        this.em = em;
        this.realm = realm;
        this.role = role;
        this.session = session;
    }

    @Override
    public RoleEntity getEntity() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
    }

    @Override
    public boolean isComposite() {
        // Use the composite role collection if already loaded, or use a named query to avoid triggering its lazy loading
        if (Hibernate.isPropertyInitialized(getEntity(), "compositeRoles")) {
            return !getEntity().getCompositeRoles().isEmpty();
        }
        else {
            TypedQuery<String> query = em.createNamedQuery("getChildrenRoleIds", String.class);
            query.setParameter("roleId", getId());
            query.setMaxResults(1);
            return !query.getResultList().isEmpty();
        }
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        // Avoid lazy loading the composite role collection if not already done
        // Not using Persistence.getPersistenceUtil().isLoaded(Object, String) as this is not working
        // properly (Hibernate 5.6) - still returning false even after the collection was lazy loaded
        if (Hibernate.isPropertyInitialized(getEntity(), "compositeRoles")) {
            addCompositeRoleUsingLoadedCompositeCollection(role);
        }
        else {
            addCompositeRoleWithoutLoadingCompositeCollection(role);
        }
    }

    private void addCompositeRoleUsingLoadedCompositeCollection(RoleModel role) {
        RoleEntity entity = toRoleEntity(role);
        // Why performing this loop at all? The semantic of Set.add(T) ensures that the operation
        // is performed only if there is not entry already...
        for (RoleEntity composite : getEntity().getCompositeRoles()) {
            if (composite.equals(entity)) return;
        }
        getEntity().getCompositeRoles().add(entity);
    }

    private void addCompositeRoleWithoutLoadingCompositeCollection(RoleModel role) {
        // Ensure that the entry does not exist already - will hit the database,
        // but it's required to maintain the semantic of method #addCompositeRole(RoleModel)
        CompositeRoleEntityKey compositeKey = new CompositeRoleEntityKey(this.getId(), role.getId());
        if (em.find(CompositeRoleEntity.class, compositeKey) == null) {
            // Using a native query to perform insertion (not possible with JPQL), allowing to instruct
            // the auto-flushing mechanism which query spaces (tables) should be flushed
            // (and invalidated in 2nd level cache) before executing it.
            String compositeRoleTable = JpaUtils.getTableNameForNativeQuery("COMPOSITE_ROLE", em);
            Query q = em.createNativeQuery("insert into " + compositeRoleTable + " (COMPOSITE, CHILD_ROLE) values (:composite, :child)")
                    .setParameter("composite", this.getId())
                    .setParameter("child", role.getId())
                    ;
            // Table for the RoleEntity class is added to the query space so that
            // any pending operation in this table is flushed as well.
            SynchronizeableQuery<?> sq = q.unwrap(SynchronizeableQuery.class);
            sq.addSynchronizedEntityClass(CompositeRoleEntity.class, RoleEntity.class);
            q.executeUpdate();
        }
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        // Avoid lazy loading the composite role collection if not already done
        // Not using Persistence.getPersistenceUtil().isLoaded(Object, String) as this is not working
        // properly (Hibernate 5.6) - still returning false even after the collection was lazy loaded
        if (Hibernate.isPropertyInitialized(getEntity(), "compositeRoles")) {
            removeCompositeRoleUsingLoadedCompositeCollection(role);
        }
        else {
            removeCompositeRoleWithoutLoadingCompositeCollection(role);
        }
    }

    private void removeCompositeRoleUsingLoadedCompositeCollection(RoleModel role) {
        RoleEntity entity = toRoleEntity(role);
        getEntity().getCompositeRoles().remove(entity);
    }

    private void removeCompositeRoleWithoutLoadingCompositeCollection(RoleModel role) {
        // Using a named query here to avoid the two-steps find() + delete() operation when using
        // EntityManager.
        em.createNamedQuery("removeCompositeAndChildRoleEntry")
            .setParameter("compositeId", this.getId())
            .setParameter("childId", role.getId())
            .executeUpdate();
    }
    
    @Override
    public Stream<RoleModel> getCompositesStream() {
        Stream<RoleModel> composites = getEntity().getCompositeRoles().stream().map(c -> new RoleAdapter(session, realm, em, c));
        return composites.filter(Objects::nonNull);
    }
    
    @Override
    public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
        return session.roles().getRolesStream(realm,
                getEntity().getCompositeRoles().stream().map(RoleEntity::getId),
                search, first, max);
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return this.equals(role) || KeycloakModelUtils.searchFor(role, this, new HashSet<>());
    }

    private void persistAttributeValue(String name, String value) {
        RoleAttributeEntity attr = new RoleAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setRole(role);
        em.persist(attr);
        role.getAttributes().add(attr);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        removeAttribute(name);

        for (String value : values) {
            persistAttributeValue(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        List<RoleAttributeEntity> attributes = role.getAttributes();

        Query query = em.createNamedQuery("deleteRoleAttributesByNameAndUser");
        query.setParameter("name", name);
        query.setParameter("roleId", role.getId());
        query.executeUpdate();

        attributes.removeIf(attribute -> attribute.getName().equals(name));
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return role.getAttributes().stream()
                .filter(a -> Objects.equals(a.getName(), name))
                .map(RoleAttributeEntity::getValue);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> map = new HashMap<>();
        for (RoleAttributeEntity attribute : role.getAttributes()) {
            map.computeIfAbsent(attribute.getName(), name -> new ArrayList<>()).add(attribute.getValue());
        }

        return map;
    }

    @Override
    public boolean isClientRole() {
        return role.isClientRole();
    }

    @Override
    public String getContainerId() {
        return isClientRole() ? role.getClientId() : role.getRealmId();
    }


    @Override
    public RoleContainerModel getContainer() {
        return isClientRole() ? realm.getClientById(role.getClientId()) : realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private RoleEntity toRoleEntity(RoleModel model) {
        if (model instanceof RoleAdapter) {
            return ((RoleAdapter) model).getEntity();
        }
        return em.getReference(RoleEntity.class, model.getId());
    }

}