/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.light;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 *
 * @author hmlnarik
 */
class LightweightConsentEntity {

    private String clientId;
    private Long createdDate;
    private Set<String> grantedClientScopesIds;
    private Long lastUpdatedDate;

    public static LightweightConsentEntity fromModel(UserConsentModel model) {
        long currentTime = Time.currentTimeMillis();

        LightweightConsentEntity consentEntity = new LightweightConsentEntity();
        consentEntity.setClientId(model.getClient().getId());
        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);

        model.getGrantedClientScopes()
          .stream()
          .map(ClientScopeModel::getId)
          .forEach(consentEntity::addGrantedClientScopesId);

        return consentEntity;
    }

    public static UserConsentModel toModel(RealmModel realm, LightweightConsentEntity entity) {
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

    @Override
    public int hashCode() {
        return Objects.hash(clientId, grantedClientScopesIds, lastUpdatedDate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LightweightConsentEntity other = (LightweightConsentEntity) obj;
        return Objects.equals(this.clientId, other.clientId)
          && Objects.equals(this.lastUpdatedDate, other.lastUpdatedDate)
          && Objects.equals(this.grantedClientScopesIds, other.grantedClientScopesIds);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", "LightweightConsentEntity", System.identityHashCode(this));
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Set<String> getGrantedClientScopesIds() {
        return grantedClientScopesIds;
    }

    public void removeGrantedClientScopesId(String clientScopeId) {
        if (grantedClientScopesIds == null) {
            return;
        }
        if (grantedClientScopesIds.remove(clientScopeId)) {
            this.lastUpdatedDate = Time.currentTimeMillis();
        }
    }

    public void setGrantedClientScopesIds(Set<String> clientScopeIds) {
        clientScopeIds = clientScopeIds == null ? null : new HashSet<>(clientScopeIds);
        if (clientScopeIds != null) {
            clientScopeIds.removeIf(Objects::isNull);
            if (clientScopeIds.isEmpty()) {
                clientScopeIds = null;
            }
        }
        grantedClientScopesIds = clientScopeIds;
        this.lastUpdatedDate = Time.currentTimeMillis();
    }

    public void addGrantedClientScopesId(String clientScopeId) {
        if (clientScopeId == null) {
            return;
        }
        if (grantedClientScopesIds == null) {
            grantedClientScopesIds = new HashSet<>();
        }
        grantedClientScopesIds.add(clientScopeId);
        this.lastUpdatedDate = Time.currentTimeMillis();
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

}
