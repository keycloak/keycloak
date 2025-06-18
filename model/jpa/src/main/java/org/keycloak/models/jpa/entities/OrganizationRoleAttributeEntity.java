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
 * JPA entity for organization role attributes.
 */
@Entity
@Table(name = "ORG_ROLE_ATTRIBUTE")
@NamedQueries({
    @NamedQuery(name = "organizationRoleAttributes", query = "SELECT a FROM OrganizationRoleAttributeEntity a WHERE a.organizationRole.id = :organizationRoleId"),
    @NamedQuery(name = "organizationRoleAttributesByNameAndRole", query = "SELECT a FROM OrganizationRoleAttributeEntity a WHERE a.organizationRole.id = :organizationRoleId AND a.name = :name"),
    @NamedQuery(name = "deleteOrganizationRoleAttributesByNameAndRole", query = "DELETE FROM OrganizationRoleAttributeEntity a WHERE a.organizationRole.id = :organizationRoleId AND a.name = :name"),
    @NamedQuery(name = "deleteOrganizationRoleAttributesByRole", query = "DELETE FROM OrganizationRoleAttributeEntity a WHERE a.organizationRole.id = :organizationRoleId")
})
public class OrganizationRoleAttributeEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORG_ROLE_ID")
    private OrganizationRoleEntity organizationRole;

    @Column(name = "NAME")
    private String name;

    @Column(name = "VALUE", length = 4000)
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrganizationRoleEntity getOrganizationRole() {
        return organizationRole;
    }

    public void setOrganizationRole(OrganizationRoleEntity organizationRole) {
        this.organizationRole = organizationRole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationRoleAttributeEntity)) return false;

        OrganizationRoleAttributeEntity that = (OrganizationRoleAttributeEntity) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
