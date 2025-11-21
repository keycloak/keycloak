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
 * JPA entity for user organization role mappings.
 */
@Entity
@Table(name = "USER_ORG_ROLE_MAPPING")
@NamedQueries({
    @NamedQuery(name = "userOrganizationRoleMappingsByUser", query = "SELECT m FROM UserOrganizationRoleMappingEntity m WHERE m.user.id = :userId"),
    @NamedQuery(name = "userOrganizationRoleMappingsByRole", query = "SELECT m FROM UserOrganizationRoleMappingEntity m WHERE m.organizationRole.id = :organizationRoleId"),
    @NamedQuery(name = "userOrganizationRoleMappingByUserAndRole", query = "SELECT m FROM UserOrganizationRoleMappingEntity m WHERE m.user.id = :userId AND m.organizationRole.id = :organizationRoleId"),
    @NamedQuery(name = "userOrganizationRoleMappingsByUserAndOrganization", query = "SELECT m FROM UserOrganizationRoleMappingEntity m WHERE m.user.id = :userId AND m.organizationRole.organization.id = :organizationId"),
    @NamedQuery(name = "deleteUserOrganizationRoleMappingsByUser", query = "DELETE FROM UserOrganizationRoleMappingEntity m WHERE m.user.id = :userId"),
    @NamedQuery(name = "deleteUserOrganizationRoleMappingsByRole", query = "DELETE FROM UserOrganizationRoleMappingEntity m WHERE m.organizationRole.id = :organizationRoleId"),
    @NamedQuery(name = "deleteUserOrganizationRoleMappingsByOrganization", query = "DELETE FROM UserOrganizationRoleMappingEntity m WHERE m.organizationRole.organization.id = :organizationId")
})
public class UserOrganizationRoleMappingEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORG_ROLE_ID")
    private OrganizationRoleEntity organizationRole;

    @Column(name = "USER_ID", insertable = false, updatable = false)
    private String userId;

    @Column(name = "ORG_ROLE_ID", insertable = false, updatable = false)
    private String organizationRoleId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public OrganizationRoleEntity getOrganizationRole() {
        return organizationRole;
    }

    public void setOrganizationRole(OrganizationRoleEntity organizationRole) {
        this.organizationRole = organizationRole;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrganizationRoleId() {
        return organizationRoleId;
    }

    public void setOrganizationRoleId(String organizationRoleId) {
        this.organizationRoleId = organizationRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserOrganizationRoleMappingEntity)) return false;

        UserOrganizationRoleMappingEntity that = (UserOrganizationRoleMappingEntity) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
