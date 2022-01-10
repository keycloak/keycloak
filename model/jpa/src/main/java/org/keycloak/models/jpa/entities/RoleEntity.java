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

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Nationalized;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        @NamedQuery(name="getClientRoles", query="select role from RoleEntity role where role.clientId = :client order by role.name"),
        @NamedQuery(name="getClientRoleIds", query="select role.id from RoleEntity role where role.clientId = :client"),
        @NamedQuery(name="getClientRoleByName", query="select role from RoleEntity role where role.name = :name and role.clientId = :client"),
        @NamedQuery(name="getClientRoleIdByName", query="select role.id from RoleEntity role where role.name = :name and role.clientId = :client"),
        @NamedQuery(name="searchForClientRoles", query="select role from RoleEntity role where role.clientId = :client and ( lower(role.name) like :search or lower(role.description) like :search ) order by role.name"),
        @NamedQuery(name="getRealmRoles", query="select role from RoleEntity role where role.clientRole = false and role.realmId = :realm order by role.name"),
        @NamedQuery(name="getRealmRoleIds", query="select role.id from RoleEntity role where role.clientRole = false and role.realmId = :realm"),
        @NamedQuery(name="getRealmRoleByName", query="select role from RoleEntity role where role.clientRole = false and role.name = :name and role.realmId = :realm"),
        @NamedQuery(name="getRealmRoleIdByName", query="select role.id from RoleEntity role where role.clientRole = false and role.name = :name and role.realmId = :realm"),
        @NamedQuery(name="searchForRealmRoles", query="select role from RoleEntity role where role.clientRole = false and role.realmId = :realm and ( lower(role.name) like :search or lower(role.description) like :search ) order by role.name"),
        @NamedQuery(name="getRoleIdsFromIdList", query="select role.id from RoleEntity role where role.realmId = :realm and role.id in :ids order by role.name ASC"),
        @NamedQuery(name="getRoleIdsByNameContainingFromIdList", query="select role.id from RoleEntity role where role.realmId = :realm and lower(role.name) like lower(concat('%',:search,'%')) and role.id in :ids order by role.name ASC"),
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

    @Column(name="CLIENT")
    private String clientId;

    // Hack to ensure that either name+client or name+realm are unique. Needed due to MS-SQL as it don't allow multiple NULL values in the column, which is part of constraint
    @Column(name="CLIENT_REALM_CONSTRAINT", length = 36)
    private String clientRealmConstraint;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "COMPOSITE_ROLE", joinColumns = @JoinColumn(name = "COMPOSITE"), inverseJoinColumns = @JoinColumn(name = "CHILD_ROLE"))
    private Set<RoleEntity> compositeRoles;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="role")
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    protected List<RoleAttributeEntity> attributes;

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

    public Set<RoleEntity> getCompositeRoles() {
        if (compositeRoles == null) {
            compositeRoles = new HashSet<>();
        }
        return compositeRoles;
    }

    public void setCompositeRoles(Set<RoleEntity> compositeRoles) {
        this.compositeRoles = compositeRoles;
    }

    public boolean isClientRole() {
        return clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.clientRole = clientRole;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        this.clientRealmConstraint = clientId;
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
