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

public class EventHookTargetModel {

    private String id;
    private String realmId;
    private String realmName;
    private String name;
    private String type;
    private boolean enabled;
    private long createdAt;
    private long updatedAt;
    private Long autoDisabledUntil;
    private String autoDisabledReason;
    private Integer consecutive429Count;
    private Map<String, Object> settings;

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

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public Long getAutoDisabledUntil() {
        return autoDisabledUntil;
    }

    public void setAutoDisabledUntil(Long autoDisabledUntil) {
        this.autoDisabledUntil = autoDisabledUntil;
    }

    public String getAutoDisabledReason() {
        return autoDisabledReason;
    }

    public void setAutoDisabledReason(String autoDisabledReason) {
        this.autoDisabledReason = autoDisabledReason;
    }

    public Integer getConsecutive429Count() {
        return consecutive429Count;
    }

    public void setConsecutive429Count(Integer consecutive429Count) {
        this.consecutive429Count = consecutive429Count;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
}
