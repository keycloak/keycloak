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
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.keys.loader.PublicKeyStorageManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.util.JsonSerialization;

/**
 * Parse the parameters from OIDC "request" object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class AuthzEndpointRequestObjectParser extends AuthzEndpointRequestParser {

    private final JsonNode requestParams;

    public AuthzEndpointRequestObjectParser(KeycloakSession session, String requestObject, ClientModel client) throws Exception {
        JWSInput input = new JWSInput(requestObject);
        JWSHeader header = input.getHeader();

        Algorithm requestedSignatureAlgorithm = OIDCAdvancedConfigWrapper.fromClientModel(client).getRequestObjectSignatureAlg();

        if (requestedSignatureAlgorithm != null && requestedSignatureAlgorithm != header.getAlgorithm()) {
            throw new RuntimeException("Request object signed with different algorithm than client requested algorithm");
        }

        if (header.getAlgorithm() == Algorithm.none) {
            this.requestParams = JsonSerialization.readValue(input.getContent(), JsonNode.class);
        } else if (header.getAlgorithm() == Algorithm.RS256) {
            PublicKey clientPublicKey = PublicKeyStorageManager.getClientPublicKey(session, client, input);
            if (clientPublicKey == null) {
                throw new RuntimeException("Client public key not found");
            }

            boolean verified = RSAProvider.verify(input, clientPublicKey);
            if (!verified) {
                throw new RuntimeException("Failed to verify signature on 'request' object");
            }

            this.requestParams = JsonSerialization.readValue(input.getContent(), JsonNode.class);
        } else {
            throw new RuntimeException("Unsupported JWA algorithm used for signed request");
        }
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

    static class TypedHashMap extends HashMap<String, Object> {
    }
}
