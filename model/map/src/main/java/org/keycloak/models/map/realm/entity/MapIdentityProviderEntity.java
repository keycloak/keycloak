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
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapIdentityProviderEntity extends UpdatableEntity.Impl {

    private String id;
    private String alias;
    private String displayName;
    private String providerId;
    private String firstBrokerLoginFlowId;
    private String postBrokerLoginFlowId;
    private Boolean enabled = false;
    private Boolean trustEmail = false;
    private Boolean storeToken = false;
    private Boolean linkOnly = false;
    private Boolean addReadTokenRoleOnCreate = false;
    private Boolean authenticateByDefault = false;
    private Map<String, String> config = new HashMap<>();


    private MapIdentityProviderEntity() {}

    public static MapIdentityProviderEntity fromModel(IdentityProviderModel model) {
        if (model == null) return null;
        MapIdentityProviderEntity entity = new MapIdentityProviderEntity();
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
        entity.setConfig(model.getConfig() == null ? null : new HashMap<>(model.getConfig()));
        return entity;
    }

    public static IdentityProviderModel toModel(MapIdentityProviderEntity entity) {
        if (entity == null) return null;
        IdentityProviderModel model = new IdentityProviderModel();
        model.setInternalId(entity.getId());
        model.setAlias(entity.getAlias());
        model.setDisplayName(entity.getDisplayName());
        model.setProviderId(entity.getProviderId());
        model.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
        model.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
        model.setEnabled(entity.isEnabled());
        model.setTrustEmail(entity.isTrustEmail());
        model.setStoreToken(entity.isStoreToken());
        model.setLinkOnly(entity.isLinkOnly());
        model.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());
        model.setAuthenticateByDefault(entity.isAuthenticateByDefault());
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.updated = !Objects.equals(this.displayName, displayName);
        this.displayName = displayName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.updated = !Objects.equals(this.providerId, providerId);
        this.providerId = providerId;
    }

    public String getFirstBrokerLoginFlowId() {
        return firstBrokerLoginFlowId;
    }

    public void setFirstBrokerLoginFlowId(String firstBrokerLoginFlowId) {
        this.updated = !Objects.equals(this.firstBrokerLoginFlowId, firstBrokerLoginFlowId);
        this.firstBrokerLoginFlowId = firstBrokerLoginFlowId;
    }

    public String getPostBrokerLoginFlowId() {
        return postBrokerLoginFlowId;
    }

    public void setPostBrokerLoginFlowId(String postBrokerLoginFlowId) {
        this.updated = !Objects.equals(this.postBrokerLoginFlowId, postBrokerLoginFlowId);
        this.postBrokerLoginFlowId = postBrokerLoginFlowId;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.updated = !Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    public Boolean isTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(boolean trustEmail) {
        this.updated = !Objects.equals(this.trustEmail, trustEmail);
        this.trustEmail = trustEmail;
    }

    public Boolean isStoreToken() {
        return storeToken;
    }

    public void setStoreToken(boolean storeToken) {
        this.updated = !Objects.equals(this.storeToken, storeToken);
        this.storeToken = storeToken;
    }

    public Boolean isLinkOnly() {
        return linkOnly;
    }

    public void setLinkOnly(boolean linkOnly) {
        this.updated = !Objects.equals(this.linkOnly, linkOnly);
        this.linkOnly = linkOnly;
    }

    public Boolean isAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(boolean addReadTokenRoleOnCreate) {
        this.updated = !Objects.equals(this.addReadTokenRoleOnCreate, addReadTokenRoleOnCreate);
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    public Boolean isAuthenticateByDefault() {
        return authenticateByDefault;
    }

    public void setAuthenticateByDefault(boolean authenticateByDefault) {
        this.updated = !Objects.equals(this.authenticateByDefault, authenticateByDefault);
        this.authenticateByDefault = authenticateByDefault;
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
        if (!(obj instanceof MapIdentityProviderEntity)) return false;
        final MapIdentityProviderEntity other = (MapIdentityProviderEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
