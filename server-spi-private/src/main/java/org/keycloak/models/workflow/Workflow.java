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

package org.keycloak.models.workflow;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ERROR;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RECURRING;

import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;

public class Workflow {

    private MultivaluedHashMap<String, String> config;
    private final String providerId;
    private String id;
    private Long notBefore;

    public Workflow(ComponentModel c) {
        this.id = c.getId();
        this.providerId = c.getProviderId();
        this.config = c.getConfig();
    }

    public Workflow(String providerId, Map<String, List<String>> config) {
        this.providerId = providerId;
        MultivaluedHashMap<String, String> c = new MultivaluedHashMap<>();
        config.forEach(c::addAll);
        this.config = c;
    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public String getName() {
        return config != null ? config.getFirst(CONFIG_NAME) : null;
    }

    public boolean isEnabled() {
        return config != null && Boolean.parseBoolean(config.getFirstOrDefault(CONFIG_ENABLED, "true"));
    }

    public boolean isRecurring() {
        return config != null && Boolean.parseBoolean(config.getFirst(CONFIG_RECURRING));
    }

    public Long getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Long notBefore) {
        this.notBefore = notBefore;
    }

    public void setEnabled(boolean enabled) {
        if (config == null) {
            config = new MultivaluedHashMap<>();
        }
        config.putSingle(CONFIG_ENABLED, String.valueOf(enabled));
    }

    public void setError(String message) {
        if (config == null) {
            config = new MultivaluedHashMap<>();
        }
        config.putSingle(CONFIG_ERROR, message);
    }
}
