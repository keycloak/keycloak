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

import java.util.List;

/**
 * Represents the proofs parameter structure
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request}
 *
 * @author Bertrand Ogen
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Proofs {
    @JsonProperty(ProofType.JWT)
    private List<String> jwt;

    @JsonProperty(ProofType.LD_PROOF)
    private List<ProofTypeLdpVp> ldpVp;

    public List<String> getJwt() {
        return jwt;
    }

    public Proofs setJwt(List<String> jwt) {
        this.jwt = jwt;
        return this;
    }

    public List<ProofTypeLdpVp> getLdpVp() {
        return ldpVp;
    }

    public Proofs setLdpVp(List<ProofTypeLdpVp> ldpVp) {
        this.ldpVp = ldpVp;
        return this;
    }

    @Override
    public String toString() {
        return "Proofs [jwt=" + jwt + ", ldpVp=" + ldpVp + "]";
    }
}
