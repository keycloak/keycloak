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
 *
 */
package org.keycloak.testsuite.rar;

import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.rar.AuthorizationRequestSource;
import org.keycloak.representations.idm.ClientScopeRepresentation;

import java.util.List;
import java.util.Objects;

/**
 * The local testsuite representation of a {@link org.keycloak.rar.AuthorizationRequestContext} server object
 *
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
public class AuthorizationRequestContextHolder {

    private List<AuthorizationRequestHolder> authorizationRequestHolders;

    public AuthorizationRequestContextHolder() {
    }

    public AuthorizationRequestContextHolder(List<AuthorizationRequestHolder> authorizationRequestHolders) {
        this.authorizationRequestHolders = authorizationRequestHolders;
    }

    public List<AuthorizationRequestHolder> getAuthorizationRequestHolders() {
        return authorizationRequestHolders;
    }

    public void setAuthorizationRequestHolders(List<AuthorizationRequestHolder> authorizationRequestHolders) {
        this.authorizationRequestHolders = authorizationRequestHolders;
    }

    /**
     * The local testsuite representation of a {@link AuthorizationDetails} server object
     */
    public static class AuthorizationRequestHolder {

        private ClientScopeRepresentation clientScopeRepresentation;
        private AuthorizationRequestSource source;
        private AuthorizationDetailsJSONRepresentation authorizationDetails;

        public AuthorizationRequestHolder() {

        }

        public AuthorizationRequestHolder(AuthorizationDetails authorizationDetails) {
            this.clientScopeRepresentation = ModelToRepresentation.toRepresentation(authorizationDetails.getClientScope());
            this.source = authorizationDetails.getSource();
            this.authorizationDetails = authorizationDetails.getAuthorizationDetails();
        }

        public ClientScopeRepresentation getClientScopeRepresentation() {
            return clientScopeRepresentation;
        }

        public AuthorizationRequestSource getSource() {
            return source;
        }

        public AuthorizationDetailsJSONRepresentation getAuthorizationDetails() {
            return authorizationDetails;
        }

        public void setClientScopeRepresentation(ClientScopeRepresentation clientScopeRepresentation) {
            this.clientScopeRepresentation = clientScopeRepresentation;
        }

        public void setSource(AuthorizationRequestSource source) {
            this.source = source;
        }

        public void setAuthorizationDetails(AuthorizationDetailsJSONRepresentation authorizationDetails) {
            this.authorizationDetails = authorizationDetails;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AuthorizationRequestHolder that = (AuthorizationRequestHolder) o;
            return Objects.equals(clientScopeRepresentation, that.clientScopeRepresentation) && source == that.source && Objects.equals(authorizationDetails, that.authorizationDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clientScopeRepresentation, source, authorizationDetails);
        }
    }
}
