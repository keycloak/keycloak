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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

import org.keycloak.utils.StringUtil;

@Table(name="ORG")
@Entity
@NamedQueries({
        @NamedQuery(name="getByOrgName", query="select distinct o from OrganizationEntity o where o.realmId = :realmId AND o.name = :name"),
        @NamedQuery(name="getByDomainName", query="select distinct o from OrganizationEntity o inner join OrganizationDomainEntity d ON o.id = d.organization.id" +
                " where o.realmId = :realmId AND d.name = :name"),
        @NamedQuery(name="getCount", query="select count(o) from OrganizationEntity o where o.realmId = :realmId"),
        @NamedQuery(name="deleteOrganizationsByRealm", query="delete from OrganizationEntity o where o.realmId = :realmId"),
        @NamedQuery(name="getGroupsByMember", query="select m.groupId from UserGroupMembershipEntity m join GroupEntity g on g.id = m.groupId where g.type = 1 and m.user.id = :userId"),
        @NamedQuery(name="getGroupsByFederatedMember", query="select m.groupId from FederatedUserGroupMembershipEntity m join GroupEntity g on g.id = m.groupId where g.type = 1 and m.userId = :userId")
})
public class OrganizationEntity {

    @Id
    @Column(name = "ID", length = 36)
    @Access(AccessType.PROPERTY)
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "ALIAS")
    private String alias;

    @Column(name = "ENABLED")
    private boolean enabled;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REDIRECT_URL")
    private String redirectUrl;

    @Column(name = "REALM_ID")
    private String realmId;

    @Column(name = "GROUP_ID")
    private String groupId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="organization")
    protected Set<OrganizationDomainEntity> domains = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        if (StringUtil.isNullOrEmpty(redirectUrl)) {
            redirectUrl = null;
        }
        this.redirectUrl = redirectUrl;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realm) {
        this.realmId = realm;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public Collection<OrganizationDomainEntity> getDomains() {
        if (this.domains == null) {
            this.domains = new HashSet<>();
        }
        return this.domains;
    }

    public void addDomain(OrganizationDomainEntity domainEntity) {
        this.domains.add(domainEntity);
    }

    public void removeDomain(OrganizationDomainEntity domainEntity) {
        this.domains.remove(domainEntity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof OrganizationEntity)) return false;

        OrganizationEntity that = (OrganizationEntity) o;

        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return super.hashCode();
        }
        return id.hashCode();
    }
}
