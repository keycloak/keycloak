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
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * Resolved authorization parameters for redirect URI validation. Response type validation and
 * authentication session creation have not happened yet.
 */
public class AuthorizationRequestRedirectUriContext implements AuthorizationRequestParametersContext {

    private final ClientModel client;
    private final AuthorizationEndpointRequest request;
    private final String redirectUri;

    public AuthorizationRequestRedirectUriContext(ClientModel client, AuthorizationEndpointRequest request,
            String redirectUri) {
        this.client = client;
        this.request = request;
        this.redirectUri = redirectUri;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.AUTHORIZATION_REQUEST_REDIRECT_URI;
    }

    @Override
    public AuthorizationEndpointRequest getAuthorizationEndpointRequest() {
        return request;
    }

    @Override
    public String getRedirectUri() {
        return redirectUri;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }
}
