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
package org.keycloak.representations.idm.authorization;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class ClientScopePolicyRepresentation extends AbstractPolicyRepresentation {

    private Set<ClientScopeDefinition> clientScopes;

    @Override
    public String getType() {
        return "client-scope";
    }

    public Set<ClientScopeDefinition> getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(Set<ClientScopeDefinition> clientScopes) {
        this.clientScopes = clientScopes;
    }

    public void addClientScope(String name, boolean required) {
        if (clientScopes == null) {
            clientScopes = new HashSet<>();
        }
        clientScopes.add(new ClientScopeDefinition(name, required));
    }

    public void addClientScope(String name) {
        addClientScope(name, false);
    }

    public static class ClientScopeDefinition {
        private String id;
        private boolean required;

        public ClientScopeDefinition() {
            this(null, false);
        }

        public ClientScopeDefinition(String id, boolean required) {
            this.id = id;
            this.required = required;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

    }
}
