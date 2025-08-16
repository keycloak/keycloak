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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.AttestationProof;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.Proof;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;


import java.util.List;
import java.util.Optional;

/**
 * Validates attestation proofs as per OID4VCI specification.
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-attestation-proof-type">
 * Attestation Proof Type</a>
 */
public class AttestationProofValidator extends AbstractProofValidator {

    private final AttestationKeyResolver keyResolver;

    public AttestationProofValidator(KeycloakSession session, AttestationKeyResolver keyResolver) {
        super(session);
        this.keyResolver = keyResolver;
    }

    @Override
    public String getProofType() {
        return ProofType.ATTESTATION;
    }

    @Override
    public List<JWK> validateProof(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {
        try {
            AttestationProof proof = extractAttestationProof(vcIssuanceContext);
            String jwt = Optional.ofNullable(proof.getAttestation())
                    .orElseThrow(() -> new VCIssuerException("Attestation JWT is missing"));

            KeyAttestationJwtBody attestationBody = AttestationValidatorUtil.validateAttestationJwt(
                    jwt, keycloakSession, vcIssuanceContext, keyResolver);

            if (attestationBody.getAttestedKeys() == null || attestationBody.getAttestedKeys().isEmpty()) {
                throw new VCIssuerException("No valid attested keys found in attestation proof");
            }

            return attestationBody.getAttestedKeys();
        } catch (VCIssuerException e) {
            throw e; // Re-throw specific exceptions
        } catch (Exception e) {
            throw new VCIssuerException("Failed to validate attestation proof: " + e.getMessage(), e);
        }
    }

    private AttestationProof extractAttestationProof(VCIssuanceContext vcIssuanceContext)
            throws VCIssuerException {

        SupportedCredentialConfiguration config = Optional.ofNullable(vcIssuanceContext.getCredentialConfig())
                .orElseThrow(() -> new VCIssuerException("Credential configuration is missing"));

        if (config.getProofTypesSupported() == null || config.getProofTypesSupported().getSupportedProofTypes().get("attestation") == null) {
            throw new VCIssuerException("Attestation proof type not supported");
        }

        Proof proof = vcIssuanceContext.getCredentialRequest().getProof();
        if (!(proof instanceof AttestationProof attestationProof)) {
            throw new VCIssuerException("Expected attestation proof type, found: " +
                    (proof == null ? "null" : proof.getClass().getSimpleName()));
        }

        return attestationProof;
    }
}
