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

import java.util.Objects;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapComponentEntity implements UpdatableEntity {

    private String id;
    private String name;
    private String providerId;
    private String providerType;
    private String subType;
    private String parentId;
    private MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();

    private boolean updated;

    private MapComponentEntity() {}

    public static MapComponentEntity fromModel(ComponentModel model) {
        if (model == null) return null;
        MapComponentEntity entity = new MapComponentEntity();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setProviderId(model.getProviderId());
        entity.setProviderType(model.getProviderType());
        entity.setSubType(model.getSubType());
        entity.setParentId(model.getParentId());
        entity.setConfig(model.getConfig() == null ? null : new MultivaluedHashMap<>(model.getConfig()));
        return entity;
    }

    public static ComponentModel toModel(MapComponentEntity entity) {
        if (entity == null) return null;
        ComponentModel model = new ComponentModel();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setProviderId(entity.getProviderId());
        model.setProviderType(entity.getProviderType());
        model.setSubType(entity.getSubType());
        model.setParentId(entity.getParentId());
        model.setConfig(entity.getConfig() == null ? null : new MultivaluedHashMap<>(entity.getConfig()));
        return model;
    }

    @Override
    public boolean isUpdated() {
        return updated;
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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.updated = !Objects.equals(this.providerId, providerId);
        this.providerId = providerId;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.updated = !Objects.equals(this.providerType, providerType);
        this.providerType = providerType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.updated = !Objects.equals(this.subType, subType);
        this.subType = subType;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.updated = !Objects.equals(this.parentId, parentId);
        this.parentId = parentId;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
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
        if (!(obj instanceof MapComponentEntity)) return false;
        final MapComponentEntity other = (MapComponentEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
