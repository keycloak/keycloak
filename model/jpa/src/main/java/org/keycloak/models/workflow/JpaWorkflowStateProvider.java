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

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import java.util.List;

public class JpaWorkflowStateProvider implements WorkflowStateProvider {

    private final EntityManager em;
    private static final Logger LOGGER = Logger.getLogger(JpaWorkflowStateProvider.class);
    private final KeycloakSession session;

    public JpaWorkflowStateProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public ScheduledAction getScheduledAction(String workflowId, String resourceId) {
        WorkflowActionStateEntity.PrimaryKey pk = new WorkflowActionStateEntity.PrimaryKey(resourceId, workflowId);
        WorkflowActionStateEntity entity = em.find(WorkflowActionStateEntity.class, pk);
        if (entity != null) {
            return new ScheduledAction(entity.getWorkflowId(), entity.getScheduledActionId(), entity.getResourceId());
        }
        return null;
    }

    @Override
    public void scheduleAction(Workflow workflow, WorkflowAction action, long scheduledTimeOffset, String resourceId) {
        WorkflowActionStateEntity.PrimaryKey pk = new WorkflowActionStateEntity.PrimaryKey(resourceId, workflow.getId());
        WorkflowActionStateEntity entity = em.find(WorkflowActionStateEntity.class, pk);
        if (entity == null) {
            entity = new WorkflowActionStateEntity();
            entity.setResourceId(resourceId);
            entity.setWorkflowId(workflow.getId());
            entity.setWorkflowProviderId(workflow.getProviderId());
            entity.setScheduledActionId(action.getId());
            entity.setScheduledActionTimestamp(Time.currentTimeMillis() + scheduledTimeOffset);
            em.persist(entity);
        }
        else {
            entity.setScheduledActionId(action.getId());
            entity.setScheduledActionTimestamp(Time.currentTimeMillis() + scheduledTimeOffset);
        }
    }

    @Override
    public List<ScheduledAction> getDueScheduledActions(Workflow workflow) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowActionStateEntity> query = cb.createQuery(WorkflowActionStateEntity.class);
        Root<WorkflowActionStateEntity> stateRoot = query.from(WorkflowActionStateEntity.class);

        Predicate byWorkflow = cb.equal(stateRoot.get("workflowId"), workflow.getId());
        Predicate isExpired = cb.lessThan(stateRoot.get("scheduledActionTimestamp"), Time.currentTimeMillis());

        query.where(cb.and(byWorkflow, isExpired));

        return em.createQuery(query).getResultStream()
                .map(s -> new ScheduledAction(s.getWorkflowId(), s.getScheduledActionId(), s.getResourceId()))
                .toList();
    }

    @Override
    public List<ScheduledAction> getScheduledActionsByWorkflow(String workflowId) {
        if (StringUtil.isBlank(workflowId)) {
            return List.of();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowActionStateEntity> query = cb.createQuery(WorkflowActionStateEntity.class);
        Root<WorkflowActionStateEntity> stateRoot = query.from(WorkflowActionStateEntity.class);

        Predicate byWorkflow = cb.equal(stateRoot.get("workflowId"), workflowId);
        query.where(byWorkflow);

        return em.createQuery(query).getResultStream()
                .map(s -> new ScheduledAction(s.getWorkflowId(), s.getScheduledActionId(), s.getResourceId()))
                .toList();
    }

    @Override
    public List<ScheduledAction> getScheduledActionsByResource(String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowActionStateEntity> query = cb.createQuery(WorkflowActionStateEntity.class);
        Root<WorkflowActionStateEntity> stateRoot = query.from(WorkflowActionStateEntity.class);

        Predicate byResource = cb.equal(stateRoot.get("resourceId"), resourceId);
        query.where(byResource);

        return em.createQuery(query).getResultStream()
                .map(s -> new ScheduledAction(s.getWorkflowId(), s.getScheduledActionId(), s.getResourceId()))
                .toList();
    }

    @Override
    public void removeByResource(String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowActionStateEntity> delete = cb.createCriteriaDelete(WorkflowActionStateEntity.class);
        Root<WorkflowActionStateEntity> root = delete.from(WorkflowActionStateEntity.class);
        delete.where(cb.equal(root.get("resourceId"), resourceId));
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                LOGGER.tracev("Deleted {0} orphaned state records for resource {1}", deletedCount, resourceId);
            }
        }
    }

    @Override
    public void remove(String workflowId, String resourceId) {
        WorkflowActionStateEntity.PrimaryKey pk = new WorkflowActionStateEntity.PrimaryKey(resourceId, workflowId);
        WorkflowActionStateEntity entity = em.find(WorkflowActionStateEntity.class, pk);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public void remove(String workflowId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowActionStateEntity> delete = cb.createCriteriaDelete(WorkflowActionStateEntity.class);
        Root<WorkflowActionStateEntity> root = delete.from(WorkflowActionStateEntity.class);
        delete.where(cb.equal(root.get("workflowId"), workflowId));
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                RealmModel realm = session.getContext().getRealm();
                LOGGER.tracev("Deleted {0} state records for realm {1}", deletedCount, realm.getId());
            }
        }
    }

    @Override
    public void removeAll() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowActionStateEntity> delete = cb.createCriteriaDelete(WorkflowActionStateEntity.class);
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                RealmModel realm = session.getContext().getRealm();
                LOGGER.tracev("Deleted {0} state records for realm {1}", deletedCount, realm.getId());
            }
        }
    }

    @Override
    public void close() {
    }

}
