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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.RoleAttributeEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

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
        return getCompositesStream().count() > 0;
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        RoleEntity entity = toRoleEntity(role);
        for (RoleEntity composite : getEntity().getCompositeRoles()) {
            if (composite.equals(entity)) return;
        }
        getEntity().getCompositeRoles().add(entity);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        RoleEntity entity = toRoleEntity(role);
        getEntity().getCompositeRoles().remove(entity);
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
