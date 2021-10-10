/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authorization.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_PERM_TICKET", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"OWNER", "REQUESTER", "RESOURCE_SERVER_ID", "RESOURCE_ID", "SCOPE_ID"})
})
@NamedQueries(
    {
        @NamedQuery(name="findPermissionIdByResource", query="select p.id from PermissionTicketEntity p inner join p.resource r where p.resourceServer.id = :serverId and (r.resourceServer = :serverId and r.id = :resourceId)"),
        @NamedQuery(name="findPermissionIdByScope", query="select p.id from PermissionTicketEntity p inner join p.scope s where p.resourceServer.id = :serverId and (s.resourceServer.id = :serverId and s.id = :scopeId)"),
        @NamedQuery(name="findPermissionTicketIdByServerId", query="select p.id from PermissionTicketEntity p where  p.resourceServer.id = :serverId "),
        @NamedQuery(name="findGrantedResources", query="select distinct(r.id) from ResourceEntity r inner join PermissionTicketEntity p on r.id = p.resource.id where p.grantedTimestamp is not null and p.requester = :requester order by r.id"),
        @NamedQuery(name="findGrantedResourcesByName", query="select distinct(r.id) from ResourceEntity r inner join PermissionTicketEntity p on r.id = p.resource.id where p.grantedTimestamp is not null and p.requester = :requester and lower(r.name) like :resourceName order by r.id"),
        @NamedQuery(name="findGrantedOwnerResources", query="select distinct(r.id) from ResourceEntity r inner join PermissionTicketEntity p on r.id = p.resource.id where p.grantedTimestamp is not null and p.owner = :owner order by r.id")
    }
)
public class PermissionTicketEntity {

    @Id
    @Column(name = "ID", length = 36)
    @Access(AccessType.PROPERTY)
    // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "OWNER")
    private String owner;

    @Column(name = "REQUESTER")
    private String requester;

    @Column(name = "CREATED_TIMESTAMP")
    private Long createdTimestamp;

    @Column(name = "GRANTED_TIMESTAMP")
    private Long grantedTimestamp;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOURCE_ID")
    private ResourceEntity resource;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "SCOPE_ID")
    private ScopeEntity scope;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "POLICY_ID")
    private PolicyEntity policy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ResourceEntity getResource() {
        return resource;
    }

    public void setResource(ResourceEntity resource) {
        this.resource = resource;
    }

    public ScopeEntity getScope() {
        return scope;
    }

    public void setScope(ScopeEntity scope) {
        this.scope = scope;
    }

    public ResourceServerEntity getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(ResourceServerEntity resourceServer) {
        this.resourceServer = resourceServer;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getRequester() {
        return requester;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Long getGrantedTimestamp() {
        return grantedTimestamp;
    }

    public void setGrantedTimestamp(long grantedTimestamp) {
        this.grantedTimestamp = grantedTimestamp;
    }

    public boolean isGranted() {
        return grantedTimestamp != null;
    }

    public PolicyEntity getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyEntity policy) {
        this.policy = policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PermissionTicketEntity that = (PermissionTicketEntity) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
