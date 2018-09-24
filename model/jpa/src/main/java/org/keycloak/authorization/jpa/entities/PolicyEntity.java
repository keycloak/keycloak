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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Nationalized;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Entity
@Table(name = "RESOURCE_SERVER_POLICY", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID"})
})
@NamedQueries(
        {
                @NamedQuery(name="findPolicyIdByServerId", query="select p.id from PolicyEntity p where  p.resourceServer.id = :serverId "),
                @NamedQuery(name="findPolicyIdByName", query="select p from PolicyEntity p left join fetch p.associatedPolicies a where  p.resourceServer.id = :serverId  and p.name = :name"),
                @NamedQuery(name="findPolicyIdByResource", query="select p from PolicyEntity p inner join fetch p.resources r left join fetch p.scopes s inner join fetch p.associatedPolicies a where p.resourceServer.id = :serverId and (r.resourceServer.id = :serverId and r.id = :resourceId)"),
                @NamedQuery(name="findPolicyIdByScope", query="select pe from PolicyEntity pe left join fetch pe.resources r inner join fetch pe.scopes s inner join fetch pe.associatedPolicies a where pe.resourceServer.id = :serverId and exists (select p.id from ScopeEntity s inner join s.policies p where s.resourceServer.id = :serverId and (p.resourceServer.id = :serverId and p.type = 'scope' and s.id in (:scopeIds) and p.id = pe.id))"),
                @NamedQuery(name="findPolicyIdByResourceScope", query="select pe from PolicyEntity pe inner join fetch pe.resources r inner join fetch pe.scopes s inner join fetch pe.associatedPolicies a where pe.resourceServer.id = :serverId and exists (select p.id from ScopeEntity s inner join s.policies p where s.resourceServer.id = :serverId and (p.resourceServer.id = :serverId and p.type = 'scope' and s.id in (:scopeIds) and p.id = pe.id)) and exists (select p.id from ResourceEntity r inner join r.policies p where r.resourceServer.id = :serverId and (p.resourceServer.id = :serverId and p.id = pe.id and p.type = 'scope' and r.id in (:resourceId)))"),
                @NamedQuery(name="findPolicyIdByNullResourceScope", query="select pe from PolicyEntity pe left join fetch pe.resources r inner join fetch pe.scopes s inner join fetch pe.associatedPolicies a where pe.resourceServer.id = :serverId and exists (select p.id from ScopeEntity s inner join s.policies p where s.resourceServer.id = :serverId and (p.resourceServer.id = :serverId and p.id = pe.id and p.type = 'scope' and s.id in (:scopeIds))) and pe.resources is empty"),
                @NamedQuery(name="findPolicyIdByType", query="select p.id from PolicyEntity p where p.resourceServer.id = :serverId and p.type = :type"),
                @NamedQuery(name="findPolicyIdByResourceType", query="select p from PolicyEntity p inner join p.config c inner join fetch p.associatedPolicies a where p.resourceServer.id = :serverId and KEY(c) = 'defaultResourceType' and c like :type"),
                @NamedQuery(name="findPolicyIdByDependentPolices", query="select p.id from PolicyEntity p inner join p.associatedPolicies ap where p.resourceServer.id = :serverId and (ap.resourceServer.id = :serverId and ap.id = :policyId)"),
                @NamedQuery(name="deletePolicyByResourceServer", query="delete from PolicyEntity p where p.resourceServer.id = :serverId")
        }
)

public class PolicyEntity {

    @Id
    @Column(name = "ID", length = 36)
    @Access(AccessType.PROPERTY)
    // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name = "NAME")
    private String name;

    @Nationalized
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "DECISION_STRATEGY")
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;

    @Column(name = "LOGIC")
    private Logic logic = Logic.POSITIVE;

    @ElementCollection(fetch = FetchType.LAZY)
    @MapKeyColumn(name = "NAME")
    @Column(name = "VALUE", columnDefinition = "TEXT")
    @CollectionTable(name = "POLICY_CONFIG", joinColumns = {@JoinColumn(name = "POLICY_ID")})
    private Map<String, String> config = new HashMap();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @OneToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "ASSOCIATED_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "ASSOCIATED_POLICY_ID"))
    private Set<PolicyEntity> associatedPolicies = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "RESOURCE_ID"))
    private Set<ResourceEntity> resources = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "SCOPE_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "SCOPE_ID"))
    private Set<ScopeEntity> scopes = new HashSet<>();

    @Column(name = "OWNER")
    private String owner;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DecisionStrategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.decisionStrategy = decisionStrategy;
    }

    public Logic getLogic() {
        return this.logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceServerEntity getResourceServer() {
        return this.resourceServer;
    }

    public void setResourceServer(ResourceServerEntity resourceServer) {
        this.resourceServer = resourceServer;
    }

    public Set<ResourceEntity> getResources() {
        return this.resources;
    }

    public void setResources(Set<ResourceEntity> resources) {
        this.resources = resources;
    }

    public Set<ScopeEntity> getScopes() {
        return this.scopes;
    }

    public void setScopes(Set<ScopeEntity> scopes) {
        this.scopes = scopes;
    }

    public Set<PolicyEntity> getAssociatedPolicies() {
        return associatedPolicies;
    }

    public void setAssociatedPolicies(Set<PolicyEntity> associatedPolicies) {
        this.associatedPolicies = associatedPolicies;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PolicyEntity that = (PolicyEntity) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
