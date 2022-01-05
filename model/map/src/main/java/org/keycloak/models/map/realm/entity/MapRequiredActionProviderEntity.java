/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.realm.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapRequiredActionProviderEntity extends UpdatableEntity.Impl {

    private String id;
    private String alias;
    private String name;
    private String providerId;
    private Integer priority = 0;
    private Boolean enabled = false;
    private Boolean defaultAction = false;
    private Map<String, String> config = new HashMap<>();


    private MapRequiredActionProviderEntity() {}

    public static MapRequiredActionProviderEntity fromModel(RequiredActionProviderModel model) {
        if (model == null) return null;
        MapRequiredActionProviderEntity entity = new MapRequiredActionProviderEntity();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setName(model.getName());
        entity.setProviderId(model.getProviderId());
        entity.setPriority(model.getPriority());
        entity.setEnabled(model.isEnabled());
        entity.setDefaultAction(model.isDefaultAction());
        entity.setConfig(model.getConfig() == null ? null : new HashMap<>(model.getConfig()));
        return entity;
    }

    public static RequiredActionProviderModel toModel(MapRequiredActionProviderEntity entity) {
        if (entity == null) return null;
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setName(entity.getName());
        model.setProviderId(entity.getProviderId());
        model.setPriority(entity.getPriority());
        model.setEnabled(entity.isEnabled());
        model.setDefaultAction(entity.isDefaultAction());
        model.setConfig(entity.getConfig() == null ? null : new HashMap<>(entity.getConfig()));
        return model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.updated = !Objects.equals(this.id, id);
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.updated = !Objects.equals(this.alias, alias);
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated = !Objects.equals(this.name, name);
        this.name = name;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.updated = !Objects.equals(this.providerId, providerId);
        this.providerId = providerId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.updated = !Objects.equals(this.priority, priority);
        this.priority = priority;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.updated = !Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    public Boolean isDefaultAction() {
        return defaultAction;
    }

    public void setDefaultAction(boolean defaultAction) {
        this.updated = !Objects.equals(this.defaultAction, defaultAction);
        this.defaultAction = defaultAction;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.updated = !Objects.equals(this.config, config);
        this.config = config;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapRequiredActionProviderEntity)) return false;
        final MapRequiredActionProviderEntity other = (MapRequiredActionProviderEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
