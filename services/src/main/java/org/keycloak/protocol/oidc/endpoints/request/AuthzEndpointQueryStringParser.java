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

package org.keycloak.protocol.oidc.endpoints.request;

import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Parse the parameters from request queryString
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthzEndpointQueryStringParser extends AuthzEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(AuthzEndpointRequestParser.class);

    private final MultivaluedMap<String, String> requestParams;

    private final boolean isResponseTypeParameterRequired;

    private String invalidRequestMessage = null;

    public AuthzEndpointQueryStringParser(KeycloakSession keycloakSession, MultivaluedMap<String, String> requestParams, boolean isResponseTypeParameterRequired) {
        super(keycloakSession);
        this.requestParams = requestParams;
        this.isResponseTypeParameterRequired = isResponseTypeParameterRequired;
    }

    @Override
    protected void validateResponseTypeParameter(String responseTypeParameter, AuthorizationEndpointRequest request) {
        // response_type parameter is required in the query string even if present in 'request' object. This is per OIDC core specification
        if (isResponseTypeParameterRequired && responseTypeParameter == null) {
            logger.warn("Missing parameter 'response_type' in the OAuth 2.0 request parameters");
            invalidRequestMessage = "Missing parameter: response_type";
        }

        super.validateResponseTypeParameter(responseTypeParameter, request);
    }

    @Override
    protected String getParameter(String paramName) {
        checkDuplicated(requestParams, paramName);
        return requestParams.getFirst(paramName);
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        checkDuplicated(requestParams, paramName);
        String paramVal = requestParams.getFirst(paramName);
        return paramVal==null ? null : Integer.valueOf(paramVal);
    }

    public String getInvalidRequestMessage() {
        return invalidRequestMessage;
    }

    @Override
    protected Set<String> keySet() {
        return requestParams.keySet();
    }

    private void checkDuplicated(MultivaluedMap<String, String> requestParams, String paramName) {
        if (invalidRequestMessage == null) {
            if (requestParams.get(paramName) != null && requestParams.get(paramName).size() != 1) {
                invalidRequestMessage = "duplicated parameter";
            }
        }
    }

}
