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

package org.keycloak.services.clientpolicy.context;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

/**
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a>
 */
public class PreAuthorizationRequestContext implements ClientPolicyContext {

    private final String clientId;
    private final MultivaluedMap<String, String> requestParameters;

    public PreAuthorizationRequestContext(String clientId, MultivaluedMap<String, String> requestParameters) {
        this.clientId = clientId;
        this.requestParameters = requestParameters;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return ClientPolicyEvent.PRE_AUTHORIZATION_REQUEST;
    }

    public String getClientId() {
        return clientId;
    }

    public MultivaluedMap<String, String> getRequestParameters() {
        return requestParameters;
    }

}
