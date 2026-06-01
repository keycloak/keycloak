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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.CryptoUtils;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.ProofTypesSupported;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
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
    // JOSE private JWK parameters across RSA/EC/OKP/oct key types. TODO: This is not very reliable and should be either removed or improved to cover the cases when other algorithms are introduced in the future
    private static final Set<String> JWK_PRIVATE_KEY_CLAIMS = Set.of("d", "p", "q", "dp", "dq", "qi", "oth", "k");
    private static final int PROOF_MAX_AGE_SECONDS = 30;
    private static final int PROOF_FUTURE_SKEW_SECONDS = 10;
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
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Could not validate JWT proof", e);
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
            JWK jwk = validateSingleJwtProof(vcIssuanceContext, jwt);
            validJwks.add(jwk);
            LOGGER.debugf("Successfully validated JWT proof at index %d", i);
        }

        if (validJwks.isEmpty()) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "No valid JWT proof found in the proofs array");
        }

        LOGGER.debugf("Successfully validated %d JWT proofs", validJwks.size());
        return validJwks;
    }

    private JWK validateSingleJwtProof(VCIssuanceContext vcIssuanceContext, String jwt) throws VCIssuerException, JWSInputException, VerificationException, IOException {
        JWSInput jwsInput = getJwsInput(jwt);
        JWSHeader jwsHeader = jwsInput.getHeader();
        validateJwsHeader(vcIssuanceContext, jwsHeader);

        // Parse raw JOSE header claims so we can resolve optional key_attestation consistently.
        Map<String, Object> headerClaims = JsonSerialization.mapper.convertValue(jwsHeader,
                new TypeReference<>() {
                });
        validateNoPrivateKeyInHeaderClaims(headerClaims);
        KeyAttestationInfo attestationInfo = resolveHeaderAttestation(vcIssuanceContext, headerClaims);

        // Handle both JWK and kid cases for the proof key
        JWK jwk;
        if (jwsHeader.getKey() != null) {
            jwk = jwsHeader.getKey();
        } else if (jwsHeader.getKeyId() != null) {
            if (attestationInfo.isPresent()) {
                List<JWK> attestedKeys = attestationInfo.attestedKeys();

                // Resolve key from attestation using kid
                jwk = attestedKeys.stream()
                        .filter(k -> jwsHeader.getKeyId().equals(k.getKeyId()))
                        .findFirst()
                        .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF,
                                "No attested key found matching kid: " + jwsHeader.getKeyId()));
            } else {
                jwk = keyResolver.resolveKey(jwsHeader.getKeyId(), headerClaims, Map.of());
                if (jwk == null) {
                    throw new VCIssuerException(ErrorType.INVALID_PROOF,
                            "No trusted key found matching kid: " + jwsHeader.getKeyId());
                }
            }
        } else if (jwsHeader.getX5c() != null && !jwsHeader.getX5c().isEmpty()) {
            jwk = AttestationValidatorUtil.resolveJwkFromValidatedX5c(jwsHeader.getX5c(), jwsHeader.getAlgorithm().name());
        } else {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Missing binding key. JWT must contain either jwk, kid, or x5c in header.");
        }

        // If a key attestation is present, proof key must be one of attested_keys.
        if (attestationInfo.isPresent()) {
            boolean attested = attestationInfo.attestedKeys().stream()
                    .anyMatch(attestedKey -> jwkMaterialEquals(attestedKey, jwk));
            if (!attested) {
                throw new VCIssuerException(ErrorType.INVALID_PROOF,
                        "JWT proof key is not included in attested_keys");
            }
        }

        // Rest of the validation
        AccessToken proofPayload = JsonSerialization.readValue(jwsInput.getContent(), AccessToken.class);
        validateProofPayload(vcIssuanceContext, proofPayload);

        SignatureVerifierContext signatureVerifierContext = getVerifier(jwk, jwsHeader.getAlgorithm().name());
        if (signatureVerifierContext == null) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "No verifier configured for " + jwsHeader.getAlgorithm());
        }
        if (!signatureVerifierContext.verify(jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8),
                jwsInput.getSignature())) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Could not verify signature of provided proof");
        }

        return jwk;
    }

    private void checkCryptographicKeyBinding(VCIssuanceContext vcIssuanceContext) {
        // If the credential configuration does not require cryptographic holder binding, the metadata will omit
        // cryptographic_binding_methods_supported and proof_types_supported. In that case, we must not enforce
        // JWT-based cryptographic binding.
        if (vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported() == null ||
                vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported().isEmpty()) {
            return;
        }

        // If binding is required, this implementation currently only supports the "jwk" method.
        if (!vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported()
                .contains(CRYPTOGRAPHIC_BINDING_METHOD_JWK)) {
            throw new IllegalStateException("This SD-JWT implementation only supports jwk as cryptographic binding method");
        }
    }

    private Optional<List<String>> getProofFromContext(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {
        SupportedCredentialConfiguration config = vcIssuanceContext.getCredentialConfig();
        if (config == null) {
            return Optional.empty();
        }
        ProofTypesSupported proofTypesSupported = config.getProofTypesSupported();
        CredentialRequest credentialRequest = vcIssuanceContext.getCredentialRequest();
        Proofs proofs = credentialRequest != null ? credentialRequest.getProofs() : null;

        // If no proof types are configured for this credential configuration, cryptographic binding is
        // not required and we must not enforce presence of proofs. However, if a JWT proof is supplied,
        // reject it explicitly rather than silently ignoring an unconfigured proof input.
        // Note: do not use Optional.map(getProofTypesSupported): a null ProofTypesSupported must still run this logic.
        if (proofTypesSupported == null
                || proofTypesSupported.getSupportedProofTypes() == null
                || proofTypesSupported.getSupportedProofTypes().isEmpty()) {
            if (proofs != null && proofs.getJwt() != null && !proofs.getJwt().isEmpty()) {
                throw new VCIssuerException(
                        ErrorType.INVALID_PROOF,
                        "Proof type " + ProofType.JWT + " is not supported for this credential configuration"
                );
            }
            return Optional.empty();
        }

        Map<String, SupportedProofTypeData> supportedProofTypes = proofTypesSupported.getSupportedProofTypes();
        Optional.ofNullable(supportedProofTypes.get(ProofType.JWT))
                .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF, "SD-JWT supports only jwt proof type."));

        // At this point, JWT is an explicitly supported proof type and must be enforced.
        if (proofs == null || proofs.getJwt() == null || proofs.getJwt().isEmpty()) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Credential configuration requires a proof of type: " + ProofType.JWT);
        }

        return Optional.of(proofs.getJwt());
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
        String alg = Optional.ofNullable(jwsHeader.getAlgorithm())
                .map(algorithm -> algorithm.name())
                .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF, "Missing jwsHeader claim alg"));
        if (!CryptoUtils.getSupportedAsymmetricSignatureAlgorithms(keycloakSession).contains(alg)) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Proof signature algorithm not supported: " + alg);
        }

        // As we limit accepted algorithm to the ones listed by the server, we can omit checking for "none"
        // The Algorithm enum class does not list the none value anyway.
        Optional.ofNullable(vcIssuanceContext.getCredentialConfig())
                .map(SupportedCredentialConfiguration::getProofTypesSupported)
                .map(ProofTypesSupported::getSupportedProofTypes)
                .map(proofTypeData -> proofTypeData.get("jwt"))
                .map(SupportedProofTypeData::getSigningAlgorithmsSupported)
                .filter(supportedAlgs -> supportedAlgs.contains(alg))
                .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF, "Proof signature algorithm not supported: " + alg));

        Optional.ofNullable(jwsHeader.getType())
                .filter(type -> Objects.equals(PROOF_JWT_TYP, type))
                .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF, "JWT type must be: " + PROOF_JWT_TYP));

        boolean hasJwk = jwsHeader.getKey() != null;
        boolean hasKid = jwsHeader.getKeyId() != null;
        boolean hasX5c = jwsHeader.getX5c() != null && !jwsHeader.getX5c().isEmpty();

        int presentKeyHeaders = (hasJwk ? 1 : 0) + (hasKid ? 1 : 0) + (hasX5c ? 1 : 0);
        if (presentKeyHeaders > 1) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Header claims kid, jwk, and x5c are mutually exclusive");
        }

        // OID4VCI F.1: trust_chain is not implemented (OpenID Federation verification); reject explicitly.
        if (jwsHeader.getOtherClaims() != null && jwsHeader.getOtherClaims().get("trust_chain") != null) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "trust_chain JOSE header is not supported");
        }

    }

    private KeyAttestationInfo resolveHeaderAttestation(VCIssuanceContext vcIssuanceContext, Map<String, Object> headerClaims)
            throws JWSInputException, VerificationException {
        if (!headerClaims.containsKey(KEY_ATTESTATION_CLAIM)) {
            return KeyAttestationInfo.absent();
        }

        Object keyAttestation = headerClaims.get(KEY_ATTESTATION_CLAIM);
        if (keyAttestation == null) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "The 'key_attestation' claim is present in JWT header but is null.");
        }

        List<JWK> attestedKeys = AttestationValidatorUtil.validateAttestationJwt(
                keyAttestation.toString(),
                keycloakSession,
                vcIssuanceContext,
                keyResolver,
                true,
                ProofType.JWT).getAttestedKeys();
        if (attestedKeys == null || attestedKeys.isEmpty()) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "key_attestation does not contain attested keys");
        }

        return new KeyAttestationInfo(attestedKeys);
    }

    private record KeyAttestationInfo(List<JWK> attestedKeys) {

        static KeyAttestationInfo absent() {
            return new KeyAttestationInfo(List.of());
        }

        boolean isPresent() {
            return !attestedKeys.isEmpty();
        }
    }

    /**
     * Compare key material instead of object identity so we can correctly match keys even when kid is absent.
     */
    private boolean jwkMaterialEquals(JWK left, JWK right) {
        if (left == null || right == null) {
            return false;
        }
        if (!Objects.equals(left.getKeyType(), right.getKeyType())) {
            return false;
        }

        try {
            PublicKey leftPublicKey = JWKParser.create(left).toPublicKey();
            PublicKey rightPublicKey = JWKParser.create(right).toPublicKey();
            return Objects.equals(leftPublicKey.getAlgorithm(), rightPublicKey.getAlgorithm())
                    && Arrays.equals(leftPublicKey.getEncoded(), rightPublicKey.getEncoded());
        } catch (RuntimeException e) {
            // If one key cannot be parsed into a public key, treat as non-match and let caller fail with INVALID_PROOF.
            return false;
        }
    }

    private void validateNoPrivateKeyInHeaderClaims(Map<String, Object> headerClaims) {
        Object jwkClaim = headerClaims.get("jwk");
        if (!(jwkClaim instanceof Map<?, ?> jwkMap)) {
            return;
        }
        for (String privateClaim : JWK_PRIVATE_KEY_CLAIMS) {
            if (jwkMap.containsKey(privateClaim)) {
                throw new VCIssuerException(ErrorType.INVALID_PROOF,
                        "JWK header must not contain private key material claim: " + privateClaim);
            }
        }
    }

    private void validateProofPayload(VCIssuanceContext vcIssuanceContext, AccessToken proofPayload)
            throws VCIssuerException, VerificationException {
        AuthenticationManager.AuthResult authResult = vcIssuanceContext.getAuthResult();
        AccessToken requestToken = authResult != null ? authResult.getToken() : null;
        String expectedClientId = requestToken != null ? requestToken.getIssuedFor() : null;
        String proofIssuer = proofPayload.getIssuer();

        // OID4VCI F.1: For client-bound flows, iss is optional, but if present it must match requesting client_id.
        // For anonymous flows, iss must be omitted.
        if (expectedClientId == null || expectedClientId.isBlank()) {
            if (proofIssuer != null) {
                throw new VCIssuerException(ErrorType.INVALID_PROOF, "Issuer claim must be omitted for anonymous flow");
            }
        } else if (proofIssuer != null && !Objects.equals(expectedClientId, proofIssuer)) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "Issuer claim must be the client_id of the request: " + expectedClientId);
        }

        // The audience of the proof MUST be the Credential Issuer Identifier.
        // https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-jwt-proof-type
        String credentialIssuer = OID4VCIssuerWellKnownProvider.getIssuer(keycloakSession.getContext());
        String[] audiences = Optional.ofNullable(proofPayload.getAudience())
                .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF,
                        "Proof not produced for this audience. Audience claim must be: " + credentialIssuer + " but is missing"));
        if (audiences.length != 1 || !Objects.equals(credentialIssuer, audiences[0])) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "Proof not produced for this audience. Audience claim must be single value: " + credentialIssuer + " but are " + Arrays.asList(audiences));
        }

        // Validate mandatory iat.
        Long iat = Optional.ofNullable(proofPayload.getIat())
                .orElseThrow(() -> new VCIssuerException(ErrorType.INVALID_PROOF, "Missing proof issuing time. iat claim must be provided."));
        long now = Time.currentTime();
        if (iat < now - PROOF_MAX_AGE_SECONDS) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Proof iat is too old");
        }
        if (iat > now + PROOF_FUTURE_SKEW_SECONDS) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Proof iat is in the future beyond allowed clock skew");
        }
        if (proofPayload.getExp() != null && proofPayload.getExp() < now) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Proof has expired");
        }
        if (proofPayload.getNbf() != null && proofPayload.getNbf() > now + PROOF_FUTURE_SKEW_SECONDS) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Proof is not yet valid");
        }

        KeycloakContext keycloakContext = keycloakSession.getContext();
        CNonceHandler cNonceHandler = keycloakSession.getProvider(CNonceHandler.class);
        if (cNonceHandler == null) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "CNonce handler not configured");
        }
        try {
            cNonceHandler.verifyCNonce(proofPayload.getNonce(),
                    List.of(OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(keycloakContext)),
                    Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                            OID4VCIssuerWellKnownProvider.getNonceEndpoint(keycloakContext)));
        } catch (VerificationException e) {
            throw new VCIssuerException(ErrorType.INVALID_NONCE, e.getMessage());
        }
    }
}
