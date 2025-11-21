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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.workflow.conditions.ExpressionWorkflowConditionProvider;
import org.keycloak.utils.StringUtil;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;

public class UserResourceTypeWorkflowProvider implements ResourceTypeSelector {

    private final EntityManager em;
    private final KeycloakSession session;

    public UserResourceTypeWorkflowProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public List<String> getResourceIds(Workflow workflow) {
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
                cb.equal(stateRoot.get("workflowId"), workflow.getId())
            )
        );
        Predicate notExistsPredicate = cb.not(cb.exists(subquery));
        predicates.add(notExistsPredicate);

        predicates.add(getConditionsPredicate(workflow, cb, query, userRoot));

        query.select(userRoot.get("id")).where(predicates);

        return em.createQuery(query).getResultList();
    }

    @Override
    public Object resolveResource(String resourceId) {
        Objects.requireNonNull(resourceId, "resourceId");
        return ResourceType.USERS.resolveResource(session, resourceId);
    }

    private Predicate getConditionsPredicate(Workflow workflow, CriteriaBuilder cb, CriteriaQuery<String> query, Root<UserEntity> path) {
        MultivaluedHashMap<String, String> config = workflow.getConfig();
        String conditions = config.getFirst(CONFIG_CONDITIONS);

        if (StringUtil.isBlank(conditions)) {
            return cb.conjunction();
        }

        return new ExpressionWorkflowConditionProvider(session, conditions).toPredicate(cb, query, path);
    }
}
