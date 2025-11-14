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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * W3C Verifiable Presentation as defined by [VC_DATA_2.0] or [VC_DATA] secured using Data Integrity [VC_Data_Integrity].
 * Used as a proof type in OID4VCI (Section F.2).
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-di_vp-proof-type">OID4VCI di_vp Proof Type</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiVpProof {

    @JsonProperty("@context")
    private List<String> context;

    @JsonProperty("type")
    private List<String> type;

    @JsonProperty("holder")
    private String holder;

    @JsonProperty("proof")
    private List<DataIntegrityProof> proof;

    public DiVpProof() {
    }

    public DiVpProof(List<String> context, List<String> type, String holder, List<DataIntegrityProof> proof) {
        this.context = context;
        this.type = type;
        this.holder = holder;
        this.proof = proof;
    }

    public List<String> getContext() {
        return context;
    }

    public DiVpProof setContext(List<String> context) {
        this.context = context;
        return this;
    }

    public List<String> getType() {
        return type;
    }

    public DiVpProof setType(List<String> type) {
        this.type = type;
        return this;
    }

    public String getHolder() {
        return holder;
    }

    public DiVpProof setHolder(String holder) {
        this.holder = holder;
        return this;
    }

    public List<DataIntegrityProof> getProof() {
        return proof;
    }

    public DiVpProof setProof(List<DataIntegrityProof> proof) {
        this.proof = proof;
        return this;
    }

    /**
     * Data Integrity Proof as defined in [VC_Data_Integrity].
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataIntegrityProof {

        @JsonProperty("type")
        private String type;

        @JsonProperty("cryptosuite")
        private String cryptosuite;

        @JsonProperty("proofPurpose")
        private String proofPurpose;

        @JsonProperty("verificationMethod")
        private String verificationMethod;

        @JsonProperty("created")
        private String created;

        @JsonProperty("challenge")
        private String challenge;

        @JsonProperty("domain")
        private String domain;

        @JsonProperty("proofValue")
        private String proofValue;

        public DataIntegrityProof() {
        }

        public DataIntegrityProof(String type, String cryptosuite, String proofPurpose,
                                  String verificationMethod, String created, String challenge,
                                  String domain, String proofValue) {
            this.type = type;
            this.cryptosuite = cryptosuite;
            this.proofPurpose = proofPurpose;
            this.verificationMethod = verificationMethod;
            this.created = created;
            this.challenge = challenge;
            this.domain = domain;
            this.proofValue = proofValue;
        }

        public String getType() {
            return type;
        }

        public DataIntegrityProof setType(String type) {
            this.type = type;
            return this;
        }

        public String getCryptosuite() {
            return cryptosuite;
        }

        public DataIntegrityProof setCryptosuite(String cryptosuite) {
            this.cryptosuite = cryptosuite;
            return this;
        }

        public String getProofPurpose() {
            return proofPurpose;
        }

        public DataIntegrityProof setProofPurpose(String proofPurpose) {
            this.proofPurpose = proofPurpose;
            return this;
        }

        public String getVerificationMethod() {
            return verificationMethod;
        }

        public DataIntegrityProof setVerificationMethod(String verificationMethod) {
            this.verificationMethod = verificationMethod;
            return this;
        }

        public String getCreated() {
            return created;
        }

        public DataIntegrityProof setCreated(String created) {
            this.created = created;
            return this;
        }

        public String getChallenge() {
            return challenge;
        }

        public DataIntegrityProof setChallenge(String challenge) {
            this.challenge = challenge;
            return this;
        }

        public String getDomain() {
            return domain;
        }

        public DataIntegrityProof setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public String getProofValue() {
            return proofValue;
        }

        public DataIntegrityProof setProofValue(String proofValue) {
            this.proofValue = proofValue;
            return this;
        }
    }
} 
