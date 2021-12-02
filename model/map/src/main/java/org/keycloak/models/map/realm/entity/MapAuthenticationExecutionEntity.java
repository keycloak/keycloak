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
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapAuthenticationExecutionEntity extends UpdatableEntity.Impl {

    private String id;
    private String authenticator;
    private String authenticatorConfig;
    private String flowId;
    private String parentFlowId;
    private AuthenticationExecutionModel.Requirement requirement;
    private Boolean autheticatorFlow = false;
    private Integer priority = 0;


    private MapAuthenticationExecutionEntity() {}

    public static MapAuthenticationExecutionEntity fromModel(AuthenticationExecutionModel model) {
        if (model == null) return null;
        MapAuthenticationExecutionEntity entity = new MapAuthenticationExecutionEntity();
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

    public static AuthenticationExecutionModel toModel(MapAuthenticationExecutionEntity entity) {
        if (entity == null) return null;
        AuthenticationExecutionModel model = new AuthenticationExecutionModel();
        model.setId(entity.getId());
        model.setAuthenticator(entity.getAuthenticator());
        model.setAuthenticatorConfig(entity.getAuthenticatorConfig());
        model.setFlowId(entity.getFlowId());
        model.setParentFlow(entity.getParentFlowId());
        model.setRequirement(entity.getRequirement());
        model.setAuthenticatorFlow(entity.isAutheticatorFlow());
        model.setPriority(entity.getPriority());
        return model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.updated = !Objects.equals(this.id, id);
        this.id = id;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.updated = !Objects.equals(this.authenticator, authenticator);
        this.authenticator = authenticator;
    }

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.updated = !Objects.equals(this.authenticatorConfig, authenticatorConfig);
        this.authenticatorConfig = authenticatorConfig;
    }

    public AuthenticationExecutionModel.Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(AuthenticationExecutionModel.Requirement requirement) {
        this.updated = !Objects.equals(this.requirement, requirement);
        this.requirement = requirement;
    }

    public Boolean isAutheticatorFlow() {
        return autheticatorFlow;
    }

    public void setAutheticatorFlow(boolean autheticatorFlow) {
        this.updated = !Objects.equals(this.requirement, requirement);
        this.autheticatorFlow = autheticatorFlow;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.updated = !Objects.equals(this.flowId, flowId);
        this.flowId = flowId;
    }

    public String getParentFlowId() {
        return parentFlowId;
    }

    public void setParentFlowId(String parentFlowId) {
        this.updated = !Objects.equals(this.parentFlowId, parentFlowId);
        this.parentFlowId = parentFlowId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.updated = !Objects.equals(this.priority, priority);
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapAuthenticationExecutionEntity)) return false;
        final MapAuthenticationExecutionEntity other = (MapAuthenticationExecutionEntity) obj;
        return Objects.equals(other.getId(), getId());
    }

}
