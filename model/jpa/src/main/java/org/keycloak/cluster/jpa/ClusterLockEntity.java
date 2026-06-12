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
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "CLUSTER_LOCK")
@NamedQueries({
        @NamedQuery(name = "ClusterLockEntity.insertIfAbsent",
                query = "INSERT INTO ClusterLockEntity (taskKey, expiresAt) VALUES (:taskKey, :expiresAt)"
                        + " ON CONFLICT DO NOTHING"),
        @NamedQuery(name = "ClusterLockEntity.tryLock",
                query = "SELECT l FROM ClusterLockEntity l WHERE l.taskKey = :taskKey"),
        @NamedQuery(name = "ClusterLockEntity.selectExpired",
                query = "SELECT l FROM ClusterLockEntity l WHERE l.expiresAt < :now")
})
public class ClusterLockEntity {

    @Id
    @Column(name = "TASK_KEY", length = 255)
    private String taskKey;

    @Column(name = "EXPIRES_AT", nullable = false)
    private long expiresAt;

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
