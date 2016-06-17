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

import org.keycloak.authorization.model.Policy;
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
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_SCOPE", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID"})
})
public class ScopeEntity implements Scope {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "ICON_URI")
    private String iconUri;

    @ManyToOne(optional = false)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "SCOPE_POLICY", joinColumns = @JoinColumn(name = "SCOPE_ID"), inverseJoinColumns = @JoinColumn(name = "POLICY_ID"))
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

    public List<? extends Policy> getPolicies() {
        return this.policies;
    }

    public void setPolicies(List<PolicyEntity> policies) {
        this.policies = policies;
    }

    public void setResourceServer(final ResourceServerEntity resourceServer) {
        this.resourceServer = resourceServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeEntity that = (ScopeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
