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

import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapAuthenticatorConfigEntity extends UpdatableEntity, AbstractEntity {
    static MapAuthenticatorConfigEntity fromModel(AuthenticatorConfigModel model) {
        if (model == null) return null;
        MapAuthenticatorConfigEntity entity = new MapAuthenticatorConfigEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setConfig(model.getConfig());
        return entity;
    }

    static AuthenticatorConfigModel toModel(MapAuthenticatorConfigEntity entity) {
        if (entity == null) return null;
        AuthenticatorConfigModel model = new AuthenticatorConfigModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        Map<String, String> config = new HashMap<>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        model.setConfig(config);
        return model;
    }

    String getAlias();
    void setAlias(String alias);

    Map<String, String> getConfig();
    void setConfig(Map<String, String> config);
}
