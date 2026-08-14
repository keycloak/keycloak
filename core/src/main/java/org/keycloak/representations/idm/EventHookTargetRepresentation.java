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

package org.keycloak.representations.idm;

import java.util.Map;

public class EventHookTargetRepresentation {

    private String id;
    private String name;
    private String type;
    private Boolean enabled;
    private Long createdAt;
    private Long updatedAt;
    private Map<String, Object> settings;
    private String displayInfo;
    private String status;
    private Boolean autoDisabled;
    private Long autoDisabledUntil;
    private String autoDisabledReason;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public String getDisplayInfo() {
        return displayInfo;
    }

    public void setDisplayInfo(String displayInfo) {
        this.displayInfo = displayInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAutoDisabled() {
        return autoDisabled;
    }

    public void setAutoDisabled(Boolean autoDisabled) {
        this.autoDisabled = autoDisabled;
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
}
