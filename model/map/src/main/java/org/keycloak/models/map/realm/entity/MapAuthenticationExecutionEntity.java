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

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapAuthenticationExecutionEntity extends UpdatableEntity, AbstractEntity {
    static MapAuthenticationExecutionEntity fromModel(AuthenticationExecutionModel model) {
        if (model == null) return null;
        MapAuthenticationExecutionEntity entity = new MapAuthenticationExecutionEntityImpl();
        String id = model.getId() == null ? KeycloakModelUtils.generateId() : model.getId();
        entity.setId(id);
        entity.setAuthenticator(model.getAuthenticator());
        entity.setAuthenticatorConfig(model.getAuthenticatorConfig());
        entity.setFlowId(model.getFlowId());
        entity.setParentFlowId(model.getParentFlow());
        entity.setRequirement(model.getRequirement());
        entity.setAutheticatorFlow(model.isAuthenticatorFlow());
        entity.setPriority(model.getPriority());
        return entity;
    }

    static AuthenticationExecutionModel toModel(MapAuthenticationExecutionEntity entity) {
        if (entity == null) return null;
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setAuthenticator(entity.getAuthenticator());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        model.setFlowId(entity.getFlowId());
        model.setParentFlow(entity.getParentFlowId());
        model.setRequirement(entity.getRequirement());
        Boolean authenticatorFlow = entity.isAutheticatorFlow();
        model.setAuthenticatorFlow(authenticatorFlow == null ? false : authenticatorFlow);
        Integer priority = entity.getPriority();
        model.setPriority(priority == null ? 0 : priority);
        return model;
    }

    String getAuthenticator();
    void setAuthenticator(String authenticator);

    String getAuthenticatorConfig();
    void setAuthenticatorConfig(String authenticatorConfig);

    AuthenticationExecutionModel.Requirement getRequirement();
    void setRequirement(AuthenticationExecutionModel.Requirement requirement);

    Boolean isAutheticatorFlow();
    void setAutheticatorFlow(Boolean autheticatorFlow);

    String getFlowId();
    void setFlowId(String flowId);

    String getParentFlowId();
    void setParentFlowId(String parentFlowId);

    Integer getPriority();
    void setPriority(Integer priority);
}
