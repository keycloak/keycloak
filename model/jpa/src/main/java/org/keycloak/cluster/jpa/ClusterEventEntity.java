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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.keycloak.connections.jpa.AsynchronousCommitAllowed;

@Entity
@Table(name = "CLUSTER_EVENT")
@NamedQueries({
        @NamedQuery(name = "clusterEvent.readByTargetCluster",
                query = "SELECT e.id, e.eventData FROM ClusterEventEntity e WHERE e.targetCluster = :target ORDER BY e.createdAt"),
        @NamedQuery(name = "clusterEvent.deleteByIds",
                query = "DELETE FROM ClusterEventEntity e WHERE e.id IN :ids AND e.targetCluster = :targetCluster"),
        @NamedQuery(name = "clusterEvent.deleteOlderThan",
                query = "DELETE FROM ClusterEventEntity e WHERE e.createdAt < :timestamp"),
        @NamedQuery(name = "clusterEvent.eventWithIdExists",
                query = "SELECT 1 FROM ClusterEventEntity e WHERE e.id = :id")
})
@IdClass(ClusterEventKey.class)
public class ClusterEventEntity implements AsynchronousCommitAllowed {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Id
    @Column(name = "TARGET_CLUSTER", nullable = false, length = 200)
    private String targetCluster;

    @Column(name = "SENDER_CLUSTER", nullable = false, length = 200)
    private String senderCluster;

    @Lob
    @Column(name = "EVENT_DATA", nullable = false)
    private byte[] eventData;

    @Column(name = "CREATED_AT", nullable = false)
    private long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(String targetCluster) {
        this.targetCluster = targetCluster;
    }

    public String getSenderCluster() {
        return senderCluster;
    }

    public void setSenderCluster(String senderCluster) {
        this.senderCluster = senderCluster;
    }

    public byte[] getEventData() {
        return eventData;
    }

    public void setEventData(byte[] eventData) {
        this.eventData = eventData;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
