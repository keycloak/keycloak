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
 *
 */

package org.keycloak.protocol.oidc.grants.ciba.endpoints.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

/**
 * Parse the parameters from OIDC "request" object
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
class BackchannelAuthenticationEndpointSignedRequestParser extends BackchannelAuthenticationEndpointRequestParser {

    private final JsonNode requestParams;

    public BackchannelAuthenticationEndpointSignedRequestParser(KeycloakSession session, String signedAuthReq, ClientModel client, CibaConfig config) throws Exception {
        JOSE jwt = JOSEParser.parse(signedAuthReq);

        if (jwt instanceof JWE) {
            throw new RuntimeException("Encrypted request object is not allowed");
        }

        JWSInput input = (JWSInput) jwt;
        JWSHeader header = input.getHeader();
        Algorithm headerAlgorithm = header.getAlgorithm();

        Algorithm requestedSignatureAlgorithm = config.getBackchannelAuthRequestSigningAlg(client);

        if (headerAlgorithm == null) {
            throw new RuntimeException("Signed algorithm not specified");
        }
        if (header.getAlgorithm() == Algorithm.none) {
            throw new RuntimeException("None signed algorithm is not allowed");
        }
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, headerAlgorithm.name());
        if (signatureProvider == null) {
            throw new RuntimeException("Not found provider for the algorithm " + headerAlgorithm.name());
        }
        if (!signatureProvider.isAsymmetricAlgorithm()) {
            throw new RuntimeException("Signed algorithm is not allowed");
        }
        if (requestedSignatureAlgorithm == null || requestedSignatureAlgorithm != headerAlgorithm) {
            throw new RuntimeException("Client requested algorithm not registered in advance or request signed with different algorithm other than client requested algorithm");
        }

        this.requestParams = session.tokens().decodeClientJWT(signedAuthReq, client, JsonNode.class);
        if (this.requestParams == null) {
            throw new RuntimeException("Failed to verify signature");
        }

        session.setAttribute(BackchannelAuthenticationEndpointRequestParser.CIBA_SIGNED_AUTHENTICATION_REQUEST, requestParams);
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
        return val==null ? null : Integer.valueOf(getParameter(paramName));
    }

    @Override
    protected Set<String> keySet() {
        HashSet<String> keys = new HashSet<>();
        requestParams.fieldNames().forEachRemaining(keys::add);
        return keys;
    }
}
