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
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapIdentityProviderMapperEntity extends UpdatableEntity.Impl {

    private String id;
    private String name;
    private String identityProviderAlias;
    private String identityProviderMapper;
    private Map<String, String> config = new HashMap<>();


    private MapIdentityProviderMapperEntity() {}

    public static MapIdentityProviderMapperEntity fromModel(IdentityProviderMapperModel model) {
        if (model == null) return null;
        MapIdentityProviderMapperEntity entity = new MapIdentityProviderMapperEntity();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setConfig(model.getConfig() == null ? null : new HashMap<>(model.getConfig()));
        return entity;
    }

    public static IdentityProviderMapperModel toModel(MapIdentityProviderMapperEntity entity) {
        if (entity == null) return null;
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        model.setIdentityProviderMapper(entity.getIdentityProviderMapper());
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated = !Objects.equals(this.name, name);
        this.name = name;
    }

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.updated = !Objects.equals(this.identityProviderAlias, identityProviderAlias);
        this.identityProviderAlias = identityProviderAlias;
    }

    public String getIdentityProviderMapper() {
        return identityProviderMapper;
    }

    public void setIdentityProviderMapper(String identityProviderMapper) {
        this.updated = !Objects.equals(this.identityProviderMapper, identityProviderMapper);
        this.identityProviderMapper = identityProviderMapper;
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
        if (!(obj instanceof MapIdentityProviderMapperEntity)) return false;
        final MapIdentityProviderMapperEntity other = (MapIdentityProviderMapperEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
