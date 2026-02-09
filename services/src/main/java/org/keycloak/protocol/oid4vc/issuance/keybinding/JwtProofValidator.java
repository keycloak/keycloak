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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * Validates the conformance and authenticity of presented JWT proofs.
 *
 * @see "https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-jwt-proof-type"
 */
public class JwtProofValidator extends AbstractProofValidator {

    private static final Logger LOGGER = Logger.getLogger(JwtProofValidator.class);

    public static final String PROOF_JWT_TYP = "openid4vci-proof+jwt";
    private static final String CRYPTOGRAPHIC_BINDING_METHOD_JWK = "jwk";
    private static final String KEY_ATTESTATION_CLAIM = "key_attestation";
    private final AttestationKeyResolver keyResolver;

    public JwtProofValidator(KeycloakSession keycloakSession, AttestationKeyResolver keyResolver) {
        super(keycloakSession);
        this.keyResolver = keyResolver;
    }

    @Override
    public String getProofType() {
        return ProofType.JWT;
    }

    @Override
    public List<JWK> validateProof(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {
        try {
            return validateJwtProof(vcIssuanceContext);
        } catch (JWSInputException | VerificationException | IOException e) {
            throw new VCIssuerException("Could not validate JWT proof", e);
        }
    }

    /*
     * Validates a proof provided by the client if any.
     *
     * Returns null if there is no need to include a key binding in the credential
     *
     * Return the JWK to be included as key binding in the JWK if the provided proof was correctly validated
     *
     * @param vcIssuanceContext
     * @return
     * @throws VCIssuerException
     * @throws JWSInputException
     * @throws VerificationException
     * @throws IllegalStateException: is credential type badly configured
     * @throws IOException
     */
    private List<JWK> validateJwtProof(VCIssuanceContext vcIssuanceContext) throws VCIssuerException, JWSInputException, VerificationException, IOException {

        Optional<List<String>> optionalProof = getProofFromContext(vcIssuanceContext);

        if (optionalProof.isEmpty() || optionalProof.get().isEmpty()) {
            return null; // No proof support
        }

        List<String> jwtProofs = optionalProof.get();

        // Check key binding config for jwt. Only type supported.
        checkCryptographicKeyBinding(vcIssuanceContext);

        // Validate all JWT proofs in the array
        List<JWK> validJwks = new ArrayList<>();

        for (int i = 0; i < jwtProofs.size(); i++) {
            String jwt = jwtProofs.get(i);
            try {
                JWK jwk = validateSingleJwtProof(vcIssuanceContext, jwt);
                validJwks.add(jwk);
                LOGGER.debugf("Successfully validated JWT proof at index %d", i);
            } catch (VCIssuerException e) {
                // If any proof fails validation, throw the exception
                throw new VCIssuerException(String.format("Failed to validate JWT proof at index %d: %s", i, e.getMessage()), e);
            }
        }

        if (validJwks.isEmpty()) {
            throw new VCIssuerException("No valid JWT proof found in the proofs array");
        }

        LOGGER.debugf("Successfully validated %d JWT proofs", validJwks.size());
        return validJwks;
    }

    private JWK validateSingleJwtProof(VCIssuanceContext vcIssuanceContext, String jwt) throws VCIssuerException, JWSInputException, VerificationException, IOException {
        JWSInput jwsInput = getJwsInput(jwt);
        JWSHeader jwsHeader = jwsInput.getHeader();
        validateJwsHeader(vcIssuanceContext, jwsHeader);

        // Handle both JWK and kid cases for the proof key
        JWK jwk;
        if (jwsHeader.getKey() != null) {
            jwk = jwsHeader.getKey();
        } else if (jwsHeader.getKeyId() != null) {
            // For kid case, we need to parse the raw header to check for key_attestation
            Map<String, Object> headerClaims = JsonSerialization.mapper.convertValue(jwsHeader,
                    new TypeReference<>() {
                    });

            if (!headerClaims.containsKey(KEY_ATTESTATION_CLAIM)) {
                throw new VCIssuerException("Key ID provided but no key_attestation in header to resolve it");
            }

            Object keyAttestation = headerClaims.get(KEY_ATTESTATION_CLAIM);
            if (keyAttestation == null) {
                throw new VCIssuerException("The 'key_attestation' claim is present in JWT header but is null.");
            }

            List<JWK> attestedKeys = AttestationValidatorUtil.validateAttestationJwt(
                    keyAttestation.toString(), keycloakSession, vcIssuanceContext, keyResolver).getAttestedKeys();

            // Resolve key from attestation using kid
            jwk = attestedKeys.stream()
                    .filter(k -> jwsHeader.getKeyId().equals(k.getKeyId()))
                    .findFirst()
                    .orElseThrow(() -> new VCIssuerException(
                            "No attested key found matching kid: " + jwsHeader.getKeyId()));
        } else {
            throw new VCIssuerException("Missing binding key. JWT must contain either jwk or kid in header.");
        }

        // Rest of the validation
        AccessToken proofPayload = JsonSerialization.readValue(jwsInput.getContent(), AccessToken.class);
        validateProofPayload(vcIssuanceContext, proofPayload);

        SignatureVerifierContext signatureVerifierContext = getVerifier(jwk, jwsHeader.getAlgorithm().name());
        if (signatureVerifierContext == null) {
            throw new VCIssuerException("No verifier configured for " + jwsHeader.getAlgorithm());
        }
        if (!signatureVerifierContext.verify(jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8),
                jwsInput.getSignature())) {
            throw new VCIssuerException("Could not verify signature of provided proof");
        }

        return jwk;
    }

    private void checkCryptographicKeyBinding(VCIssuanceContext vcIssuanceContext) {
        // Make sure we are dealing with a jwk proof.
        if (vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported() == null ||
                !vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported()
                        .contains(CRYPTOGRAPHIC_BINDING_METHOD_JWK)) {
            throw new IllegalStateException("This SD-JWT implementation only supports jwk as cryptographic binding method");
        }
    }

    private Optional<List<String>> getProofFromContext(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {
        return Optional.ofNullable(vcIssuanceContext.getCredentialConfig())
                .map(SupportedCredentialConfiguration::getProofTypesSupported)
                .flatMap(proofTypesSupported -> {
                    Optional.ofNullable(proofTypesSupported.getSupportedProofTypes().get("jwt"))
                            .orElseThrow(() -> new VCIssuerException("SD-JWT supports only jwt proof type."));

                    Proofs proofs = vcIssuanceContext.getCredentialRequest().getProofs();
                    if (proofs == null || proofs.getJwt() == null || proofs.getJwt().isEmpty()) {
                        throw new VCIssuerException("Credential configuration requires a proof of type: " + ProofType.JWT);
                    }

                    return Optional.of(proofs.getJwt());
                });
    }

    private JWSInput getJwsInput(String jwt) throws JWSInputException {
        return new JWSInput(jwt);
    }

    /**
     * As we limit accepted algorithm to the ones listed by the issuer, we can omit checking for "none"
     * The Algorithm enum class does not list the none value anyway.
     *
     * @param vcIssuanceContext
     * @param jwsHeader
     * @throws VCIssuerException
     */
    private void validateJwsHeader(VCIssuanceContext vcIssuanceContext, JWSHeader jwsHeader) throws VCIssuerException {
        Optional.ofNullable(jwsHeader.getAlgorithm())
                .orElseThrow(() -> new VCIssuerException("Missing jwsHeader claim alg"));

        // As we limit accepted algorithm to the ones listed by the server, we can omit checking for "none"
        // The Algorithm enum class does not list the none value anyway.
        Optional.ofNullable(vcIssuanceContext.getCredentialConfig())
                .map(SupportedCredentialConfiguration::getProofTypesSupported)
                .map(ProofTypesSupported::getSupportedProofTypes)
                .map(proofTypeData -> proofTypeData.get("jwt"))
                .map(SupportedProofTypeData::getSigningAlgorithmsSupported)
                .filter(supportedAlgs -> supportedAlgs.contains(jwsHeader.getAlgorithm().name()))
                .orElseThrow(() -> new VCIssuerException("Proof signature algorithm not supported: " + jwsHeader.getAlgorithm().name()));

        Optional.ofNullable(jwsHeader.getType())
                .filter(type -> Objects.equals(PROOF_JWT_TYP, type))
                .orElseThrow(() -> new VCIssuerException("JWT type must be: " + PROOF_JWT_TYP));

        // KeyId shall not be present alongside the jwk.
        Optional.ofNullable(jwsHeader.getKeyId())
                .ifPresent(keyId -> {
                    throw new VCIssuerException("KeyId not expected in this JWT. Use the jwk claim instead.");
                });
    }

    private void validateProofPayload(VCIssuanceContext vcIssuanceContext, AccessToken proofPayload)
            throws VCIssuerException, VerificationException {
        // azp is the id of the client, as mentioned in the access token used to request the credential.
        // Token provided from user is obtained with a clientId that support the oidc login protocol.
        // oid4vci client doesn't. But it is the client needed at the credential endpoint.
        //        String azp = vcIssuanceContext.getAuthResult().getToken().getIssuedFor();
        //        Optional.ofNullable(proofPayload.getIssuer())
        //                .filter(proofIssuer -> Objects.equals(azp, proofIssuer))
        //                .orElseThrow(() -> new VCIssuerException("Issuer claim must be null for preauthorized code else the clientId of the client making the request: " + azp));

        // The audience of the proof MUST be the Credential Issuer Identifier.
        // https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-jwt-proof-type
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(keycloakSession.getContext());
        Optional.ofNullable(proofPayload.getAudience()) // Ensure null-safety with Optional
                .map(Arrays::asList) // Convert to List<String>
                .filter(audiences -> audiences.contains(credentialIssuer)) // Check if the issuer is in the audience list
                .orElseThrow(() -> new VCIssuerException(
                        "Proof not produced for this audience. Audience claim must be: " + credentialIssuer + " but are " + Arrays.asList(proofPayload.getAudience())));

        // Validate mandatory iat.
        // I do not understand the rationale behind requiring an issue time if we are not checking expiration.
        Optional.ofNullable(proofPayload.getIat())
                .orElseThrow(() -> new VCIssuerException("Missing proof issuing time. iat claim must be provided."));

        KeycloakContext keycloakContext = keycloakSession.getContext();
        CNonceHandler cNonceHandler = keycloakSession.getProvider(CNonceHandler.class);
        try {
            cNonceHandler.verifyCNonce(proofPayload.getNonce(),
                    List.of(OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(keycloakContext)),
                    Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                            OID4VCIssuerWellKnownProvider.getNonceEndpoint(keycloakContext)));
        } catch (VerificationException e) {
            throw new VCIssuerException(ErrorType.INVALID_NONCE,
                    "The proofs parameter in the Credential Request uses an invalid nonce", e);
        }
    }
}
