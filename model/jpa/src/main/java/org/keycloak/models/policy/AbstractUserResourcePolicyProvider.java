/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.policy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Collections;
import java.util.List;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;

public abstract class AbstractUserResourcePolicyProvider implements ResourcePolicyProvider {

    private final ComponentModel policyModel;
    private final EntityManager em;
    private final KeycloakSession session;

    public AbstractUserResourcePolicyProvider(KeycloakSession session, ComponentModel model) {
        this.policyModel = model;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.session = session;
    }

    public abstract Predicate timePredicate(long time, CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> userRoot);

    // For each user row, a subquery is executed to check if a corresponding record exists in
    // the state table. If no record is found, the condition is met -> user is eligible for initial action

    @Override
    public List<String> getEligibleResourcesForInitialAction(long time) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<UserEntity> userRoot = query.from(UserEntity.class);

        // Subquery will find if a state record exists for the user and policy
        // SELECT 1 FROM ResourcePolicyStateEntity s WHERE s.resourceId = userRoot.id AND s.policyId = :policyId
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ResourcePolicyStateEntity> stateRoot = subquery.from(ResourcePolicyStateEntity.class);
        subquery.select(cb.literal(1)); // Select 1 for existence check
        subquery.where(
            cb.and(
                cb.equal(stateRoot.get("resourceId"), userRoot.get("id")),
                cb.equal(stateRoot.get("policyId"), policyModel.getId())
            )
        );

        // Time-based condition
        Predicate timePredicate = timePredicate(time, cb, query, userRoot);

        // NOT EXISTS condition
        Predicate notExistsPredicate = cb.not(cb.exists(subquery));

        query.where(cb.and(timePredicate, notExistsPredicate));
        query.select(userRoot.get("id"));

        return em.createQuery(query).getResultList();
    }
    @Override
    public List<String> filterEligibleResources(List<String> candidateResourceIds, long time) {
        // If there are no candidates, return an empty list
        if (candidateResourceIds == null || candidateResourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<UserEntity> userRoot = query.from(UserEntity.class);

        // Time-based condition
        Predicate timePredicate = timePredicate(time, cb, query, userRoot);

        // IN clause with candidateResourceIds
        Predicate inClausePredicate = userRoot.get("id").in(candidateResourceIds);

        query.where(cb.and(timePredicate, inClausePredicate));
        query.select(userRoot.get("id"));

        return em.createQuery(query).getResultList();
    }

    /**
     * Indicates whether the specified resource is in the scope of this policy. For example, a policy associated with a
     * broker is applicable only to users with a federated identity associated with the same broker.
     *
     * @param resourceId the id of the resource being checked.
     * @return {@code true} if the resource is in the policy scope; {@code false} otherwise.
     */
    protected boolean isResourceInScope(String resourceId) {
        UserModel user = this.getSession().users().getUserById(this.getRealm(), resourceId);
        if (user != null) {
            List<String> brokerAliases = this.getBrokerAliases();
            if (!brokerAliases.isEmpty()) {
                return session.users().getFederatedIdentitiesStream(this.getRealm(), user)
                        .map(FederatedIdentityModel::getIdentityProvider)
                        .anyMatch(brokerAliases::contains);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean supports(ResourceType type) {
        return ResourceType.USERS.equals(type);
    }

    @Override
    public boolean scheduleOnEvent(ResourcePolicyEvent event) {
        return this.supports(event.getResourceType())
                && this.getSupportedOperationsForScheduling().contains(event.getOperation())
                && this.isResourceInScope(event.getResourceId());
    }

    @Override
    public boolean resetOnEvent(ResourcePolicyEvent event) {
        return this.supports(event.getResourceType())
                && this.getSupportedOperationsForResetting().contains(event.getOperation())
                && this.isResourceInScope(event.getResourceId());
    }

    @Override
    public void close() {
        // no-op
    }

    protected List<ResourceOperationType> getSupportedOperationsForScheduling() {
        return List.of();
    }

    protected List<ResourceOperationType> getSupportedOperationsForResetting() {
        return List.of();
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    protected ComponentModel getModel() {
        return policyModel;
    }

    protected KeycloakSession getSession() {
        return session;
    }

    protected RealmModel getRealm() {
        return getSession().getContext().getRealm();
    }

    protected List<String> getBrokerAliases() {
        return getModel().getConfig().getOrDefault("broker-aliases", List.of());
    }
}
