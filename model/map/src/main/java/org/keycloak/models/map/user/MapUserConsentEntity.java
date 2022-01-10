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

package org.keycloak.models.map.user;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Objects;
import java.util.Set;

@GenerateEntityImplementations
@DeepCloner.Root
public interface MapUserConsentEntity extends UpdatableEntity {

    public static MapUserConsentEntity fromModel(UserConsentModel model) {
        long currentTime = Time.currentTimeMillis();

        MapUserConsentEntity consentEntity = new MapUserConsentEntityImpl();
        consentEntity.setClientId(model.getClient().getId());
        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);

        model.getGrantedClientScopes()
                .stream()
                .map(ClientScopeModel::getId)
                .forEach(consentEntity::addGrantedClientScopesId);

        return consentEntity;
    }

    public static UserConsentModel toModel(RealmModel realm, MapUserConsentEntity entity) {
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


        Set<String> grantedClientScopesIds = entity.getGrantedClientScopesIds();

        if (grantedClientScopesIds != null && !grantedClientScopesIds.isEmpty()) {
            grantedClientScopesIds.stream()
                    .map(scopeId -> KeycloakModelUtils.findClientScopeById(realm, client, scopeId))
                    .filter(Objects::nonNull)
                    .forEach(model::addGrantedClientScope);
        }

        return model;
    }

    String getClientId();
    void setClientId(String clientId);

    Set<String> getGrantedClientScopesIds();
    void addGrantedClientScopesId(String scope);
    void setGrantedClientScopesIds(Set<String> scopesIds);
    void removeGrantedClientScopesId(String scopesId);

    Long getCreatedDate();
    void setCreatedDate(Long createdDate);

    Long getLastUpdatedDate();
    void setLastUpdatedDate(Long lastUpdatedDate);
}
