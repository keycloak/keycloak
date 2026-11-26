/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.common.util.MultivaluedHashMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModel {

    private final ClientModel client;
    private final Set<ClientScopeModel> clientScopes = new HashSet<>();
    private final MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
    private Long createdDate;
    private Long lastUpdatedDate;

    public UserConsentModel(ClientModel client) {
        this.client = client;
    }

    public ClientModel getClient() {
        return client;
    }

    public void addGrantedClientScope(ClientScopeModel clientScope) {
        addGrantedClientScope(clientScope, null);
    }

    public void addGrantedClientScope(ClientScopeModel clientScope, String parameter) {
        if (clientScope.isAlwaysConsent()) {
            // always consent scopes are skipped
            return;
        }
        clientScopes.add(clientScope);
        if (ClientScopeModel.isParameterizedScope(clientScope)) {
            if (parameter == null) {
                throw new IllegalArgumentException("Parameter value is compulsory for Parameterized Scope " + clientScope.getName());
            }
            parameters.add(clientScope.getId(), parameter);
        }
    }

    public Set<ClientScopeModel> getGrantedClientScopes() {
        return clientScopes;
    }

    public List<String> getParameters(ClientScopeModel clientScope) {
        if (ClientScopeModel.isParameterizedScope(clientScope)) {
            return parameters.getList(clientScope.getId());
        }
        return Collections.emptyList();
    }

    public boolean isClientScopeGranted(ClientScopeModel clientScope) {
        return isClientScopeGranted(clientScope, null);
    }

    public boolean isClientScopeGranted(ClientScopeModel clientScope, String parameter) {
        for (ClientScopeModel apprClientScope : clientScopes) {
            if (apprClientScope.getId().equals(clientScope.getId())) {
                if (ClientScopeModel.isParameterizedScope(clientScope)) {
                    return parameter != null && parameters.getList(apprClientScope.getId()).contains(parameter);
                } else {
                    return parameter == null && parameters.getList(apprClientScope.getId()).isEmpty();
                }
            }
        }
        return false;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
