/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "EVENT_HOOK_MESSAGE")
public class EventHookMessageEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "REALM_ID", length = 36, nullable = false)
    private String realmId;

    @Column(name = "TARGET_ID", length = 36, nullable = false)
    private String targetId;

    @Column(name = "EXECUTION_ID", length = 36)
    private String executionId;

    @Column(name = "SOURCE_TYPE", length = 16, nullable = false)
    private String sourceType;

    @Column(name = "SOURCE_EVENT_ID", length = 255)
    private String sourceEventId;

    @Column(name = "STATUS", length = 32, nullable = false)
    private String status;

    @Lob
    @Column(name = "PAYLOAD_JSON")
    private String payload;

    @Column(name = "ATTEMPT_COUNT", nullable = false)
    private int attemptCount;

    @Column(name = "NEXT_ATTEMPT_AT", nullable = false)
    private long nextAttemptAt;

    @Column(name = "EXECUTION_STARTED_AT")
    private Long executionStartedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private long createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private long updatedAt;

    @Column(name = "LAST_ERROR", length = 2048)
    private String lastError;

    @Column(name = "EXECUTION_BATCH", nullable = false)
    private boolean executionBatch;

    @Column(name = "IS_TEST", nullable = false)
    private boolean test;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public long getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(long nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Long getExecutionStartedAt() {
        return executionStartedAt;
    }

    public void setExecutionStartedAt(Long executionStartedAt) {
        this.executionStartedAt = executionStartedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean isExecutionBatch() {
        return executionBatch;
    }

    public void setExecutionBatch(boolean executionBatch) {
        this.executionBatch = executionBatch;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
