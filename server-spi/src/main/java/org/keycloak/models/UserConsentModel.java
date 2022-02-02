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

import org.keycloak.rar.AuthorizationDetails;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserConsentModel {

    private String id;
    private final ClientModel client;
    private Set<ClientScopeModel> clientScopes = new HashSet<>();
    private List<String> consentedScopesFromAuthorizationDetails = new LinkedList<>();
    private Long createdDate;
    private Long lastUpdatedDate;

    public UserConsentModel(ClientModel client) {
        this.client = client;
    }

    public ClientModel getClient() {
        return client;
    }

    public void addGrantedClientScope(ClientScopeModel clientScope) {
        clientScopes.add(clientScope);
    }

    public void addConsentedScopeFromAuthorizationDetails(AuthorizationDetails authorizationDetails) {
        String scopeName = authorizationDetails.getClientScope().getId();
        if (authorizationDetails.isDynamicScope()) {
            scopeName = scopeName.concat(":").concat(authorizationDetails.getDynamicScopeParam());
        }
        consentedScopesFromAuthorizationDetails.add(scopeName);
    }

    public Set<ClientScopeModel> getGrantedClientScopes() {
        return clientScopes;
    }

    public List<String> getConsentedScopesFromAuthorizationDetails() {
        return consentedScopesFromAuthorizationDetails;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isClientScopeGranted(ClientScopeModel clientScope) {
        // TODO: May need to be changed with adding support for client scopes inheritance
        for (ClientScopeModel apprClientScope : clientScopes) {
            if (apprClientScope.getId().equals(clientScope.getId())) return true;
        }
        return false;
    }

    public boolean isClientScopeGranted(AuthorizationDetails authDetails, List<String> consentedScopes) {
        return consentedScopes.stream().anyMatch(s -> authDetails.getClientScope().getId().equalsIgnoreCase(s) || (authDetails.getClientScope().getId() + ":" + authDetails.getDynamicScopeParam()).equalsIgnoreCase(s));
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
