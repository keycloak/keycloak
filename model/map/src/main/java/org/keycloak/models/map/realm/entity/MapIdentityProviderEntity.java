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

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.Map;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapIdentityProviderEntity extends UpdatableEntity, AbstractEntity {
    static MapIdentityProviderEntity fromModel(IdentityProviderModel model) {
        if (model == null) return null;
        MapIdentityProviderEntity entity = new MapIdentityProviderEntityImpl();
        String id = model.getInternalId() == null ? KeycloakModelUtils.generateId() : model.getInternalId();
        entity.setId(id);
        entity.setAlias(model.getAlias());
        entity.setDisplayName(model.getDisplayName());
        entity.setProviderId(model.getProviderId());
        entity.setFirstBrokerLoginFlowId(model.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(model.getPostBrokerLoginFlowId());
        entity.setEnabled(model.isEnabled());
        entity.setTrustEmail(model.isTrustEmail());
        entity.setStoreToken(model.isStoreToken());
        entity.setLinkOnly(model.isLinkOnly());
        entity.setAddReadTokenRoleOnCreate(model.isAddReadTokenRoleOnCreate());
        entity.setAuthenticateByDefault(model.isAuthenticateByDefault());
        entity.setConfig(model.getConfig());
        return entity;
    }

    static IdentityProviderModel toModel(MapIdentityProviderEntity entity) {
        if (entity == null) return null;
        IdentityProviderModel model = new IdentityProviderModel();
        model.setInternalId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setDisplayName(entity.getDisplayName());
        model.setProviderId(entity.getProviderId());
        model.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
        model.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
        Boolean enabled = entity.isEnabled();
        model.setEnabled(enabled == null ? false : enabled);
        Boolean trustEmail = entity.isTrustEmail();
        model.setTrustEmail(trustEmail == null ? false : trustEmail);
        Boolean storeToken = entity.isStoreToken();
        model.setStoreToken(storeToken == null ? false : storeToken);
        Boolean linkOnly = entity.isLinkOnly();
        model.setLinkOnly(linkOnly == null ? false : linkOnly);
        Boolean addReadTokenRoleOnCreate = entity.isAddReadTokenRoleOnCreate();
        model.setAddReadTokenRoleOnCreate(addReadTokenRoleOnCreate == null ? false : addReadTokenRoleOnCreate);
        Boolean authenticateByDefault = entity.isAuthenticateByDefault();
        model.setAuthenticateByDefault(authenticateByDefault == null ? false : authenticateByDefault);
        Map<String, String> config = entity.getConfig();
        model.setConfig(config == null ? new HashMap<>() : new HashMap<>(config));
        return model;
    }

    String getAlias();
    void setAlias(String alias);

    String getDisplayName();
    void setDisplayName(String displayName);

    String getProviderId();
    void setProviderId(String providerId);

    String getFirstBrokerLoginFlowId();
    void setFirstBrokerLoginFlowId(String firstBrokerLoginFlowId);

    String getPostBrokerLoginFlowId();
    void setPostBrokerLoginFlowId(String postBrokerLoginFlowId);

    Boolean isEnabled();
    void setEnabled(Boolean enabled);

    Boolean isTrustEmail();
    void setTrustEmail(Boolean trustEmail);

    Boolean isStoreToken();
    void setStoreToken(Boolean storeToken);

    Boolean isLinkOnly();
    void setLinkOnly(Boolean linkOnly);

    Boolean isAddReadTokenRoleOnCreate();
    void setAddReadTokenRoleOnCreate(Boolean addReadTokenRoleOnCreate);

    Boolean isAuthenticateByDefault();
    void setAuthenticateByDefault(Boolean authenticateByDefault);

    Map<String, String> getConfig();
    void setConfig(Map<String, String> config);
}
