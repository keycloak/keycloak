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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;

/**
 * Parse the parameters from OIDC "request" object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthzEndpointRequestObjectParser extends AuthzEndpointRequestParser {

    private static void validateAlgorithm(JOSE jwt, ClientModel clientModel) {
        if (jwt instanceof JWSInput) {
            JOSEHeader header = jwt.getHeader();
            String headerAlgorithm = header.getRawAlgorithm();

            if (headerAlgorithm == null) {
                throw new RuntimeException("Request object signed algorithm not specified");
            }

            Algorithm requestedSignatureAlgorithm = OIDCAdvancedConfigWrapper.fromClientModel(clientModel)
                    .getRequestObjectSignatureAlg();

            if (requestedSignatureAlgorithm != null && !requestedSignatureAlgorithm.name().equals(headerAlgorithm)) {
                throw new RuntimeException("Request object signed with different algorithm than client requested algorithm");
            }
        }
    }

    private final JsonNode requestParams;

    public AuthzEndpointRequestObjectParser(KeycloakSession session, String requestObject, ClientModel client) {
        this.requestParams = session.tokens().decodeClientJWT(requestObject, client, AuthzEndpointRequestObjectParser::validateAlgorithm, JsonNode.class);

        if (this.requestParams == null) {
            throw new RuntimeException("Failed to verify signature on 'request' object");
        }

        JsonNode clientId = this.requestParams.get(OAuth2Constants.CLIENT_ID);

        if (clientId == null) {
            throw new RuntimeException("Request object must be set with the client_id");
        }

        if (!client.getClientId().equals(clientId.asText())) {
            throw new RuntimeException("The client_id in the request object is not the same as the authorizing client");
        }

        session.setAttribute(AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT, requestParams);
    }

    @Override
    protected String getParameter(String paramName) {
        JsonNode val = this.requestParams.get(paramName);
        if (val == null) {
            return null;
        } else if (val.isValueNode()) {
            return val.asText();
        } else {
            return val.toString();
        }
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        Object val = this.requestParams.get(paramName);
        return val==null ? null : Integer.parseInt(getParameter(paramName));
    }

    @Override
    protected Set<String> keySet() {
        HashSet<String> keys = new HashSet<>();
        requestParams.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    @Override
    protected <T> T replaceIfNotNull(T previousVal, T newVal) {
        // force parameters values from request object as per spec any parameter set directly should be ignored
        return newVal;
    }
}
