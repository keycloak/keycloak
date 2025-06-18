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

package org.keycloak.models.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * JPA entity for organization roles.
 */
@Entity
@Table(name = "ORG_ROLE")
@NamedQueries({
    @NamedQuery(name = "organizationRolesByOrganization", query = "SELECT r FROM OrganizationRoleEntity r WHERE r.organization.id = :organizationId"),
    @NamedQuery(name = "organizationRoleByName", query = "SELECT r FROM OrganizationRoleEntity r WHERE r.organization.id = :organizationId AND r.name = :name"),
    @NamedQuery(name = "organizationRoleById", query = "SELECT r FROM OrganizationRoleEntity r WHERE r.id = :id"),
    @NamedQuery(name = "organizationRoleByNameContaining", query = "SELECT r FROM OrganizationRoleEntity r WHERE r.organization.id = :organizationId AND LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))"),
    @NamedQuery(name = "organizationRoleCount", query = "SELECT COUNT(r) FROM OrganizationRoleEntity r WHERE r.organization.id = :organizationId"),
    @NamedQuery(name = "deleteOrganizationRolesByOrganization", query = "DELETE FROM OrganizationRoleEntity r WHERE r.organization.id = :organizationId")
})
public class OrganizationRoleEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORG_ID")
    private OrganizationEntity organization;

    @Column(name = "ORG_ID", insertable = false, updatable = false)
    private String organizationId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationRoleEntity)) return false;

        OrganizationRoleEntity that = (OrganizationRoleEntity) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
