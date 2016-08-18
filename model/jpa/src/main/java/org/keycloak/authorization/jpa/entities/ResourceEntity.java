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

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_RESOURCE", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID", "OWNER"})
})
public class ResourceEntity implements Resource {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "URI")
    private String uri;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "ICON_URI")
    private String iconUri;

    @Column(name = "OWNER")
    private String owner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_SCOPE", joinColumns = @JoinColumn(name = "RESOURCE_ID"), inverseJoinColumns = @JoinColumn(name = "SCOPE_ID"))
    private List<ScopeEntity> scopes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_POLICY", joinColumns = @JoinColumn(name = "RESOURCE_ID"), inverseJoinColumns = @JoinColumn(name = "POLICY_ID"))
    private List<PolicyEntity> policies = new ArrayList<>();

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<ScopeEntity> getScopes() {
        return this.scopes;
    }

    @Override
    public String getIconUri() {
        return iconUri;
    }

    @Override
    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }

    @Override
    public ResourceServerEntity getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(ResourceServerEntity resourceServer) {
        this.resourceServer = resourceServer;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<PolicyEntity> getPolicies() {
        return this.policies;
    }

    public void updateScopes(Set<Scope> toUpdate) {
        for (Scope scope : toUpdate) {
            boolean hasScope = false;

            for (Scope existingScope : this.scopes) {
                if (existingScope.equals(scope)) {
                    hasScope = true;
                }
            }

            if (!hasScope) {
                this.scopes.add((ScopeEntity) scope);
            }
        }

        for (Scope scopeModel : new HashSet<Scope>(this.scopes)) {
            boolean hasScope = false;

            for (Scope scope : toUpdate) {
                if (scopeModel.equals(scope)) {
                    hasScope = true;
                }
            }

            if (!hasScope) {
                this.scopes.remove(scopeModel);
            }
        }
    }

    public void setPolicies(List<PolicyEntity> policies) {
        this.policies = policies;
    }
}
