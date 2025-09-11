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

public class JpaResourcePolicyStateProvider implements ResourcePolicyStateProvider {

    private final EntityManager em;
    private static final Logger LOGGER = Logger.getLogger(JpaResourcePolicyStateProvider.class);
    private final KeycloakSession session;

    public JpaResourcePolicyStateProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public ScheduledAction getScheduledAction(String policyId, String resourceId) {
        ResourcePolicyStateEntity.PrimaryKey pk = new ResourcePolicyStateEntity.PrimaryKey(resourceId, policyId);
        ResourcePolicyStateEntity entity = em.find(ResourcePolicyStateEntity.class, pk);
        if (entity != null) {
            return new ScheduledAction(entity.getPolicyId(), entity.getScheduledActionId(), entity.getResourceId());
        }
        return null;
    }

    @Override
    public void scheduleAction(ResourcePolicy policy, ResourceAction action, long scheduledTimeOffset, String resourceId) {
        ResourcePolicyStateEntity.PrimaryKey pk = new ResourcePolicyStateEntity.PrimaryKey(resourceId, policy.getId());
        ResourcePolicyStateEntity entity = em.find(ResourcePolicyStateEntity.class, pk);
        if (entity == null) {
            entity = new ResourcePolicyStateEntity();
            entity.setResourceId(resourceId);
            entity.setPolicyId(policy.getId());
            entity.setPolicyProviderId(policy.getProviderId());
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
    public List<ScheduledAction> getDueScheduledActions(ResourcePolicy policy) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ResourcePolicyStateEntity> query = cb.createQuery(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> stateRoot = query.from(ResourcePolicyStateEntity.class);

        Predicate byPolicy = cb.equal(stateRoot.get("policyId"), policy.getId());
        Predicate isExpired = cb.lessThan(stateRoot.get("scheduledActionTimestamp"), Time.currentTimeMillis());

        query.where(cb.and(byPolicy, isExpired));

        return em.createQuery(query).getResultStream()
                .map(s -> new ScheduledAction(s.getPolicyId(), s.getScheduledActionId(), s.getResourceId()))
                .toList();
    }

    @Override
    public List<ScheduledAction> getScheduledActionsByPolicy(String id) {
        if (StringUtil.isBlank(id)) {
            return List.of();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ResourcePolicyStateEntity> query = cb.createQuery(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> stateRoot = query.from(ResourcePolicyStateEntity.class);

        Predicate byPolicy = cb.equal(stateRoot.get("policyId"), id);
        query.where(byPolicy);

        return em.createQuery(query).getResultStream()
                .map(s -> new ScheduledAction(s.getPolicyId(), s.getScheduledActionId(), s.getResourceId()))
                .toList();
    }

    @Override
    public List<ScheduledAction> getScheduledActionsByResource(String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ResourcePolicyStateEntity> query = cb.createQuery(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> stateRoot = query.from(ResourcePolicyStateEntity.class);

        Predicate byResource = cb.equal(stateRoot.get("resourceId"), resourceId);
        query.where(byResource);

        return em.createQuery(query).getResultStream()
                .map(s -> new ScheduledAction(s.getPolicyId(), s.getScheduledActionId(), s.getResourceId()))
                .toList();
    }

    @Override
    public void removeByResource(String resourceId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<ResourcePolicyStateEntity> delete = cb.createCriteriaDelete(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> root = delete.from(ResourcePolicyStateEntity.class);
        delete.where(cb.equal(root.get("resourceId"), resourceId));
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                LOGGER.tracev("Deleted {0} orphaned state records for resource {1}", deletedCount, resourceId);
            }
        }
    }

    @Override
    public void remove(String policyId, String resourceId) {
        ResourcePolicyStateEntity.PrimaryKey pk = new ResourcePolicyStateEntity.PrimaryKey(resourceId, policyId);
        ResourcePolicyStateEntity entity = em.find(ResourcePolicyStateEntity.class, pk);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public void remove(String policyId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<ResourcePolicyStateEntity> delete = cb.createCriteriaDelete(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> root = delete.from(ResourcePolicyStateEntity.class);
        delete.where(cb.equal(root.get("policyId"), policyId));
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
        CriteriaDelete<ResourcePolicyStateEntity> delete = cb.createCriteriaDelete(ResourcePolicyStateEntity.class);
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
