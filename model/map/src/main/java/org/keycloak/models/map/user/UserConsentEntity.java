/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.user;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class UserConsentEntity implements UpdatableEntity {

    private String clientId;
    private final Set<String> grantedClientScopesIds = new HashSet<>();
    private Long createdDate;
    private Long lastUpdatedDate;
    private boolean updated;
    
    private UserConsentEntity() {}

    public static UserConsentEntity fromModel(UserConsentModel model) {
        long currentTime = Time.currentTimeMillis();

        UserConsentEntity consentEntity = new UserConsentEntity();
        consentEntity.setClientId(model.getClient().getId());
        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);

        model.getGrantedClientScopes()
                .stream()
                .map(ClientScopeModel::getId)
                .forEach(consentEntity::addGrantedClientScopeId);
        
        return consentEntity;
    }
    
    public static UserConsentModel toModel(RealmModel realm, UserConsentEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientModel client = realm.getClientById(entity.getClientId());
        if (client == null) {
            throw new ModelException("Client with id " + entity.getClientId() + " is not available");
        }
        UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());

        entity.getGrantedClientScopesIds().stream()
                .map(scopeId -> KeycloakModelUtils.findClientScopeById(realm, client, scopeId))
                .filter(Objects::nonNull)
                .forEach(model::addGrantedClientScope);
        
        return model;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.updated = !Objects.equals(this.clientId, clientId);
        this.clientId = clientId;
    }

    public Set<String> getGrantedClientScopesIds() {
        return grantedClientScopesIds;
    }

    public void addGrantedClientScopeId(String scope) {
        this.updated |= grantedClientScopesIds.add(scope);
    }
    
    public void setGrantedClientScopesIds(Set<String> scopesIds) {
        this.updated |= !Objects.equals(grantedClientScopesIds, scopesIds);
        this.grantedClientScopesIds.clear();
        this.grantedClientScopesIds.addAll(scopesIds);
    }

    public void removeGrantedClientScopesIds(String scopesId) {
        this.updated |= this.grantedClientScopesIds.remove(scopesId);
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.updated |= !Objects.equals(this.createdDate, createdDate);
        this.createdDate = createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.updated |= !Objects.equals(this.lastUpdatedDate, lastUpdatedDate);
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
