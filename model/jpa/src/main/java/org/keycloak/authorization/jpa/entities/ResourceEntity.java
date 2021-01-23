/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_RESOURCE", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID", "OWNER"})
})
@NamedQueries(
        {
                @NamedQuery(name="findResourceIdByOwner", query="select r from ResourceEntity r  where r.resourceServer = :serverId and r.owner = :owner"),
                @NamedQuery(name="findResourceIdByOwnerOrdered", query="select r from ResourceEntity r where r.resourceServer = :serverId and r.owner = :owner order by r.id"),
                @NamedQuery(name="findAnyResourceIdByOwner", query="select r from ResourceEntity r where r.owner = :owner"),
                @NamedQuery(name="findAnyResourceIdByOwnerOrdered", query="select r.id from ResourceEntity r where r.owner = :owner order by r.id"),
                @NamedQuery(name="findResourceIdByUri", query="select r.id from ResourceEntity r where  r.resourceServer = :serverId  and :uri in elements(r.uris)"),
                @NamedQuery(name="findResourceIdByName", query="select r from ResourceEntity r left join fetch r.scopes s where  r.resourceServer = :serverId  and r.owner = :ownerId and r.name = :name"),
                @NamedQuery(name="findResourceIdByType", query="select r from ResourceEntity r left join fetch r.scopes s where  r.resourceServer = :serverId  and r.owner = :ownerId and r.type = :type"),
                @NamedQuery(name="findResourceIdByTypeNoOwner", query="select r from ResourceEntity r left join fetch r.scopes s where  r.resourceServer = :serverId  and r.type = :type"),
                @NamedQuery(name="findResourceIdByTypeInstance", query="select r from ResourceEntity r left join fetch r.scopes s where  r.resourceServer = :serverId and r.type = :type and r.owner <> :serverId"),
                @NamedQuery(name="findResourceIdByServerId", query="select r.id from ResourceEntity r where  r.resourceServer = :serverId "),
                @NamedQuery(name="findResourceIdByScope", query="select r from ResourceEntity r inner join r.scopes s where r.resourceServer = :serverId and (s.resourceServer = :serverId and s.id in (:scopeIds))"),
                @NamedQuery(name="deleteResourceByResourceServer", query="delete from ResourceEntity r where r.resourceServer = :serverId")
        }
)
public class ResourceEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "VALUE")
    @CollectionTable(name = "RESOURCE_URIS", joinColumns = { @JoinColumn(name="RESOURCE_ID") })
    private Set<String> uris;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "ICON_URI")
    private String iconUri;

    @Column(name = "OWNER")
    private String owner;

    @Column(name = "OWNER_MANAGED_ACCESS")
    private boolean ownerManagedAccess;

    @Column(name = "RESOURCE_SERVER_ID")
    private String resourceServer;

    @OneToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_SCOPE", joinColumns = @JoinColumn(name = "RESOURCE_ID"), inverseJoinColumns = @JoinColumn(name = "SCOPE_ID"))
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    private List<ScopeEntity> scopes;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_POLICY", joinColumns = @JoinColumn(name = "RESOURCE_ID"), inverseJoinColumns = @JoinColumn(name = "POLICY_ID"))
    private List<PolicyEntity> policies;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy="resource", fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    private Collection<ResourceAttributeEntity> attributes;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<String> getUris() {
        if (uris == null) {
            uris = new HashSet<>();
        }
        return uris;
    }

    public void setUris(Set<String> uris) {
        this.uris = uris;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ScopeEntity> getScopes() {
        if (scopes == null) {
            scopes = new LinkedList<>();
        }
        return this.scopes;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    public String getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(String resourceServer) {
        this.resourceServer = resourceServer;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setOwnerManagedAccess(boolean ownerManagedAccess) {
        this.ownerManagedAccess = ownerManagedAccess;
    }

    public boolean isOwnerManagedAccess() {
        return ownerManagedAccess;
    }

    public List<PolicyEntity> getPolicies() {
        if (policies == null) {
            policies = new LinkedList<>();
        }
        return this.policies;
    }


    public void setPolicies(List<PolicyEntity> policies) {
        this.policies = policies;
    }

    public Collection<ResourceAttributeEntity> getAttributes() {
        if (attributes == null) {
            attributes = new LinkedList<>();
        }
        return attributes;
    }

    public void setAttributes(Collection<ResourceAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceEntity that = (ResourceEntity) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
