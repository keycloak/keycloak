/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.jpa.entities.OrganizationRoleAttributeEntity;
import org.keycloak.models.jpa.entities.OrganizationRoleCompositeEntity;
import org.keycloak.models.jpa.entities.OrganizationRoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JPA adapter for OrganizationRoleModel.
 */
public class OrganizationRoleAdapter implements OrganizationRoleModel, JpaModel<OrganizationRoleEntity> {

    private final RealmModel realm;
    private final OrganizationModel organization;
    private final OrganizationRoleEntity entity;
    private final EntityManager em;

    public OrganizationRoleAdapter(KeycloakSession session, RealmModel realm, OrganizationModel organization, OrganizationRoleEntity entity) {
        this.realm = realm;
        this.organization = organization;
        this.entity = entity;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public OrganizationRoleEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
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
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public OrganizationModel getOrganization() {
        return organization;
    }

    @Override
    public boolean isComposite() {
        return em.createNamedQuery("organizationRoleCompositeExists", Long.class)
                .setParameter("composite", getId())
                .getSingleResult() > 0;
    }

    @Override
    public void addCompositeRole(RoleModel role) {
        if (hasRole(role)) {
            return;
        }

        OrganizationRoleCompositeEntity composite = new OrganizationRoleCompositeEntity();
        composite.setComposite(getId());
        composite.setChildRole(role.getId());
        
        if (role.isClientRole()) {
            composite.setChildType("CLIENT");
        } else {
            composite.setChildType("REALM");
        }
        
        em.persist(composite);
    }

    @Override
    public void addCompositeRole(OrganizationRoleModel role) {
        if (hasRole(role)) {
            return;
        }

        OrganizationRoleCompositeEntity composite = new OrganizationRoleCompositeEntity();
        composite.setComposite(getId());
        composite.setChildRole(role.getId());
        composite.setChildType("ORGANIZATION");
        em.persist(composite);
    }

    @Override
    public void removeCompositeRole(RoleModel role) {
        try {
            OrganizationRoleCompositeEntity composite = em.createNamedQuery("organizationRoleComposite", OrganizationRoleCompositeEntity.class)
                    .setParameter("composite", getId())
                    .setParameter("childRole", role.getId())
                    .getSingleResult();
            em.remove(composite);
        } catch (NoResultException e) {
            // Composite doesn't exist, nothing to remove
        }
    }

    @Override
    public void removeCompositeRole(OrganizationRoleModel role) {
        try {
            OrganizationRoleCompositeEntity composite = em.createNamedQuery("organizationRoleComposite", OrganizationRoleCompositeEntity.class)
                    .setParameter("composite", getId())
                    .setParameter("childRole", role.getId())
                    .getSingleResult();
            em.remove(composite);
        } catch (NoResultException e) {
            // Composite doesn't exist, nothing to remove
        }
    }

    @Override
    public Stream<RoleModel> getCompositesStream() {
        return em.createNamedQuery("organizationRoleComposites", OrganizationRoleCompositeEntity.class)
                .setParameter("composite", getId())
                .getResultList()
                .stream()
                .filter(composite -> !"ORGANIZATION".equals(composite.getChildType()))
                .map(composite -> realm.getRoleById(composite.getChildRole()))
                .filter(role -> role != null);
    }

    @Override
    public Stream<OrganizationRoleModel> getCompositeOrganizationRolesStream() {
        return em.createNamedQuery("organizationRoleComposites", OrganizationRoleCompositeEntity.class)
                .setParameter("composite", getId())
                .getResultList()
                .stream()
                .filter(composite -> "ORGANIZATION".equals(composite.getChildType()))
                .map(composite -> organization.getRoleById(composite.getChildRole()))
                .filter(role -> role != null);
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) return true;
        
        try {
            em.createNamedQuery("organizationRoleComposite", OrganizationRoleCompositeEntity.class)
                    .setParameter("composite", getId())
                    .setParameter("childRole", role.getId())
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public boolean hasRole(OrganizationRoleModel role) {
        if (this.equals(role)) return true;
        
        try {
            em.createNamedQuery("organizationRoleComposite", OrganizationRoleCompositeEntity.class)
                    .setParameter("composite", getId())
                    .setParameter("childRole", role.getId())
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        setAttribute(name, List.of(value));
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // Remove existing attributes with this name
        em.createNamedQuery("deleteOrganizationRoleAttributesByNameAndRole")
                .setParameter("name", name)
                .setParameter("organizationRoleId", getId())
                .executeUpdate();

        // Add new attributes
        for (String value : values) {
            OrganizationRoleAttributeEntity attr = new OrganizationRoleAttributeEntity();
            attr.setId(KeycloakModelUtils.generateId());
            attr.setOrganizationRole(entity);
            attr.setName(name);
            attr.setValue(value);
            em.persist(attr);
        }
    }

    @Override
    public void removeAttribute(String name) {
        em.createNamedQuery("deleteOrganizationRoleAttributesByNameAndRole")
                .setParameter("name", name)
                .setParameter("organizationRoleId", getId())
                .executeUpdate();
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return em.createNamedQuery("organizationRoleAttributesByNameAndRole", OrganizationRoleAttributeEntity.class)
                .setParameter("name", name)
                .setParameter("organizationRoleId", getId())
                .getResultList()
                .stream()
                .map(OrganizationRoleAttributeEntity::getValue);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        List<OrganizationRoleAttributeEntity> attrs = em.createNamedQuery("organizationRoleAttributes", OrganizationRoleAttributeEntity.class)
                .setParameter("organizationRoleId", getId())
                .getResultList();

        Map<String, List<String>> result = new HashMap<>();
        for (OrganizationRoleAttributeEntity attr : attrs) {
            result.computeIfAbsent(attr.getName(), k -> List.of()).add(attr.getValue());
        }
        
        return result.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationRoleModel)) return false;

        OrganizationRoleModel that = (OrganizationRoleModel) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    @Override
    public String toString() {
        return getName();
    }
}
