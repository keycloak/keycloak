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

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapIdentityProviderMapperEntity extends UpdatableEntity, AbstractEntity {
    static MapIdentityProviderMapperEntity fromModel(IdentityProviderMapperModel model) {
        if (model == null) return null;
        MapIdentityProviderMapperEntity entity = new MapIdentityProviderMapperEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setConfig(model.getConfig());
        return entity;
    }

    static IdentityProviderMapperModel toModel(MapIdentityProviderMapperEntity entity) {
        if (entity == null) return null;
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        model.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = entity.getConfig();
        model.setConfig(config == null ? new HashMap<>() : new HashMap<>(config));
        return model;
    }

    String getName();
    void setName(String name);

    String getIdentityProviderAlias();
    void setIdentityProviderAlias(String identityProviderAlias);

    String getIdentityProviderMapper();
    void setIdentityProviderMapper(String identityProviderMapper);

    Map<String, String> getConfig();
    void setConfig(Map<String, String> config);
}
