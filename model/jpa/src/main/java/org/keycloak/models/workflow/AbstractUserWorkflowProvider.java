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

package org.keycloak.models.workflow;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.UserEntity;

public abstract class AbstractUserWorkflowProvider extends EventBasedWorkflowProvider {

    private final EntityManager em;

    public AbstractUserWorkflowProvider(KeycloakSession session, ComponentModel model) {
        super(session, model);
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public List<String> getEligibleResourcesForInitialStep() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<UserEntity> userRoot = query.from(UserEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        // Subquery will find if a state record exists for the user and workflow
        // SELECT 1 FROM WorkflowActionStateEntity s WHERE s.resourceId = userRoot.id AND s.workflowId = :workflowId
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<WorkflowStateEntity> stateRoot = subquery.from(WorkflowStateEntity.class);
        subquery.select(cb.literal(1));
        subquery.where(
            cb.and(
                cb.equal(stateRoot.get("resourceId"), userRoot.get("id")),
                cb.equal(stateRoot.get("workflowId"), getModel().getId())
            )
        );
        Predicate notExistsPredicate = cb.not(cb.exists(subquery));
        predicates.add(notExistsPredicate);

        predicates.addAll(getConditionsPredicate(cb, query, userRoot));

        query.select(userRoot.get("id")).where(predicates);

        return em.createQuery(query).getResultList();
    }

    private List<Predicate> getConditionsPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> path) {
        MultivaluedHashMap<String, String> config = getModel().getConfig();
        List<String> conditions = config.getOrDefault(CONFIG_CONDITIONS, List.of());

        if (conditions.isEmpty()) {
            return List.of();
        }

        List<Predicate> predicates = new ArrayList<>();

        for (String providerId : conditions) {
            WorkflowConditionProvider condition = getManager().getConditionProvider(providerId, config);
            Predicate predicate = condition.toPredicate(cb, query, path);

            if (predicate != null) {
                predicates.add(predicate);
            }
        }

        return predicates;
    }

    @Override
    public void close() {
        // no-op
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    protected RealmModel getRealm() {
        return getSession().getContext().getRealm();
    }
}
