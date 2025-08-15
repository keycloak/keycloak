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

import java.util.List;
import java.util.Set;

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
import org.keycloak.models.UserModel;

public class JpaResourcePolicyStateProvider implements ResourcePolicyStateProvider {

    private final EntityManager em;
    private static final Logger LOGGER = Logger.getLogger(JpaResourcePolicyStateProvider.class);
    private final KeycloakSession session;

    public JpaResourcePolicyStateProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public List<String> findResourceIdsByLastCompletedAction(String policyId, String lastCompletedActionId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<ResourcePolicyStateEntity> stateRoot = query.from(ResourcePolicyStateEntity.class);

        Predicate policyPredicate = cb.equal(stateRoot.get("policyId"), policyId);
        Predicate actionPredicate = cb.equal(stateRoot.get("lastCompletedActionId"), lastCompletedActionId);

        query.select(stateRoot.get("resourceId"));
        query.where(cb.and(policyPredicate, actionPredicate));

        return em.createQuery(query).getResultList();
    }

    @Override
    public void update(String policyId, String policyProviderId, List<String> resourceIds, String newLastCompletedActionId) {
        for (String resourceId : resourceIds) {
            ResourcePolicyStateEntity.PrimaryKey pk = new ResourcePolicyStateEntity.PrimaryKey(resourceId, policyId);
            ResourcePolicyStateEntity entity = em.find(ResourcePolicyStateEntity.class, pk);

            if (entity == null) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.tracev("Initial record for policyId ({0}), new_last_compl_actionId ({1}), userId ({2})", policyId, newLastCompletedActionId, resourceId);
                }
                entity = new ResourcePolicyStateEntity();
                entity.setResourceId(resourceId);
                entity.setPolicyId(policyId);
                entity.setPolicyProviderId(policyProviderId);
                em.persist(entity);
            } else {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.tracev("Changing record for policyId ({0}), last_compl_actionId ({1}), new_last_compl_actionId ({2}), userId ({3})",
                            entity.getPolicyId(), entity.getLastCompletedActionId(), newLastCompletedActionId, resourceId);
                }
            }

            entity.setLastCompletedActionId(newLastCompletedActionId);
            entity.setLastUpdatedTimestamp(Time.currentTimeMillis());
        }
    }

    @Override
    public void removeByCompletedActions(String policyId, Set<String> deletedActionIds) {
        if (deletedActionIds == null || deletedActionIds.isEmpty()) {
            return;
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<ResourcePolicyStateEntity> delete = cb.createCriteriaDelete(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> stateRoot = delete.from(ResourcePolicyStateEntity.class);

        Predicate policyPredicate = cb.equal(stateRoot.get("policyId"), policyId);
        Predicate inClausePredicate = stateRoot.get("lastCompletedActionId").in(deletedActionIds);

        delete.where(cb.and(policyPredicate, inClausePredicate));

        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                LOGGER.tracev("Deleted {0} orphaned state records for policy {1}", deletedCount, policyId);
            }
        }
    }

    @Override
    public void removeByUser(UserModel user) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete<ResourcePolicyStateEntity> delete = cb.createCriteriaDelete(ResourcePolicyStateEntity.class);
        Root<ResourcePolicyStateEntity> root = delete.from(ResourcePolicyStateEntity.class);
        delete.where(cb.equal(root.get("resourceId"), user.getId()));
        int deletedCount = em.createQuery(delete).executeUpdate();

        if (LOGGER.isTraceEnabled()) {
            if (deletedCount > 0) {
                LOGGER.tracev("Deleted {0} orphaned state records for user {1}", deletedCount, user.getId());
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
