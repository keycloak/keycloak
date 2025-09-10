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

package org.keycloak.models.policy;

import java.util.List;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;

public class ResourceAction implements Comparable<ResourceAction> {

    public static final String AFTER_KEY = "after";
    public static final String PRIORITY_KEY = "priority";

    private String id;
    private String providerId;
    private MultivaluedHashMap<String, String> config;
    private List<ResourceAction> actions = List.of();

    public ResourceAction() {
        // reflection
    }

    public ResourceAction(String providerId) {
        this.providerId = providerId;
    }

    public ResourceAction(String providerId, MultivaluedHashMap<String, String> config, List<ResourceAction> actions) {
        this.providerId = providerId;
        this.config = config;
        this.actions = actions;
    }

    public ResourceAction(ComponentModel model) {
        this.id = model.getId();
        this.providerId = model.getProviderId();
        this.config = model.getConfig();
    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public ResourceAction setConfig(String key, String value) {
        if (config == null) {
            config = new MultivaluedHashMap<>();
        }
        this.config.putSingle(key, value);
        return this;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        if (config == null) {
            return new MultivaluedHashMap<>();
        }
        return config;
    }

    public void setPriority(int priority) {
        setConfig(PRIORITY_KEY, String.valueOf(priority));
    }

    // todo, do not expose the priority to user?? in export etc. the order of actions should define the priority
    public int getPriority() {
        String value = getConfig().getFirst(PRIORITY_KEY);
        if (value == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    public void setAfter(Long ms) {
        setConfig(AFTER_KEY, String.valueOf(ms));
    }

    public Long getAfter() {
        return Long.valueOf(getConfig().getFirstOrDefault(AFTER_KEY, "0"));
    }

    public List<ResourceAction> getActions() {
        if (actions == null) {
            return List.of();
        }
        return actions;
    }

    public void setActions(List<ResourceAction> actions) {
        this.actions = actions;
    }

    @Override
    public int compareTo(ResourceAction other) {
        return Integer.compare(this.getPriority(), other.getPriority());
    }
}
