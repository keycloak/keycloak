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

package org.keycloak.broker.oid4vp;

import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The OID4VP signed authorization request object: the standard JWT claims from {@link JsonWebToken}
 * plus the OID4VP request parameters. It signs itself as a compact JWS that publishes the verifier's
 * leaf certificate in the {@code x5c} header.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5">OID4VP 1.0 §5 — Authorization Request</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestObject extends JsonWebToken {

    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("response_type")
    private String responseType;
    @JsonProperty("response_mode")
    private String responseMode;
    @JsonProperty("response_uri")
    private String responseUri;
    @JsonProperty("nonce")
    private String nonce;
    @JsonProperty("state")
    private String state;
    @JsonProperty("dcql_query")
    private JsonNode dcqlQuery;
    @JsonProperty("client_metadata")
    private Object clientMetadata;

    public RequestObject clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public RequestObject responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public RequestObject responseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public RequestObject responseUri(String responseUri) {
        this.responseUri = responseUri;
        return this;
    }

    public RequestObject nonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public RequestObject state(String state) {
        this.state = state;
        return this;
    }

    public RequestObject dcqlQuery(JsonNode dcqlQuery) {
        this.dcqlQuery = dcqlQuery;
        return this;
    }

    public RequestObject clientMetadata(Object clientMetadata) {
        this.clientMetadata = clientMetadata;
        return this;
    }

    public String sign(KeyWrapper signingKey) {
        X509Certificate certificate = signingKey.getCertificate();
        if (certificate == null) {
            throw new IdentityBrokerException("The OID4VP verifier signing key has no X.509 certificate");
        }
        try {
            return new JWSBuilder()
                    .type(OID4VCConstants.REQUEST_OBJECT_TYPE)
                    .kid(signingKey.getKid())
                    .x5c(List.of(certificate))
                    .jsonContent(this)
                    .sign(KeyWrapperUtil.createSignatureSignerContext(signingKey));
        } catch (Exception e) {
            throw new IdentityBrokerException("Failed to sign OID4VP request object", e);
        }
    }
}
