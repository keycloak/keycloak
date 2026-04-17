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
import jakarta.persistence.Table;

@Entity
@Table(name = "EVENT_HOOK_LOG")
public class EventHookLogEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "EXECUTION_ID", length = 36, nullable = false)
    private String executionId;

    @Column(name = "BATCH_EXECUTION", nullable = false)
    private boolean batchExecution;

    @Column(name = "MESSAGE_ID", length = 36, nullable = false)
    private String messageId;

    @Column(name = "TARGET_ID", length = 36, nullable = false)
    private String targetId;

    @Column(name = "STATUS", length = 16, nullable = false)
    private String status;

    @Column(name = "ATTEMPT_NUMBER", nullable = false)
    private int attemptNumber;

    @Column(name = "STATUS_CODE", length = 255)
    private String statusCode;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "DETAILS", length = 2048)
    private String details;

    @Column(name = "CREATED_AT", nullable = false)
    private long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public boolean isBatchExecution() {
        return batchExecution;
    }

    public void setBatchExecution(boolean batchExecution) {
        this.batchExecution = batchExecution;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
