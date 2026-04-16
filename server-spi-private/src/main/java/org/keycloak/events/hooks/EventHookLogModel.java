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

package org.keycloak.events.hooks;

public class EventHookLogModel {

    private String id;
    private String executionId;
    private boolean batchExecution;
    private String messageId;
    private String targetId;
    private EventHookSourceType sourceType;
    private String sourceEventId;
    private String sourceEventName;
    private EventHookLogStatus status;
    private EventHookMessageStatus messageStatus;
    private int attemptNumber;
    private String statusCode;
    private Long durationMs;
    private String details;
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

    public EventHookSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(EventHookSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getSourceEventName() {
        return sourceEventName;
    }

    public void setSourceEventName(String sourceEventName) {
        this.sourceEventName = sourceEventName;
    }

    public EventHookLogStatus getStatus() {
        return status;
    }

    public void setStatus(EventHookLogStatus status) {
        this.status = status;
    }

    public EventHookMessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(EventHookMessageStatus messageStatus) {
        this.messageStatus = messageStatus;
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
