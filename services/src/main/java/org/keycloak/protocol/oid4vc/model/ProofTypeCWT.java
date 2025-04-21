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

import java.util.List;
import java.util.Objects;

/**
 * See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-cwt-proof-type
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProofTypeCWT {

    @JsonProperty("proof_signing_alg_values_supported")
    private List<Integer> proofSigningAlgValuesSupported;

    @JsonProperty("proof_alg_values_supported")
    private List<Integer> proofAlgValuesSupported;

    @JsonProperty("proof_crv_values_supported")
    private List<Integer> proofCrvValuesSupported;

    public List<Integer> getProofSigningAlgValuesSupported() {
        return proofSigningAlgValuesSupported;
    }

    public ProofTypeCWT setProofSigningAlgValuesSupported(List<Integer> proofSigningAlgValuesSupported) {
        this.proofSigningAlgValuesSupported = proofSigningAlgValuesSupported;
        return this;
    }

    public List<Integer> getProofAlgValuesSupported() {
        return proofAlgValuesSupported;
    }

    public ProofTypeCWT setProofAlgValuesSupported(List<Integer> proofAlgValuesSupported) {
        this.proofAlgValuesSupported = proofAlgValuesSupported;
        return this;
    }

    public List<Integer> getProofCrvValuesSupported() {
        return proofCrvValuesSupported;
    }

    public ProofTypeCWT setProofCrvValuesSupported(List<Integer> proofCrvValuesSupported) {
        this.proofCrvValuesSupported = proofCrvValuesSupported;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProofTypeCWT that = (ProofTypeCWT) o;
        return Objects.equals(proofSigningAlgValuesSupported, that.proofSigningAlgValuesSupported) && Objects.equals(proofAlgValuesSupported, that.proofAlgValuesSupported) && Objects.equals(proofCrvValuesSupported, that.proofCrvValuesSupported);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proofSigningAlgValuesSupported, proofAlgValuesSupported, proofCrvValuesSupported);
    }
}
