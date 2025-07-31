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
import jakarta.persistence.criteria.Root;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.policy.entity.ResourceActionEntity;
import org.keycloak.models.policy.entity.ResourcePolicyEntity;
import org.keycloak.models.utils.KeycloakModelUtils;


public class JpaResourcePolicyProvider implements ResourcePolicyProvider {

    private final RealmModel realm;
    private final EntityManager em;
    private final ResourcePolicyStateProvider stateProvider;

    public JpaResourcePolicyProvider(KeycloakSession session) {
        this.realm = session.getContext().getRealm();
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        this.stateProvider = session.getProvider(ResourcePolicyStateProvider.class);
    }

    @Override
    public void close() {
    }

    @Override
    public ResourcePolicy addPolicy(ResourcePolicy policy) {
        ResourcePolicyEntity entity = new ResourcePolicyEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setRealmId(realm.getId());
        entity.setProviderId(policy.getProviderId());
        em.persist(entity);
        
        return entityToModel(entity);
    }

    @Override
    public void deletePolicy(String policyId) {
        ResourcePolicyEntity entity = em.find(ResourcePolicyEntity.class, policyId);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public ResourcePolicy getPolicy(String policyId) {
        ResourcePolicyEntity entity = em.find(ResourcePolicyEntity.class, policyId);
        return entity != null ? entityToModel(entity) : null;
    }

    //todo change this to return stream?
    @Override
    public List<ResourcePolicy> getPolicies() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ResourcePolicyEntity> query = cb.createQuery(ResourcePolicyEntity.class);
        Root<ResourcePolicyEntity> policyRoot = query.from(ResourcePolicyEntity.class);

        query.select(policyRoot);
        query.where(cb.equal(policyRoot.get("realmId"), realm.getId()));

        return em.createQuery(query)
            .getResultStream()
            .map(this::entityToModel)
            .collect(Collectors.toList());
    }

    @Override
    public List<ResourceAction> getActions(String policyId) {
        ResourcePolicyEntity policyEntity = em.find(ResourcePolicyEntity.class, policyId);
        if (policyEntity == null) {
            return Collections.emptyList();
        }
        return policyEntity.getActions().stream()
                .map(this::entityToModel)
                .collect(Collectors.toList());
    }

    @Override
    public void updateActions(String policyId, List<ResourceAction> actions) {
        validateActions(actions);

        ResourcePolicyEntity policyEntity = em.find(ResourcePolicyEntity.class, policyId);
        if (policyEntity == null) {
            throw new ModelException("Policy with ID " + policyId + " not found.");
        }

        // find which stable action IDs have been deleted.
        Set<String> oldActionIds = policyEntity.getActions().stream()
                .map(ResourceActionEntity::getId)
                .collect(Collectors.toSet());

        Set<String> newActionIds = actions.stream()
            .map(ResourceAction::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // find which action IDs were deleted
        oldActionIds.removeAll(newActionIds); // The remaining IDs are the deleted ones
        Set<String> deletedActionIds = oldActionIds;

        // delete orphaned state records - this means that we actually reset the flow for users which completed the action which is being removed
        // it seems like the best way to handle this
        if (!deletedActionIds.isEmpty()) {
            stateProvider.deleteStatesByCompletedActions(policyId, deletedActionIds);
        }

        // clear the existing collection of actions.
        policyEntity.getActions().clear();

        // map the new actions to entities and add them back to the policy.
        int priority = 1;
        for (ResourceAction action : actions) {
            ResourceActionEntity actionEntity = new ResourceActionEntity();

            actionEntity.setId(action.getId() != null ? action.getId() : KeycloakModelUtils.generateId());
            actionEntity.setProviderId(action.getProviderId());
            actionEntity.setAfterMs(action.getAfter());
            actionEntity.setPriority(priority++);
            actionEntity.setPolicy(policyEntity);

            policyEntity.getActions().add(actionEntity);
        }
    }

    private void validateActions(List<ResourceAction> actions) {
        // the list should be in the desired priority order
        for (int i = 0; i < actions.size(); i++) {
            ResourceAction currentAction = actions.get(i);

            // check that each action's duration is positive.
            if (currentAction.getAfter() <= 0) {
                throw new ModelException("Validation Error: 'after' duration must be positive.");
            }

            if (i > 0) {// skip for initial action
                ResourceAction previousAction = actions.get(i - 1);
                // compare current with the previous action in the list
                if (currentAction.getAfter() < previousAction.getAfter()) {
                    throw new ModelException(
                        String.format("Validation Error: The 'after' duration for action #%d (%s) cannot be less than the duration of the preceding action #%d (%s).",
                            i + 1, formatDuration(currentAction.getAfter()),
                            i, formatDuration(previousAction.getAfter()))
                    );
                }
            }
        }
    }

    private String formatDuration(long millis) {
        long days = Duration.ofMillis(millis).toDays();
        if (days > 0) {
            return String.format("%d day(s)", days);
        } else {
            long hours = Duration.ofMillis(millis).toHours();
            return String.format("%d hour(s)", hours);
        }
    }

    private ResourcePolicy entityToModel(ResourcePolicyEntity entity) {
        ResourcePolicy policy = new ResourcePolicy(entity.getProviderId());
        policy.setId(entity.getId());
        return policy;
    }

    private ResourceAction entityToModel(ResourceActionEntity entity) {
        ResourceAction action = new ResourceAction(entity.getProviderId());
        action.setId(entity.getId());
        action.setAfter(entity.getAfterMs());
        action.setPriority(entity.getPriority());
        return action;
    }
}
