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

import java.util.Map;

import org.keycloak.util.JsonSerialization;

public class EventHookMessageModel {

    private String id;
    private String realmId;
    private String targetId;
    private String executionId;
    private EventHookSourceType sourceType;
    private String sourceEventId;
    private String sourceEventName;
    private String userId;
    private String resourcePath;
    private EventHookMessageStatus status;
    private String payload;
    private int attemptCount;
    private long nextAttemptAt;
    private Long executionStartedAt;
    private long createdAt;
    private long updatedAt;
    private String lastError;
    private boolean executionBatch;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public EventHookMessageStatus getStatus() {
        return status;
    }

    public void setStatus(EventHookMessageStatus status) {
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
        if (test) {
            return true;
        }

        if (payload == null || payload.isBlank()) {
            return false;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedPayload = JsonSerialization.readValue(payload, Map.class);
            return Boolean.TRUE.equals(parsedPayload.get("deliveryTest"));
        } catch (Exception exception) {
            return false;
        }
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
