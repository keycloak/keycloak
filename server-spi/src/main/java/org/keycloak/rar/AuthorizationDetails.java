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
package org.keycloak.rar;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * The internal Keycloak representation of a Rich Authorization Request authorization_details object, together with
 * some extra metadata to make it easier to work with this data in other parts of the codebase.
 *
 * The {@link AuthorizationRequestSource} is needed as OAuth scopes are also parsed into AuthorizationDetails
 * to standardize the way authorization data is managed in Keycloak. Scopes parsed as AuthorizationDetails will need
 * to be treated as normal OAuth scopes in places like TokenMappers and included in the "scopes" JWT claim as such.
 *
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
public class AuthorizationDetails implements Serializable {

    private ClientScopeModel clientScope;

    private AuthorizationRequestSource source;

    private AuthorizationDetailsJSONRepresentation authorizationDetails;

    public AuthorizationDetails(ClientScopeModel clientScope, AuthorizationRequestSource source, AuthorizationDetailsJSONRepresentation authorizationDetails) {
        this.clientScope = clientScope;
        this.source = source;
        this.authorizationDetails = authorizationDetails;
    }

    public AuthorizationDetails(ClientScopeModel clientScope) {
        this.clientScope = clientScope;
        this.source = AuthorizationRequestSource.SCOPE;
    }

    public ClientScopeModel getClientScope() {
        return clientScope;
    }

    public void setClientScope(ClientScopeModel clientScope) {
        this.clientScope = clientScope;
    }

    public AuthorizationRequestSource getSource() {
        return source;
    }

    public void setSource(AuthorizationRequestSource source) {
        this.source = source;
    }

    public AuthorizationDetailsJSONRepresentation getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(AuthorizationDetailsJSONRepresentation authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }

    /**
     * Returns whether the current {@link AuthorizationDetails} object is a dynamic scope
     * @return see description
     */
    public boolean isDynamicScope() {
        return this.source.equals(AuthorizationRequestSource.SCOPE) && this.getClientScope().isDynamicScope();
    }

    /**
     * Returns the Dynamic Scope parameter from the underlying {@link AuthorizationDetailsJSONRepresentation} representation
     * @return see description
     */
    public String getDynamicScopeParam() {
        if(isDynamicScope()) {
            return this.authorizationDetails.getDynamicScopeParamFromCustomData();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationDetails that = (AuthorizationDetails) o;
        return Objects.equals(clientScope, that.clientScope) && source == that.source && Objects.equals(authorizationDetails, that.authorizationDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientScope, source, authorizationDetails);
    }

    @Override
    public String toString() {
        return "AuthorizationDetails{" +
                "clientScope=" + clientScope +
                ", source=" + source +
                ", authorizationDetails=" + authorizationDetails +
                '}';
    }
}
