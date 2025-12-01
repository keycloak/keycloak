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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deprecated: Represents a single JWT-based proof (historical 'proof' structure).
 * Prefer using {@link Proofs} with the appropriate array field (e.g., jwt).
 * This class is kept for backward compatibility only.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-15.html#name-credential-request">OID4VCI Credential Request</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Deprecated
public class JwtProof {

    @JsonProperty("jwt")
    private String jwt;

    @JsonProperty("proof_type")
    private String proofType;

    public JwtProof() {
    }

    public JwtProof(String jwt, String proofType) {
        this.jwt = jwt;
        this.proofType = proofType;
    }

    public String getJwt() {
        return jwt;
    }

    public JwtProof setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }

    public String getProofType() {
        return proofType;
    }

    public JwtProof setProofType(String proofType) {
        this.proofType = proofType;
        return this;
    }
}
