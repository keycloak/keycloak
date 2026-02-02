/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSAAlgorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IDTokenRequest {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    @JsonProperty("response_type")
    private String responseType;

    @JsonProperty("request")
    private String request;

    @JsonProperty("scope")
    private String scope;

    @JsonIgnore
    private JWSInput jwsInput;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public JWSInput getJWSInput() {
        if (jwsInput == null) {
            try {
                jwsInput = new JWSInput(request);
            } catch (JWSInputException e) {
                throw new IllegalStateException(e);
            }
        }
        return jwsInput;
    }

    // =====================================================================================
    // Serialization support
    // =====================================================================================

    public Map<String, List<String>> toRequestParameters() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        add(params, "client_id", clientId);
        add(params, "redirect_uri", redirectUri);
        add(params, "response_type", responseType);
        add(params, "request", request);
        add(params, "scope", scope);
        return params;
    }

    public String toRequestUrl(String endpointUri) {
        Map<String, List<String>> params = toRequestParameters();
        KeycloakUriBuilder b = KeycloakUriBuilder.fromUri(endpointUri, false);
        params.forEach((k, lst) -> b.queryParam(k, lst.toArray()));
        return b.build().toString();
    }

    public void verify(PublicKey publicKey) throws VerificationException {

        try {
            JWSInput jws = getJWSInput();
            String algo = jws.getHeader().getRawAlgorithm();

            // Verify signature
            byte[] signedData = jws.getEncodedSignatureInput().getBytes(UTF_8);
            byte[] signature = jws.getSignature();

            switch (algo) {
                case Algorithm.ES256: {
                    Signature verifier = Signature.getInstance(JavaAlgorithm.ES256);
                    verifier.initVerify(publicKey);
                    verifier.update(signedData);

                    int expectedSize = ECDSAAlgorithm.getSignatureLength(algo);
                    byte[] der = ECDSAAlgorithm.concatenatedRSToASN1DER(signature, expectedSize);

                    boolean valid = verifier.verify(der);
                    if (!valid)
                        throw new VerificationException("Invalid ES256 signature");
                    break;
                }
                case Algorithm.RS256: {
                    Signature verifier = Signature.getInstance(JavaAlgorithm.RS256);
                    verifier.initVerify(publicKey);
                    verifier.update(signedData);
                    boolean valid = verifier.verify(signature);
                    if (!valid)
                        throw new VerificationException("Invalid RS256 signature");
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported algorithm");
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private void add(Map<String, List<String>> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, List.of(value));
        }
    }
}
