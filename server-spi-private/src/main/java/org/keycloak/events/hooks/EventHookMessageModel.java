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

public class EventHookMessageModel {

    private String id;
    private String realmId;
    private String targetId;
    private EventHookSourceType sourceType;
    private String sourceEventId;
    private EventHookMessageStatus status;
    private String payload;
    private int attemptCount;
    private long nextAttemptAt;
    private long createdAt;
    private long updatedAt;
    private String claimOwner;
    private Long claimedAt;
    private String lastError;

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

    public String getClaimOwner() {
        return claimOwner;
    }

    public void setClaimOwner(String claimOwner) {
        this.claimOwner = claimOwner;
    }

    public Long getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Long claimedAt) {
        this.claimedAt = claimedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
