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

package org.keycloak.protocol.oidc.par.endpoints.request;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestObjectParser;

/**
 * Parse the parameters from a request object sent to PAR Endpoint
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ParEndpointRequestObjectParser extends AuthzEndpointRequestObjectParser {

    public ParEndpointRequestObjectParser(KeycloakSession session, String requestObject, ClientModel client) {
        super(session, requestObject, client);
    }

    @Override
    protected <T> T replaceIfNotNull(T previousVal, T newVal) {
        // force parameters values from request object as per spec any parameter set directly should be ignored
        return newVal;
    }

    @Override
    protected void validateResponseTypeParameter(String responseType, AuthorizationEndpointRequest request) {
        // Don't need to validate duplicated "response_type" parameter as per spec any parameter set directly should be ignored
        // and the value from "request" object should be used
    }
}
