/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JWT Proof for Credential Request in OID4VCI (Section 8.2.1.1).
 * Represents a signed JWT for holder binding.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request">OID4VCI Credential Request</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtProof implements Proof {

    @JsonProperty("proof_type")
    private final String proofType = ProofType.JWT;

    @JsonProperty("jwt")
    private String jwt;

    public JwtProof() {
    }

    public JwtProof(String jwt) {
        this.jwt = jwt;
    }

    @Override
    public String getProofType() {
        return proofType;
    }

    public String getJwt() {
        return jwt;
    }

    public JwtProof setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }
}
