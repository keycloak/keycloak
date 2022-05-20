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

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapAuthenticationFlowEntity extends UpdatableEntity, AbstractEntity {
    static MapAuthenticationFlowEntity fromModel(AuthenticationFlowModel model) {
        MapAuthenticationFlowEntity entity = new MapAuthenticationFlowEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setBuiltIn(model.isBuiltIn());
        entity.setDescription(model.getDescription());
        entity.setProviderId(model.getProviderId());
        entity.setTopLevel(model.isTopLevel());

        return entity;
    }

    static AuthenticationFlowModel toModel(MapAuthenticationFlowEntity entity) {
        AuthenticationFlowModel model = new AuthenticationFlowModel();
        model.setId(entity.getId());
        model.setAlias(entity.getAlias());
        Boolean builtIn = entity.isBuiltIn();
        model.setBuiltIn(builtIn == null ? false : builtIn);
        model.setDescription(entity.getDescription());
        model.setProviderId(entity.getProviderId());
        Boolean topLevel = entity.isTopLevel();
        model.setTopLevel(topLevel == null ? false : topLevel);
        return model;
    }

    String getAlias();
    void setAlias(String alias);

    String getDescription();
    void setDescription(String description);

    String getProviderId();
    void setProviderId(String providerId);

    Boolean isBuiltIn();
    void setBuiltIn(Boolean builtIn);

    Boolean isTopLevel();
    void setTopLevel(Boolean topLevel);
}
