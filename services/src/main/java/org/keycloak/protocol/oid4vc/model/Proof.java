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
 * Proof to be used in the Credential Request(to allow holder binding) according to OID4VCI
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-request}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Proof {

    @JsonProperty("proof_type")
    private String proofType;

    @JsonProperty("jwt")
    private String jwt;

    @JsonProperty("cwt")
    private String cwt;

    @JsonProperty("ldp_vp")
    private Object ldpVp;

    public String getProofType() {
        return proofType;
    }

    public Proof setProofType(String proofType) {
        this.proofType = proofType;
        return this;
    }

    public String getJwt() {
        return jwt;
    }

    public Proof setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }

    public String getCwt() {
        return cwt;
    }

    public Proof setCwt(String cwt) {
        this.cwt = cwt;
        return this;
    }

    public Object getLdpVp() {
        return ldpVp;
    }

    public Proof setLdpVp(Object ldpVp) {
        this.ldpVp = ldpVp;
        return this;
    }
}
