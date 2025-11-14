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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.common.util.DurationConverter;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

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
        Duration duration = DurationConverter.parseDuration(step.getAfter());
        if (duration == null) {
            // shouldn't happen as the step duration should have been validated before
            throw new IllegalArgumentException("Invalid duration (%s) found when scheduling step %s in workflow %s"
                    .formatted(step.getAfter(), step.getProviderId(), workflow.getName()));
        }

        if (entity == null) {
            entity = new WorkflowStateEntity();
            entity.setResourceId(resourceId);
            entity.setWorkflowId(workflow.getId());
            entity.setExecutionId(executionId);
            entity.setScheduledStepId(step.getId());
            entity.setScheduledStepTimestamp(Instant.now().plus(duration).toEpochMilli());
            em.persist(entity);
        } else {
            entity.setScheduledStepId(step.getId());
            entity.setScheduledStepTimestamp(Instant.now().plus(duration).toEpochMilli());
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
    public void removeByWorkflowAndResource(String workflowId, String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<WorkflowStateEntity> delete = cb.createCriteriaDelete(WorkflowStateEntity.class);
        Root<WorkflowStateEntity> root = delete.from(WorkflowStateEntity.class);
        delete.where(cb.and(cb.equal(root.get("workflowId"), workflowId),  cb.equal(root.get("resourceId"), resourceId)));
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                LOGGER.tracev("Deleted {0} state records for resource {1} of workflow {2}", deletedCount, resourceId, workflowId);
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
    public boolean hasScheduledSteps(String workflowId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);
        Root<WorkflowStateEntity> stateRoot = criteriaQuery.from(WorkflowStateEntity.class);

        criteriaQuery.select(cb.count(stateRoot));
        criteriaQuery.where(cb.equal(stateRoot.get("workflowId"), workflowId));

        TypedQuery<Long> query = em.createQuery(criteriaQuery);
        query.setMaxResults(1);

        Long count = query.getSingleResult();
        return count > 0;
    }

    @Override
    public void close() {
    }

    private ScheduledStep toScheduledStep(WorkflowStateEntity entity) {
        return new ScheduledStep(entity.getWorkflowId(), entity.getScheduledStepId(), entity.getResourceId(), entity.getExecutionId());
    }
}
