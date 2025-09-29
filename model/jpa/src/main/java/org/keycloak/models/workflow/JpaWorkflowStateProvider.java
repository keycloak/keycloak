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
    public ScheduledStep getScheduledStep(String workflowId, String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowStateEntity> query = cb.createQuery(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> stateRoot = query.from(WorkflowStateEntity.class);

        query.where(cb.and(cb.equal(stateRoot.get("workflowId"), workflowId), cb.equal(stateRoot.get("resourceId"), resourceId)));
        WorkflowStateEntity entity = em.createQuery(query).getSingleResultOrNull();
        return entity != null ? toScheduledStep(entity) : null;
    }

    @Override
    public void scheduleStep(Workflow workflow, WorkflowStep step, String resourceId, String executionId) {
        WorkflowStateEntity entity = em.find(WorkflowStateEntity.class, executionId);
        if (entity == null) {
            entity = new WorkflowStateEntity();
            entity.setResourceId(resourceId);
            entity.setWorkflowId(workflow.getId());
            entity.setExecutionId(executionId);
            entity.setWorkflowProviderId(workflow.getProviderId());
            entity.setScheduledStepId(step.getId());
            entity.setScheduledStepTimestamp(Time.currentTimeMillis() + step.getAfter());
            em.persist(entity);
        } else {
            entity.setScheduledStepId(step.getId());
            entity.setScheduledStepTimestamp(Time.currentTimeMillis() + step.getAfter());
        }
    }

    @Override
    public List<ScheduledStep> getDueScheduledSteps(Workflow workflow) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowStateEntity> query = cb.createQuery(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> stateRoot = query.from(WorkflowStateEntity.class);

        Predicate byWorkflow = cb.equal(stateRoot.get("workflowId"), workflow.getId());
        Predicate isExpired = cb.lessThan(stateRoot.get("scheduledStepTimestamp"), Time.currentTimeMillis());

        query.where(cb.and(byWorkflow, isExpired));

        return em.createQuery(query).getResultStream()
                .map(this::toScheduledStep)
                .toList();
    }

    @Override
    public List<ScheduledStep> getScheduledStepsByWorkflow(String workflowId) {
        if (StringUtil.isBlank(workflowId)) {
            return List.of();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowStateEntity> query = cb.createQuery(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> stateRoot = query.from(WorkflowStateEntity.class);

        Predicate byWorkflow = cb.equal(stateRoot.get("workflowId"), workflowId);
        query.where(byWorkflow);

        return em.createQuery(query).getResultStream()
                .map(this::toScheduledStep)
                .toList();
    }

    @Override
    public List<ScheduledStep> getScheduledStepsByResource(String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowStateEntity> query = cb.createQuery(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> stateRoot = query.from(WorkflowStateEntity.class);

        Predicate byResource = cb.equal(stateRoot.get("resourceId"), resourceId);
        query.where(byResource);

        return em.createQuery(query).getResultStream()
                .map(this::toScheduledStep)
                .toList();
    }

    public List<ScheduledStep> getScheduledStepsByStep(String stepId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<WorkflowStateEntity> query = cb.createQuery(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> stateRoot = query.from(WorkflowStateEntity.class);

        Predicate byStep = cb.equal(stateRoot.get("scheduledStepId"), stepId);
        query.where(byStep);

        return em.createQuery(query).getResultStream()
                .map(this::toScheduledStep)
                .toList();
    }

    @Override
    public void removeByResource(String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowStateEntity> delete = cb.createCriteriaDelete(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> root = delete.from(WorkflowStateEntity.class);
        delete.where(cb.equal(root.get("resourceId"), resourceId));
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                LOGGER.tracev("Deleted {0} orphaned state records for resource {1}", deletedCount, resourceId);
            }
        }
    }

    @Override
    public void removeByWorkflow(String workflowId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowStateEntity> delete = cb.createCriteriaDelete(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> root = delete.from(WorkflowStateEntity.class);
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
    public void remove(String executionId) {
        WorkflowStateEntity entity = em.find(WorkflowStateEntity.class, executionId);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public void removeAll() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowStateEntity> delete = cb.createCriteriaDelete(WorkflowStateEntity.class);
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

    private ScheduledStep toScheduledStep(WorkflowStateEntity entity) {
        return new ScheduledStep(entity.getWorkflowId(), entity.getScheduledStepId(), entity.getResourceId(), entity.getExecutionId());
    }
}
