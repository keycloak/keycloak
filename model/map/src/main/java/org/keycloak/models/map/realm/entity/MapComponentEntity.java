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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.List;
import java.util.Map;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapComponentEntity extends UpdatableEntity, AbstractEntity {
    static MapComponentEntity fromModel(ComponentModel model) {
        MapComponentEntity entity = new MapComponentEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setProviderId(model.getProviderId());
        entity.setProviderType(model.getProviderType());
        entity.setSubType(model.getSubType());
        entity.setParentId(model.getParentId());
        entity.setConfig(model.getConfig());
        return entity;
    }

    static ComponentModel toModel(MapComponentEntity entity) {
        ComponentModel model = new ComponentModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setProviderId(entity.getProviderId());
        model.setProviderType(entity.getProviderType());
        model.setSubType(entity.getSubType());
        model.setParentId(entity.getParentId());
        Map<String, List<String>> config = entity.getConfig();
        model.setConfig(config == null ? new MultivaluedHashMap<>() : new MultivaluedHashMap<>(config));
        return model;
    }

    String getName();
    void setName(String name);

    String getProviderId();
    void setProviderId(String providerId);

    String getProviderType();
    void setProviderType(String providerType);

    String getSubType();
    void setSubType(String subType);

    String getParentId();
    void setParentId(String parentId);

    Map<String, List<String>> getConfig();
    void setConfig(Map<String, List<String>> config);
}
