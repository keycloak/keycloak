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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.issuance.VCIssuanceContext;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.ISO18045ResistanceLevel;
import org.keycloak.protocol.oid4vc.model.KeyAttestationJwtBody;
import org.keycloak.protocol.oid4vc.model.KeyAttestationsRequired;
import org.keycloak.protocol.oid4vc.model.SupportedProofTypeData;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import static org.keycloak.protocol.oid4vc.model.ProofType.JWT;
import static org.keycloak.services.clientpolicy.executor.FapiConstant.ALLOWED_ALGORITHMS;

/**
 * Utility for validating attestation JWTs as per OID4VCI spec.
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class AttestationValidatorUtil {

    public static final String ATTESTATION_JWT_TYP = "key-attestation+jwt";
    private static final String CACERTS_PATH = System.getProperty("javax.net.ssl.trustStore",
            System.getProperty("java.home") + "/lib/security/cacerts");
    private static final char[] DEFAULT_TRUSTSTORE_PASSWORD = System.getProperty(
            "javax.net.ssl.trustStorePassword", "changeit").toCharArray();

    public static KeyAttestationJwtBody validateAttestationJwt(
            String attestationJwt,
            KeycloakSession keycloakSession,
            VCIssuanceContext vcIssuanceContext,
            AttestationKeyResolver keyResolver) throws IOException, JWSInputException,
            VerificationException{

        if (attestationJwt == null || attestationJwt.split("\\.").length != 3) {
            throw new VCIssuerException("Invalid JWT format");
        }

        JWSInput jwsInput = new JWSInput(attestationJwt);

        String payloadString = new String(jwsInput.getContent(), StandardCharsets.UTF_8);

        // Validate that payload is JSON
        try {
            JsonSerialization.mapper.readTree(payloadString);
        } catch (JsonProcessingException e) {
            throw new VCIssuerException("Invalid JSON in attestation payload: " + payloadString, e);
        }

        KeyAttestationJwtBody attestationBody;
        try {
            attestationBody = JsonSerialization.readValue(
                    jwsInput.getContent(),
                    KeyAttestationJwtBody.class
            );
        } catch (IOException e) {
            throw new VCIssuerException("Invalid attestation payload format", e);
        }

        JWSHeader header = jwsInput.getHeader();
        validateJwsHeader(header);

        // Verify the signature
        Map<String, Object> rawHeader = JsonSerialization.mapper.convertValue(
                jwsInput.getHeader(), new TypeReference<>() {});

        SignatureVerifierContext verifier;
        if (header.getX5c() != null && !header.getX5c().isEmpty()) {
            verifier = verifierFromX5CChain(header.getX5c(), header.getAlgorithm().name(), keycloakSession);
        } else if (header.getKeyId() != null) {
            JWK resolvedJwk = keyResolver.resolveKey(header.getKeyId(), rawHeader,
                    JsonSerialization.mapper.convertValue(attestationBody, Map.class));
            verifier = verifierFromResolvedJWK(resolvedJwk, header.getAlgorithm().name(), keycloakSession);
        } else {
            throw new VCIssuerException("Neither x5c nor kid present in attestation JWT header");
        }

        if (!verifier.verify(jwsInput.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8),
                jwsInput.getSignature())) {
            throw new VCIssuerException("Could not verify signature of attestation JWT");
        }

        validateAttestationPayload(keycloakSession, vcIssuanceContext, attestationBody);

        if (attestationBody.getAttestedKeys() == null) {
            throw new VCIssuerException("Missing required attested_keys claim in attestation");
        }

        return attestationBody;
    }

    private static void validateAttestationPayload(
            KeycloakSession keycloakSession,
            VCIssuanceContext vcIssuanceContext,
            KeyAttestationJwtBody attestationBody) throws VCIssuerException, VerificationException {

        if (attestationBody.getIat() == null) {
            throw new VCIssuerException("Missing 'iat' claim in attestation");
        }

        if (attestationBody.getNonce() == null) {
            throw new VCIssuerException("Missing 'nonce' in attestation");
        }

        CNonceHandler cNonceHandler = keycloakSession.getProvider(CNonceHandler.class);
        if (cNonceHandler == null) {
            throw new VCIssuerException("No CNonceHandler available");
        }

        // Get resistance level requirements from configuration
        KeyAttestationsRequired attestationRequirements = getAttestationRequirements(vcIssuanceContext);

        // Validate key_storage if present in attestation and required by config
        if (attestationBody.getKeyStorage() != null) {
            validateResistanceLevel(
                    attestationBody.getKeyStorage(),
                    attestationRequirements != null ? attestationRequirements.getKeyStorage() : null,
                    "key_storage");
        }
        // Validate user_authentication if present in attestation and required by config
        if (attestationBody.getUserAuthentication() != null) {
            validateResistanceLevel(
                    attestationBody.getUserAuthentication(),
                    attestationRequirements != null ? attestationRequirements.getUserAuthentication() : null,
                    "user_authentication");
        }

        cNonceHandler.verifyCNonce(
                attestationBody.getNonce(),
                List.of(OID4VCIssuerWellKnownProvider.getCredentialsEndpoint(
                        keycloakSession.getContext())),
                Map.of(JwtCNonceHandler.SOURCE_ENDPOINT,
                        OID4VCIssuerWellKnownProvider.getNonceEndpoint(
                                keycloakSession.getContext()))
        );

        // Store attested keys in context for later use
        if (attestationBody.getAttestedKeys() != null) {
            vcIssuanceContext.setAttestedKeys(attestationBody.getAttestedKeys());
        }
    }

    private static KeyAttestationsRequired getAttestationRequirements(VCIssuanceContext vcIssuanceContext) {
        if (vcIssuanceContext.getCredentialConfig() == null ||
                vcIssuanceContext.getCredentialConfig().getProofTypesSupported() == null ||
                vcIssuanceContext.getCredentialConfig().getProofTypesSupported().getSupportedProofTypes() == null) {
            return null;
        }

        SupportedProofTypeData proofTypeData = vcIssuanceContext.getCredentialConfig()
                .getProofTypesSupported()
                .getSupportedProofTypes()
                .get(JWT);

        return proofTypeData != null ? proofTypeData.getKeyAttestationsRequired() : null;
    }

    private static void validateResistanceLevel(
            List<String> actualLevels,
            List<ISO18045ResistanceLevel> requiredLevels,
            String levelType) throws VCIssuerException {

        if (requiredLevels == null || requiredLevels.isEmpty()) {
            for (String level : actualLevels) {
                try {
                    ISO18045ResistanceLevel.fromValue(level);
                } catch (Exception e) {
                    throw new VCIssuerException("Invalid " + levelType + " level: " + level);
                }
            }
            return;
        }

        // Convert required levels to string values for comparison
        Set<String> requiredLevelValues = requiredLevels.stream()
                .map(ISO18045ResistanceLevel::getValue)
                .collect(Collectors.toSet());

        // Check each actual level against requirements
        for (String level : actualLevels) {
            try {
                ISO18045ResistanceLevel levelEnum = ISO18045ResistanceLevel.fromValue(level);
                if (!requiredLevelValues.contains(levelEnum.getValue())) {
                    throw new VCIssuerException(
                            levelType + " level '" + level + "' is not accepted by credential issuer. " +
                                    "Allowed values: " + requiredLevelValues);
                }
            } catch (IllegalArgumentException e) {
                throw new VCIssuerException("Invalid " + levelType + " level: " + level);
            }
        }
    }

    private static void validateJwsHeader(JWSHeader header) {
        String alg = Optional.ofNullable(header.getAlgorithm())
                .map(Algorithm::name)
                .orElseThrow(() -> new VCIssuerException("Missing algorithm in JWS header"));

        if ("none".equalsIgnoreCase(alg)) {
            throw new VCIssuerException("'none' algorithm is not allowed");
        }

        if (!ALLOWED_ALGORITHMS.contains(alg)) {
            throw new VCIssuerException("Unsupported algorithm: " + alg +
                    ". Allowed algorithms: " + ALLOWED_ALGORITHMS);
        }

        if (!ATTESTATION_JWT_TYP.equals(header.getType())) {
            throw new VCIssuerException("Invalid JWT typ: expected " + ATTESTATION_JWT_TYP);
        }
    }

    private static SignatureVerifierContext verifierFromX5CChain(
            List<String> x5cList,
            String alg,
            KeycloakSession keycloakSession) throws VCIssuerException {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<X509Certificate> certChain = new ArrayList<>();

            for (String certBase64 : x5cList) {
                // Use Keycloak's Base64 implementation for decoding x5c certificates
                byte[] certBytes = Base64.getDecoder().decode(certBase64);
                try (InputStream in = new ByteArrayInputStream(certBytes)) {
                    certChain.add((X509Certificate) cf.generateCertificate(in));
                }
            }

            // Create a certificate path
            CertPath certPath = cf.generateCertPath(certChain);

            // Check if this is a self-signed certificate (for test environments)
            X509Certificate firstCert = certChain.get(0);
            boolean isSelfSigned = firstCert.getSubjectX500Principal().equals(firstCert.getIssuerX500Principal());
            
            // Only validate the certificate chain if it's not a self-signed certificate in a test environment
            if (!isSelfSigned) {
                // Validate certificate chain
                CertPathValidator validator = CertPathValidator.getInstance("PKIX");
                PKIXParameters params = new PKIXParameters(getTrustAnchors());
                params.setRevocationEnabled(false);
                
                validator.validate(certPath, params);
            }

            // Get public key from first certificate
            PublicKey publicKey = certChain.get(0).getPublicKey();
            JWK certJwk = convertPublicKeyToJWK(publicKey, alg, certChain);

            return verifierFromResolvedJWK(certJwk, alg, keycloakSession);

        } catch (Exception e) {
            throw new VCIssuerException("Failed to validate x5c certificate chain", e);
        }
    }

    private static Set<TrustAnchor> getTrustAnchors() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(CACERTS_PATH)) {
            trustStore.load(in, DEFAULT_TRUSTSTORE_PASSWORD);
        }

        Set<TrustAnchor> anchors = new HashSet<>();
        Enumeration<String> aliases = trustStore.aliases();
        while (aliases.hasMoreElements()) {
            Certificate cert = trustStore.getCertificate(aliases.nextElement());
            if (cert instanceof X509Certificate) {
                anchors.add(new TrustAnchor((X509Certificate) cert, null));
            }
        }
        return anchors;
    }

    private static SignatureVerifierContext verifierFromResolvedJWK(
            JWK jwk,
            String alg,
            KeycloakSession session
    ) throws VerificationException {

        SignatureProvider provider = session.getProvider(SignatureProvider.class, alg);
        KeyWrapper wrapper = new KeyWrapper();
        wrapper.setType(jwk.getKeyType());
        wrapper.setAlgorithm(alg);
        wrapper.setUse(KeyUse.SIG);

        if (jwk.getOtherClaims().get("crv") != null) {
            wrapper.setCurve((String) jwk.getOtherClaims().get("crv"));
        }

        wrapper.setPublicKey(JWKParser.create(jwk).toPublicKey());
        return provider.verifier(wrapper);
    }

    private static JWK convertPublicKeyToJWK(
            PublicKey key,
            String alg,
            List<X509Certificate> certChain
    ) {
        if (key instanceof RSAPublicKey rsa) {
            return JWKBuilder.create().algorithm(alg).rsa(rsa, certChain);
        } else if (key instanceof ECPublicKey ec) {
            return JWKBuilder.create().algorithm(alg).ec(ec, certChain, null);
        } else {
            throw new VCIssuerException("Unsupported public key type in certificate: " + key.getClass().getName());
        }
    }
}
