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

package org.keycloak.protocol.oidc.par.clientpolicy.context;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.context.ClientModelContext;
import org.keycloak.services.clientpolicy.context.ScopeParameterContext;

public class PushedAuthorizationRequestContext implements ClientPolicyContext, ClientModelContext, ScopeParameterContext {

    private final ClientModel client;
    private final MultivaluedMap<String, String> requestParameters;
    private AuthorizationEndpointRequest request;

    public PushedAuthorizationRequestContext(ClientModel client, AuthorizationEndpointRequest request,
            MultivaluedMap<String, String> requestParameters) {
        this.client = client;
        this.request = request;
        this.requestParameters = requestParameters;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.PUSHED_AUTHORIZATION_REQUEST;
    }

    public AuthorizationEndpointRequest getRequest() {
        return request;
    }

    public MultivaluedMap<String, String> getRequestParameters() {
        return requestParameters;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public String getScopeParameter() {
        return request.getScope();
    }
}
