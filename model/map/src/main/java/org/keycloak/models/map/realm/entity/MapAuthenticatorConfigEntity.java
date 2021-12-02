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
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapAuthenticatorConfigEntity extends UpdatableEntity.Impl {

    private String id;
    private String alias;
    private Map<String, String> config = new HashMap<>();


    private MapAuthenticatorConfigEntity() {}

    public static MapAuthenticatorConfigEntity fromModel(AuthenticatorConfigModel model) {
        if (model == null) return null;
        MapAuthenticatorConfigEntity entity = new MapAuthenticatorConfigEntity();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setConfig(model.getConfig() == null ? null : new HashMap<>(model.getConfig()));
        return entity;
    }

    public static AuthenticatorConfigModel toModel(MapAuthenticatorConfigEntity entity) {
        if (entity == null) return null;
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
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
        if (!(obj instanceof MapAuthenticatorConfigEntity)) return false;
        final MapAuthenticatorConfigEntity other = (MapAuthenticatorConfigEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
