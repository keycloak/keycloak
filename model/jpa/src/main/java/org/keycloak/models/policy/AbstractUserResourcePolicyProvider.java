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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.policy.conditions.IdentityProviderPolicyConditionFactory;
import org.keycloak.models.policy.conditions.IdentityProviderPolicyConditionProvider;

public abstract class AbstractUserResourcePolicyProvider implements ResourcePolicyProvider {

    private final ComponentModel policyModel;
    private final EntityManager em;
    private final KeycloakSession session;

    public AbstractUserResourcePolicyProvider(KeycloakSession session, ComponentModel model) {
        this.policyModel = model;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.session = session;
    }

    @Override
    public List<String> getEligibleResourcesForInitialAction() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<UserEntity> userRoot = query.from(UserEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        // Subquery will find if a state record exists for the user and policy
        // SELECT 1 FROM ResourcePolicyStateEntity s WHERE s.resourceId = userRoot.id AND s.policyId = :policyId
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ResourcePolicyStateEntity> stateRoot = subquery.from(ResourcePolicyStateEntity.class);
        subquery.select(cb.literal(1));
        subquery.where(
            cb.and(
                cb.equal(stateRoot.get("resourceId"), userRoot.get("id")),
                cb.equal(stateRoot.get("policyId"), policyModel.getId())
            )
        );
        Predicate notExistsPredicate = cb.not(cb.exists(subquery));
        predicates.add(notExistsPredicate);

        // origin-based condition
        Predicate originPredicate = buildOriginPredicate(cb, query, userRoot);
        if (originPredicate != null) {
            predicates.add(originPredicate);
        }

        // todo: add relationship predicates (groups, roles, perhaps user attribute?)

        query.select(userRoot.get("id")).where(predicates);
        return em.createQuery(query).getResultList();
    }

    protected Predicate buildOriginPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> userRoot) {
        // As of now we check only if there are broker aliases configured for the policy. More complete approach should check
        // a "origin" attribute for the origin type, and add the predicate accordingly
        // e.g. "origin" = "broker", check broker aliases; "origin" = "any" no need to filter anything; "origin" = "fed-provider", check "fed-providers", etc
        if (!this.getBrokerAliases().isEmpty()) {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<FederatedIdentityEntity> from = subquery.from(FederatedIdentityEntity.class);

            subquery.select(cb.literal(1));
            subquery.where(
                cb.and(
                    cb.equal(from.get("user").get("id"), userRoot.get("id")),
                    from.get("identityProvider").in(getBrokerAliases())
                )
            );
            return cb.exists(subquery);
        }
        return null;
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
    public boolean activateOnEvent(ResourcePolicyEvent event) {
        return this.supports(event.getResourceType())
                && this.getSupportedOperationsForActivation().contains(event.getOperation())
                && this.isResourceInScope(event.getResourceId());
    }

    @Override
    public boolean resetOnEvent(ResourcePolicyEvent event) {
        return this.supports(event.getResourceType())
                && this.getSupportedOperationsForResetting().contains(event.getOperation())
                && this.isResourceInScope(event.getResourceId());
    }

    public boolean deactivateOnEvent(ResourcePolicyEvent event) {
        return this.supports(event.getResourceType())
                && this.getSupportedOperationsForDeactivation().contains(event.getOperation())
                && !this.isResourceInScope(event.getResourceId());
    }

    @Override
    public void close() {
        // no-op
    }

    protected List<ResourceOperationType> getSupportedOperationsForActivation() {
        return this.getBrokerAliases().isEmpty()
                    ? Collections.emptyList()
                    : List.of(ResourceOperationType.ADD_FEDERATED_IDENTITY);
    }

    protected List<ResourceOperationType> getSupportedOperationsForResetting() {
        return Collections.emptyList();
    }

    protected List<ResourceOperationType> getSupportedOperationsForDeactivation() {
        return this.getBrokerAliases().isEmpty()
                    ? Collections.emptyList()
                    : List.of(ResourceOperationType.REMOVE_FEDERATED_IDENTITY);
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
        List<String> conditions = policyModel.getConfig().getOrDefault("conditions", List.of());

        for (String providerId : conditions) {
            ResourcePolicyConditionProvider condition = resolveCondition(providerId);

            if (condition instanceof IdentityProviderPolicyConditionProvider) {
                return getModel().getConfig().getOrDefault(providerId + "." + IdentityProviderPolicyConditionFactory.EXPECTED_ALIASES, List.of());
            }
        }

        return List.of();
    }

    private ResourcePolicyConditionProvider resolveCondition(String providerId) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        ResourcePolicyConditionProviderFactory<ResourcePolicyConditionProvider> providerFactory = (ResourcePolicyConditionProviderFactory<ResourcePolicyConditionProvider>) sessionFactory.getProviderFactory(ResourcePolicyConditionProvider.class, providerId);

        if (providerFactory == null) {
            throw new IllegalStateException("Could not find condition provider: " + providerId);
        }

        Map<String, List<String>> config = new HashMap<>();

        for (Entry<String, List<String>> configEntry : policyModel.getConfig().entrySet()) {
            if (configEntry.getKey().startsWith(providerId)) {
                config.put(configEntry.getKey().substring(providerId.length() + 1), configEntry.getValue());
            }
        }

        ResourcePolicyConditionProvider condition = providerFactory.create(session, config);

        if (condition == null) {
            throw new IllegalStateException("Factory " + providerFactory.getClass() + " returned a null provider");
        }

        return condition;
    }
}
