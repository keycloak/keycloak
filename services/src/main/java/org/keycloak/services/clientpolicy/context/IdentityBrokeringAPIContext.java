/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

public class IdentityBrokeringAPIContext implements ClientModelContext, ScopeParameterContext, IdentityProviderContext {

    private final KeycloakSession session;
    private final AccessToken accessToken;
    private final ClientModel client;
    private final String identityProviderAlias;

    public IdentityBrokeringAPIContext(KeycloakSession session, AccessToken accessToken, ClientModel client, String identityProviderAlias) {
        this.session = session;
        this.accessToken = accessToken;
        this.client = client;
        this.identityProviderAlias = identityProviderAlias;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.IDENTITY_BROKERING_API;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public String getScopeParameter() {
        return accessToken.getScope();
    }

    @Override
    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }
}
