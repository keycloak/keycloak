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
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.entities.AbstractIdentifiableEntity;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_POLICY", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID"})
})
public class PolicyEntity implements Policy {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "DECISION_STRATEGY")
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;

    @Column(name = "LOGIC")
    private Logic logic = Logic.POSITIVE;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="POLICY_CONFIG", joinColumns={ @JoinColumn(name="POLICY_ID") })
    private Map<String, String> config = new HashMap();

    @ManyToOne(optional = false)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "ASSOCIATED_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "ASSOCIATED_POLICY_ID"))
    private Set<PolicyEntity> associatedPolicies = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "RESOURCE_ID"))
    private Set<ResourceEntity> resources = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {})
    @JoinTable(name = "SCOPE_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "SCOPE_ID"))
    private Set<ScopeEntity> scopes = new HashSet<>();

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    @Override
    public Logic getLogic() {
        return this.logic;
    }

    @Override
    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    @Override
    public Map<String, String> getConfig() {
        return this.config;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ResourceServerEntity getResourceServer() {
        return this.resourceServer;
    }

    public void setResourceServer(ResourceServerEntity resourceServer) {
        this.resourceServer = resourceServer;
    }

    @Override
    public <P extends Policy> Set<P> getAssociatedPolicies() {
        return (Set<P>) this.associatedPolicies;
    }

    public void setAssociatedPolicies(Set<PolicyEntity> associatedPolicies) {
        this.associatedPolicies = associatedPolicies;
    }

    @Override
    public Set<ResourceEntity> getResources() {
        return this.resources;
    }

    public void setResources(Set<ResourceEntity> resources) {
        this.resources = resources;
    }

    @Override
    public Set<ScopeEntity> getScopes() {
        return this.scopes;
    }

    public void setScopes(Set<ScopeEntity> scopes) {
        this.scopes = scopes;
    }

    @Override
    public void addScope(Scope scope) {
        getScopes().add((ScopeEntity) scope);
    }

    @Override
    public void removeScope(Scope scope) {
        getScopes().remove(scope);
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        getAssociatedPolicies().add(associatedPolicy);
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        getAssociatedPolicies().remove(associatedPolicy);
    }

    @Override
    public void addResource(Resource resource) {
        getResources().add((ResourceEntity) resource);
    }

    @Override
    public void removeResource(Resource resource) {
        getResources().remove(resource);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (this.id == null) return false;

        if (!Policy.class.isInstance(o)) return false;

        Policy that = (Policy) o;

        if (!getId().equals(that.getId())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }
}
