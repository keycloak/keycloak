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

package org.keycloak.protocol.oid4vc.issuance.signing;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.common.util.Base64;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.signing.vcdm.Ed255192018Suite;
import org.keycloak.protocol.oid4vc.issuance.signing.vcdm.LinkedDataCryptographicSuite;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.model.vcdm.LdProof;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * {@link VerifiableCredentialsSigningService} implementing the LDP_VC format. It returns a Verifiable Credential,
 * containing the created LDProof.
 * <p>
 * {@see https://www.w3.org/TR/vc-data-model/}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class LDSigningService extends SigningService<VerifiableCredential> {

    private final LinkedDataCryptographicSuite linkedDataCryptographicSuite;
    private final TimeProvider timeProvider;
    private final String keyId;

    public LDSigningService(KeycloakSession keycloakSession, String keyId, String algorithmType, String ldpType, ObjectMapper objectMapper, TimeProvider timeProvider, Optional<String> kid) {
        super(keycloakSession, keyId, algorithmType);
        this.timeProvider = timeProvider;
        this.keyId = kid.orElse(keyId);
        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        // set the configured kid if present.
        if (kid.isPresent()) {
            // we need to clone the key first, to not change the kid of the original key so that the next request still can find it.
            signingKey = signingKey.cloneKey();
            signingKey.setKid(keyId);
        }
        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);

        linkedDataCryptographicSuite = switch (ldpType) {
            case Ed255192018Suite.PROOF_TYPE ->
                    new Ed255192018Suite(objectMapper, signatureProvider.signer(signingKey));
            default -> throw new SigningServiceException(String.format("Proof Type %s is not supported.", ldpType));
        };
    }

    @Override
    public VerifiableCredential signCredential(VerifiableCredential verifiableCredential) {
        return addProof(verifiableCredential);
    }

    // add the signed proof to the credential.
    private VerifiableCredential addProof(VerifiableCredential verifiableCredential) {

        byte[] signature = linkedDataCryptographicSuite.getSignature(verifiableCredential);

        LdProof ldProof = new LdProof();
        ldProof.setProofPurpose("assertionMethod");
        ldProof.setType(linkedDataCryptographicSuite.getProofType());
        ldProof.setCreated(Date.from(Instant.ofEpochSecond(timeProvider.currentTimeSeconds())));
        ldProof.setVerificationMethod(keyId);

        try {
            var proofValue = Base64.encodeBytes(signature, Base64.URL_SAFE);
            ldProof.setProofValue(proofValue);
            verifiableCredential.setAdditionalProperties("proof", ldProof);
            return verifiableCredential;
        } catch (IOException e) {
            throw new SigningServiceException("Was not able to encode the signature.", e);
        }
    }
}