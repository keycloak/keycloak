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

package org.keycloak.models.jpa.entities;

import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.keycloak.models.RoleModel;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Nationalized;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
//@DynamicInsert
//@DynamicUpdate
@Table(name="KEYCLOAK_ROLE", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "NAME", "CLIENT_REALM_CONSTRAINT" })
})
@NamedQueries({
        @NamedQuery(name="getClientRoles", query="select role from RoleEntity role where role.type = 'CLIENT' and role.clientId = :client order by role.name"),
        @NamedQuery(name="getClientRoleIds", query="select role.id from RoleEntity role where role.type = 'CLIENT' and role.clientId = :client"),
        @NamedQuery(name="getClientRoleByName", query="select role from RoleEntity role where role.type = 'CLIENT' and role.name = :name and role.clientId = :client"),
        @NamedQuery(name="getClientRoleIdByName", query="select role.id from RoleEntity role where role.type = 'CLIENT' and role.name = :name and role.clientId = :client"),
        @NamedQuery(name="searchForClientRoles", query="select role from RoleEntity role where role.type = 'CLIENT' and role.clientId = :client and ( lower(role.name) like :search or lower(role.description) like :search ) order by role.name"),
        @NamedQuery(name="getRealmRoles", query="select role from RoleEntity role where role.type = 'REALM' and role.realmId = :realm order by role.name"),
        @NamedQuery(name="getRealmRoleIds", query="select role.id from RoleEntity role where role.type = 'REALM' and role.realmId = :realm"),
        @NamedQuery(name="getRealmRoleByName", query="select role from RoleEntity role where role.type = 'REALM' and role.name = :name and role.realmId = :realm"),
        @NamedQuery(name="getRealmRoleIdByName", query="select role.id from RoleEntity role where role.type = 'REALM' and role.name = :name and role.realmId = :realm"),
        @NamedQuery(name="searchForRealmRoles", query="select role from RoleEntity role where role.type = 'REALM' and role.realmId = :realm and ( lower(role.name) like :search or lower(role.description) like :search ) order by role.name"),
        @NamedQuery(name="getOrganizationRoles", query="select role from RoleEntity role where role.type = 'ORGANIZATION' and role.organizationId = :organization order by role.name"),
        @NamedQuery(name="getOrganizationRoleIdsByRealm", query="select role.id from RoleEntity role where role.type = 'ORGANIZATION' and role.realmId = :realm"),
        @NamedQuery(name="getOrganizationRoleIdByName", query="select role.id from RoleEntity role where role.type = 'ORGANIZATION' and role.name = :name and role.organizationId = :organization"),
        @NamedQuery(name="searchForOrganizationRoles", query="select role from RoleEntity role where role.type = 'ORGANIZATION' and role.organizationId = :organization and ( lower(role.name) like :search or lower(role.description) like :search ) order by role.name"),
        @NamedQuery(name="getOrganizationRolesCount", query="select count(role) from RoleEntity role where role.type = 'ORGANIZATION' and role.organizationId = :organization"),
        @NamedQuery(name="searchForOrganizationRolesCount", query="select count(role) from RoleEntity role where role.type = 'ORGANIZATION' and role.organizationId = :organization and ( lower(role.name) like :search or lower(role.description) like :search )"),
        @NamedQuery(name="getRoleIdsFromIdList", query="select role.id from RoleEntity role where role.realmId = :realm and role.id in :ids order by role.name ASC"),
        @NamedQuery(name="getRoleIdsByNameContainingFromIdList", query="select role.id from RoleEntity role where role.realmId = :realm and lower(role.name) like lower(concat('%',:search,'%')) and role.id in :ids order by role.name ASC"),
        @NamedQuery(name="getChildRoles", query="select r from RoleEntity r join CompositeRoleEntity c on r.id = c.childRole.id where c.parentRole.id = :parentRoleId"),
        @NamedQuery(name="getChildRolesFromParentIds", query="select distinct r from RoleEntity r join CompositeRoleEntity c on r.id = c.childRole.id where c.parentRole.id in :parentRoleIds"),
})

public class RoleEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Nationalized
    @Column(name = "NAME")
    private String name;
    @Nationalized
    @Column(name = "DESCRIPTION")
    private String description;

    // hax! couldn't get constraint to work properly
    @Column(name = "REALM_ID")
    private String realmId;

    @Column(name="CLIENT_ROLE")
    private boolean clientRole;

    @Column(name="TYPE", length = 32)
    private String type = RoleModel.Type.REALM.name();

    @Column(name="CLIENT")
    private String clientId;

    @Column(name="ORG_ID")
    private String organizationId;

    // Hack to ensure that either name+client or name+realm are unique. Needed due to MS-SQL as it don't allow multiple NULL values in the column, which is part of constraint
    @Column(name="CLIENT_REALM_CONSTRAINT", length = 36)
    private String clientRealmConstraint;

    // Explicitly not using OrphanRemoval as we're handling the removal manually through HQL but at the same time we still
    // want to remove elements from the entity's collection in a manual way. Without this, Hibernate would do a duplicit
    // delete query.
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = false, mappedBy="role")
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    protected List<RoleAttributeEntity> attributes = new LinkedList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
        this.clientRealmConstraint = realmId;
    }

    public List<RoleAttributeEntity> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedList<>();
        }
        return attributes;
    }

    public void setAttributes(List<RoleAttributeEntity> attributes) {
        this.attributes = attributes;
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

    public boolean isClientRole() {
        return clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.clientRole = clientRole;
    }

    public RoleModel.Type getType() {
        return type == null ? RoleModel.Type.REALM : RoleModel.Type.valueOf(type);
    }

    public void setType(RoleModel.Type type) {
        this.type = type == null ? null : type.name();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        this.clientRealmConstraint = clientId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
        this.clientRealmConstraint = organizationId;
    }

    public String getClientRealmConstraint() {
        return clientRealmConstraint;
    }

    public void setClientRealmConstraint(String clientRealmConstraint) {
        this.clientRealmConstraint = clientRealmConstraint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof RoleEntity)) return false;

        RoleEntity that = (RoleEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
