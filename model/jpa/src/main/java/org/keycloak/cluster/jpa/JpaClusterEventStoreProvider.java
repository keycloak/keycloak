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

package org.keycloak.cluster.jpa;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.keycloak.cluster.StoredClusterEvent;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

public class JpaClusterEventStoreProvider {

    private static final Logger logger = Logger.getLogger(JpaClusterEventStoreProvider.class);

    /* KEYCLOAK_JDBC_PING will update the table periodically, see staleness_timeout in that class.
       To ignore entries for clusters that shut down unexpectedly or are no longer connected to the database,
       ignore those stale entries.
     */
    private static final int STALENESS_CUTOFF_SECONDS = 60;

    private final KeycloakSession session;

    public JpaClusterEventStoreProvider(KeycloakSession session) {
        this.session = session;
    }

    public String getPrimaryClusterName() {
        EntityManager em = getEntityManager();
        String tableName = JpaUtils.getTableNameForNativeQuery("JGROUPS_PING", em);

        return (String) em.createNativeQuery(
                        "SELECT min(cluster_name) FROM " + tableName + " WHERE last_update > ?1", String.class)
                .setParameter(1, Time.currentTime() - STALENESS_CUTOFF_SECONDS)
                .getSingleResult();
    }

    public String persist(String senderCluster, byte[] eventData) {
        EntityManager em = getEntityManager();
        String tableName = JpaUtils.getTableNameForNativeQuery("JGROUPS_PING", em);

        @SuppressWarnings("unchecked")
        List<String> targetClusters = em.createNativeQuery(
                        "SELECT DISTINCT cluster_name FROM " + tableName + " WHERE cluster_name != ?1 AND last_update > ?2", String.class)
                .setParameter(1, senderCluster)
                .setParameter(2, Time.currentTime() - STALENESS_CUTOFF_SECONDS)
                .getResultList();

        if (targetClusters.isEmpty()) {
            return null;
        }

        long now = Time.currentTimeMillis();
        String id = SecretGenerator.getInstance().generateSecureID();
        for (String target : targetClusters) {
            ClusterEventEntity entity = new ClusterEventEntity();
            entity.setId(id);
            entity.setTargetCluster(target);
            entity.setSenderCluster(senderCluster);
            entity.setEventData(eventData);
            entity.setCreatedAt(now);
            em.persist(entity);
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Persisted cluster event for %d target cluster(s): %s", targetClusters.size(), targetClusters);
        }

        return id;
    }

    public List<StoredClusterEvent> readEvents(String targetCluster, int maxResults) {
        return getEntityManager().createNamedQuery("clusterEvent.readByTargetCluster", StoredClusterEvent.class)
                .setParameter("target", targetCluster)
                .setMaxResults(maxResults)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    public void deleteEvents(String targetCluster, Collection<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        getEntityManager().createNamedQuery("clusterEvent.deleteByIds")
                .setParameter("ids", ids)
                .setParameter("targetCluster", targetCluster)
                .executeUpdate();
    }

    public void deleteEventsOlderThan(long timestampMillis) {
        int deleted = getEntityManager().createNamedQuery("clusterEvent.deleteOlderThan")
                .setParameter("timestamp", timestampMillis)
                .executeUpdate();
        if (deleted > 0) {
            logger.debugf("Cleaned up %d stale cluster event(s)", deleted);
        }
    }

    public boolean eventExists(String id) {
        return !getEntityManager().createNamedQuery("clusterEvent.eventWithIdExists", Integer.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList().isEmpty();
    }

    public void close() {
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    public boolean isUsingJdbcPing(String cluster, String node) {
        EntityManager em = getEntityManager();
        String tableName = JpaUtils.getTableNameForNativeQuery("JGROUPS_PING", em);

        return !em.createNativeQuery(
                        "SELECT 1 FROM " + tableName + " WHERE last_update > ?1 AND cluster_name = ?2 AND name = ?3")
                .setParameter(1, Time.currentTime() - STALENESS_CUTOFF_SECONDS)
                .setParameter(2, cluster)
                .setParameter(3, node)
                .setMaxResults(1)
                .getResultList().isEmpty();
    }
}
