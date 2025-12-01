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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Nationalized;

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
                @NamedQuery(name="findPolicyIdByResource", query="select p from PolicyEntity p inner join p.resources r where p.resourceServer.id = :serverId and (r.resourceServer = :serverId and r.id = :resourceId)"),
                @NamedQuery(name="findPolicyIdByScope", query="select pe from PolicyEntity pe inner join pe.scopes s where pe.type = 'scope' and pe.resourceServer.id = :serverId and s.id in (:scopeIds)"),
                @NamedQuery(name="findPolicyIdByResourceScope", query="select pe from PolicyEntity pe inner join pe.resources r inner join pe.scopes s where pe.resourceServer.id = :serverId and pe.type = 'scope' and s.id in (:scopeIds) and r.id in (:resourceId)"),
                @NamedQuery(name="findPolicyIdByNullResourceScope", query="select pe from PolicyEntity pe left join fetch pe.config c inner join pe.scopes s  where pe.resourceServer.id = :serverId and pe.type = 'scope' and pe.resources is empty and s.id in (:scopeIds) and not exists (select pec from pe.config pec where KEY(pec) = 'defaultResourceType')"),
                @NamedQuery(name="findPolicyIdByType", query="select p.id from PolicyEntity p where p.resourceServer.id = :serverId and p.type = :type"),
                @NamedQuery(name="findPolicyIdByResourceType", query="select p from PolicyEntity p inner join p.config c inner join fetch p.associatedPolicies a where p.resourceServer.id = :serverId and KEY(c) = 'defaultResourceType' and c like :type"),
                @NamedQuery(name="findPolicyIdByDependentPolices", query="select p.id from PolicyEntity p inner join p.associatedPolicies ap where p.resourceServer.id = :serverId and (ap.resourceServer.id = :serverId and ap.id = :policyId)"),
                @NamedQuery(name="deletePolicyByResourceServer", query="delete from PolicyEntity p where p.resourceServer.id = :serverId"),
                @NamedQuery(name="findDependentPolicyByResourceTypeAndConfig", query="select p.id from PolicyEntity p inner join p.scopes s inner join p.config c inner join p.associatedPolicies ap inner join ap.config ac where p.resourceServer.id = :serverId and (s.name in ('" + AdminPermissionsSchema.VIEW + "', '" + AdminPermissionsSchema.VIEW_MEMBERS + "')) and ap.resourceServer.id = :serverId and ap.type = :associatedPolicyType and (KEY(c) = 'defaultResourceType' and c like :resourceType) and (KEY(ac) = :configKey and ac like :configValue)")
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
    private Map<String, String> config;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "RESOURCE_SERVER_ID")
    private ResourceServerEntity resourceServer;

    @OneToMany(fetch = FetchType.EAGER, cascade = {})
    @JoinTable(name = "ASSOCIATED_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "ASSOCIATED_POLICY_ID"))
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 20)
    private Set<PolicyEntity> associatedPolicies;

    @OneToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "RESOURCE_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "RESOURCE_ID"))
    private Set<ResourceEntity> resources;

    @OneToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "SCOPE_POLICY", joinColumns = @JoinColumn(name = "POLICY_ID"), inverseJoinColumns = @JoinColumn(name = "SCOPE_ID"))
    private Set<ScopeEntity> scopes;

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
        if (config == null) {
            config = new HashMap<>();
        }
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
        if (resources == null) {
            resources = new HashSet<>();
        }
        return this.resources;
    }

    public void setResources(Set<ResourceEntity> resources) {
        this.resources = resources;
    }

    public Set<ScopeEntity> getScopes() {
        if (scopes == null) {
            scopes = new HashSet<>();
        }
        return this.scopes;
    }

    public void setScopes(Set<ScopeEntity> scopes) {
        this.scopes = scopes;
    }

    public Set<PolicyEntity> getAssociatedPolicies() {
        if (associatedPolicies == null) {
            associatedPolicies = new HashSet<>();
        }
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
