/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapRequiredActionProviderEntity extends UpdatableEntity, AbstractEntity {
    static MapRequiredActionProviderEntity fromModel(RequiredActionProviderModel model) {
        if (model == null) return null;
        MapRequiredActionProviderEntity entity = new MapRequiredActionProviderEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setName(model.getName());
        entity.setProviderId(model.getProviderId());
        entity.setPriority(model.getPriority());
        entity.setEnabled(model.isEnabled());
        entity.setDefaultAction(model.isDefaultAction());
        entity.setConfig(model.getConfig());
        return entity;
    }

    static RequiredActionProviderModel toModel(MapRequiredActionProviderEntity entity) {
        if (entity == null) return null;
        RequiredActionProviderModel model = new RequiredActionProviderModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setName(entity.getName());
        model.setProviderId(entity.getProviderId());
        Integer priority = entity.getPriority();
        model.setPriority(priority == null ? 0 : priority);
        Boolean enabled = entity.isEnabled();
        model.setEnabled(enabled == null ? false : enabled);
        Boolean defaultAction = entity.isDefaultAction();
        model.setDefaultAction(defaultAction == null ? false : defaultAction);
        Map<String, String> config = entity.getConfig();
        model.setConfig(config == null ? new HashMap<>() : new HashMap<>(config));
        return model;
    }

    String getAlias();
    void setAlias(String alias);

    String getName();
    void setName(String name);

    String getProviderId();
    void setProviderId(String providerId);

    Integer getPriority();
    void setPriority(Integer priority);

    Boolean isEnabled();
    void setEnabled(Boolean enabled);

    Boolean isDefaultAction();
    void setDefaultAction(Boolean defaultAction);

    Map<String, String> getConfig();
    void setConfig(Map<String, String> config);
}
