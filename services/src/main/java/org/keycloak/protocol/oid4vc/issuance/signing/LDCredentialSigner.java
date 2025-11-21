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

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.LDCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.signing.vcdm.Ed255192018Suite;
import org.keycloak.protocol.oid4vc.issuance.signing.vcdm.LinkedDataCryptographicSuite;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oid4vc.model.vcdm.LdProof;

import org.jboss.logging.Logger;

/**
 * {@link CredentialSigner} implementing the JWT_VC format. It returns the signed JWT-Credential as a String.
 * <p></p>
 * {@see https://identity.foundation/jwt-vc-presentation-profile/}
 */
public class LDCredentialSigner extends AbstractCredentialSigner<VerifiableCredential> {

    private static final Logger LOGGER = Logger.getLogger(LDCredentialSigner.class);

    public static final String PROOF_PURPOSE_ASSERTION = "assertionMethod";
    public static final String PROOF_KEY = "proof";

    private final TimeProvider timeProvider;

    public LDCredentialSigner(KeycloakSession keycloakSession, TimeProvider timeProvider) {
        super(keycloakSession);
        this.timeProvider = timeProvider;
    }

    @Override
    public VerifiableCredential signCredential(CredentialBody credentialBody, CredentialBuildConfig credentialBuildConfig)
            throws CredentialSignerException {
        if (!(credentialBody instanceof LDCredentialBody ldCredentialBody)) {
            throw new CredentialSignerException("Credential body unexpectedly not of type LDCredentialBody");
        }

        LOGGER.debugf("Sign credentials to ldp-vc format.");
        return addProof(
                ldCredentialBody.getVerifiableCredential(),
                credentialBuildConfig
        );
    }

    private LinkedDataCryptographicSuite getLinkedDataCryptographicSuite(CredentialBuildConfig credentialBuildConfig) {
        String ldpProofType = credentialBuildConfig.getLdpProofType();
        SignatureSignerContext signer = getSigner(credentialBuildConfig);

        if (Objects.equals(ldpProofType, Ed255192018Suite.PROOF_TYPE)) {
            return new Ed255192018Suite(signer);
        }

        throw new CredentialSignerException(String.format("Proof Type %s is not supported.", ldpProofType));
    }

    // add the signed proof to the credential.
    private VerifiableCredential addProof(
            VerifiableCredential verifiableCredential,
            CredentialBuildConfig credentialBuildConfig) {
        String keyId = Optional.ofNullable(credentialBuildConfig.getOverrideKeyId())
                .orElse(credentialBuildConfig.getSigningKeyId());

        LinkedDataCryptographicSuite suite = getLinkedDataCryptographicSuite(credentialBuildConfig);
        byte[] signature = suite.getSignature(verifiableCredential);

        LdProof ldProof = new LdProof();
        ldProof.setProofPurpose(PROOF_PURPOSE_ASSERTION);
        ldProof.setType(suite.getProofType());
        ldProof.setCreated(Date.from(Instant.ofEpochSecond(timeProvider.currentTimeSeconds())));
        ldProof.setVerificationMethod(keyId);

        try {
            var proofValue = Base64.getUrlEncoder().encodeToString(signature);
            ldProof.setProofValue(proofValue);
            verifiableCredential.setAdditionalProperties(PROOF_KEY, ldProof);
            return verifiableCredential;
        } catch (IllegalArgumentException e) {
            throw new CredentialSignerException("Was not able to encode the signature.", e);
        }
    }
}
