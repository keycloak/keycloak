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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.Proof;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.ProofTypeJWT;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.AccessToken;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * {@link VerifiableCredentialsSigningService} implementing the SD_JWT_VC format. It returns a String, containing
 * the signed SD-JWT
 * <p>
 * {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
 * {@see https://www.ietf.org/archive/id/draft-fett-oauth-selective-disclosure-jwt-02.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class SdJwtSigningService extends SigningService<String> {

    private static final Logger LOGGER = Logger.getLogger(SdJwtSigningService.class);

    private static final String ISSUER_CLAIM ="iss";
    private static final String NOT_BEFORE_CLAIM ="nbf";
    private static final String VERIFIABLE_CREDENTIAL_TYPE_CLAIM = "vct";
    private static final String CREDENTIAL_ID_CLAIM = "jti";
    private static final String CNF_CLAIM = "cnf";
    private static final String JWK_CLAIM = "jwk";
    public static final String PROOF_JWT_TYP="openid4vci-proof+jwt";

    private final ObjectMapper objectMapper;
    private final SignatureSignerContext signatureSignerContext;
    private final TimeProvider timeProvider;
    private final String tokenType;
    private final String hashAlgorithm;
    private final int decoys;
    private final List<String> visibleClaims;
    protected final String issuerDid;

    private final String vcConfigId;

    public SdJwtSigningService(KeycloakSession keycloakSession, ObjectMapper objectMapper, String keyId, String algorithmType, String tokenType, String hashAlgorithm, String issuerDid, int decoys, List<String> visibleClaims, TimeProvider timeProvider, Optional<String> kid, String vcConfigId) {
        super(keycloakSession, keyId, Format.SD_JWT_VC, algorithmType);
        this.objectMapper = objectMapper;
        this.issuerDid = issuerDid;
        this.timeProvider = timeProvider;
        this.tokenType = tokenType;
        this.hashAlgorithm = hashAlgorithm;
        this.decoys = decoys;
        this.visibleClaims = visibleClaims;
        this.vcConfigId = vcConfigId;
        // Will return the active key if key id is null.
        KeyWrapper signingKey = getKey(keyId, algorithmType);
        if (signingKey == null) {
            throw new SigningServiceException(String.format("No key for id %s and algorithm %s available.", keyId, algorithmType));
        }
        // @Francis: keyId header can be confusing if there is any key rotation, as key ids have to be immutable. It can lead
        // to different keys being exposed under the same id.
        // set the configured kid if present.
        if (kid.isPresent()) {
            // we need to clone the key first, to not change the kid of the original key so that the next request still can find it.
            signingKey = signingKey.cloneKey();
            signingKey.setKid(keyId);
        }

        SignatureProvider signatureProvider = keycloakSession.getProvider(SignatureProvider.class, algorithmType);
        signatureSignerContext = signatureProvider.signer(signingKey);

        LOGGER.debugf("Successfully initiated the SD-JWT Signing Service with algorithm %s.", algorithmType);
    }

    @Override
    public String signCredential(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {

        JWK jwk = null;
        try {
            // null returned is a valid result. Means no key binding will be included.
            jwk = validateProof(vcIssuanceContext);
        } catch (JWSInputException | VerificationException | IOException e) {
            throw new VCIssuerException("Can not verify proof", e);
        }

        VerifiableCredential verifiableCredential = vcIssuanceContext.getVerifiableCredential();
        DisclosureSpec.Builder disclosureSpecBuilder = DisclosureSpec.builder();
        CredentialSubject credentialSubject = verifiableCredential.getCredentialSubject();
        JsonNode claimSet = objectMapper.valueToTree(credentialSubject);
        // put all claims into the disclosure spec, except the one to be kept visible
        credentialSubject.getClaims()
                .entrySet()
                .stream()
                .filter(entry -> !visibleClaims.contains(entry.getKey()))
                .forEach(entry -> {
                    if (entry instanceof List<?> listValue) {
                        IntStream.range(0, listValue.size())
                                .forEach(i -> disclosureSpecBuilder.withUndisclosedArrayElt(entry.getKey(), i, SdJwtUtils.randomSalt()));
                    } else {
                        disclosureSpecBuilder.withUndisclosedClaim(entry.getKey(), SdJwtUtils.randomSalt());
                    }
                });

        // add the configured number of decoys
        if (decoys != 0) {
            IntStream.range(0, decoys)
                    .forEach(i -> disclosureSpecBuilder.withDecoyClaim(SdJwtUtils.randomSalt()));
        }

        ObjectNode rootNode = claimSet.withObject("");
        rootNode.put(ISSUER_CLAIM, issuerDid);

        // nbf, iat and exp are all optional. So need to be set by a protocol mapper if needed
        // see: https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-03.html#name-registered-jwt-claims
        if (verifiableCredential.getType() == null || verifiableCredential.getType().size() != 1) {
            throw new SigningServiceException("SD-JWT only supports single type credentials.");
        }
        rootNode.put(VERIFIABLE_CREDENTIAL_TYPE_CLAIM, verifiableCredential.getType().get(0));
        rootNode.put(CREDENTIAL_ID_CLAIM, JwtSigningService.createCredentialId(verifiableCredential));

        // add the key binding if any
        if(jwk!=null){
            rootNode.putPOJO(CNF_CLAIM, Map.of(JWK_CLAIM, jwk));
        }

        SdJwt sdJwt = SdJwt.builder()
                .withDisclosureSpec(disclosureSpecBuilder.build())
                .withClaimSet(claimSet)
                .withSigner(signatureSignerContext)
                .withHashAlgorithm(hashAlgorithm)
                .withJwsType(tokenType)
                .build();

        return sdJwt.toSdJwtString();
    }

    @Override
    public String locator() {
        return VerifiableCredentialsSigningService.locator(format,vcConfigId);
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
    private JWK validateProof(VCIssuanceContext vcIssuanceContext) throws VCIssuerException, JWSInputException, VerificationException, IOException {

        Optional<Proof> optionalProof = getProofFromContext(vcIssuanceContext);

        if (!optionalProof.isPresent()) {
            return null; // No proof support
        }

        // Check key binding config for jwt. Only type supported.
        checkCryptographicKeyBinding(vcIssuanceContext);

        JWSInput jwsInput = getJwsInput(optionalProof.get());
        JWSHeader jwsHeader = jwsInput.getHeader();
        validateJwsHeader(vcIssuanceContext, jwsHeader);

        JWK jwk = Optional.ofNullable(jwsHeader.getKey())
                .orElseThrow(() -> new VCIssuerException("Missing binding key. Make sure provided JWT contains the jwk jwsHeader claim."));

        // Parsing the Proof as an access token shall work, as a proof is a strict subset of an access token.
        AccessToken proofPayload = JsonSerialization.readValue(jwsInput.getContent(), AccessToken.class);
        validateProofPayload(vcIssuanceContext, proofPayload);

        SignatureVerifierContext signatureVerifierContext = getVerifier(jwk, jwsHeader.getAlgorithm().name());
        if(signatureVerifierContext==null){
            throw new VCIssuerException("No verifier configured for " +jwsHeader.getAlgorithm());
        }
        if (!signatureVerifierContext.verify(jwsInput.getEncodedSignatureInput().getBytes("UTF-8"), jwsInput.getSignature())) {
            throw new VCIssuerException("Could not verify provided proof");
        }

        return jwk;
    }

    private void checkCryptographicKeyBinding(VCIssuanceContext vcIssuanceContext){
        // Make sure we are dealing with a jwk proof.
        if (vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported() == null ||
                !vcIssuanceContext.getCredentialConfig().getCryptographicBindingMethodsSupported().contains("jwk")) {
            throw new IllegalStateException("This SD-JWT implementation only supports jwk as cryptographic binding method");
        }
    }

    private Optional<Proof> getProofFromContext(VCIssuanceContext vcIssuanceContext) throws VCIssuerException {
        return Optional.ofNullable(vcIssuanceContext.getCredentialConfig())
                .map(config -> config.getProofTypesSupported())
                .flatMap(proofTypesSupported -> {
                    if (proofTypesSupported == null) {
                        LOGGER.debugf("No proof support. Will skip proof validation.");
                        return Optional.empty();
                    }

                    ProofTypeJWT jwt = Optional.ofNullable(proofTypesSupported.getJwt())
                            .orElseThrow(() -> new VCIssuerException("SD-JWT supports only jwt proof type."));

                    Proof proof = Optional.ofNullable(vcIssuanceContext.getCredentialRequest().getProof())
                            .orElseThrow(() -> new VCIssuerException("Credential configuration requires a proof of type: " + ProofType.JWT.getValue()));

                    if (!Objects.equals(proof.getProofType(), ProofType.JWT)) {
                        throw new VCIssuerException("Wrong proof type");
                    }

                    return Optional.of(proof);
                });
    }

    private JWSInput getJwsInput(Proof proof) throws JWSInputException {
        return new JWSInput(proof.getJwt());
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
                .map(config -> config.getProofTypesSupported())
                .map(proofTypesSupported -> proofTypesSupported.getJwt())
                .map(jwt -> jwt.getProofSigningAlgValuesSupported())
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

    private void validateProofPayload(VCIssuanceContext vcIssuanceContext, AccessToken proofPayload) throws VCIssuerException {
        // azp is the id of the client, as mentioned in the access token used to request the credential.
        // Token provided from user is obtained with a clientId that support the oidc login protocol.
        // oid4vci client doesn't. But it is the client needed at the credential endpoint.
//        String azp = vcIssuanceContext.getAuthResult().getToken().getIssuedFor();
//        Optional.ofNullable(proofPayload.getIssuer())
//                .filter(proofIssuer -> Objects.equals(azp, proofIssuer))
//                .orElseThrow(() -> new VCIssuerException("Issuer claim must be null for preauthorized code else the clientId of the client making the request: " + azp));

        // The issuer is the token / credential is the audience of the proof
        String credentialIssuer = vcIssuanceContext.getVerifiableCredential().getIssuer().toString();
        Optional.ofNullable(proofPayload.getAudience()) // Ensure null-safety with Optional
                .map(Arrays::asList) // Convert to List<String>
                .filter(audiences -> audiences.contains(credentialIssuer)) // Check if the issuer is in the audience list
                .orElseThrow(() -> new VCIssuerException(
                        "Proof not produced for this audience. Audience claim must be: " + credentialIssuer + " but are " + Arrays.asList(proofPayload.getAudience())));

        // Validate mandatory iat.
        // I do not understand the rationale behind requiring a issue time if we are not checking expiration.
        Optional.ofNullable(proofPayload.getIat())
                .orElseThrow(() -> new VCIssuerException("Missing proof issuing time. iat claim must be provided."));

        // Check cNonce matches.
        // If the token endpoint provides a c_nonce, we would like this:
        // - stored in the access token
        // - having the same validity as the access token.
        Optional.ofNullable(vcIssuanceContext.getAuthResult().getToken().getNonce())
                        .ifPresent(
                                cNonce -> {
                                    Optional.ofNullable(proofPayload.getNonce())
                                            .filter(nonce -> Objects.equals(cNonce, nonce))
                                            .orElseThrow(() -> new VCIssuerException("Missing or wrong nonce value. Please provide nonce returned by the issuer if any."));

                                    // We expect the expiration to be identical to the token expiration. We assume token expiration has been checked by AuthManager,
                                    // So no_op
                                }
                        );

    }

}
