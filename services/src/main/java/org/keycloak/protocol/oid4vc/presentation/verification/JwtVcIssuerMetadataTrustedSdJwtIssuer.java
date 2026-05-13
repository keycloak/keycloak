/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.presentation.verification;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.consumer.HttpDataFetcher;
import org.keycloak.sdjwt.consumer.JwtVcMetadata;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;
import org.keycloak.services.Urls;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

class JwtVcIssuerMetadataTrustedSdJwtIssuer implements TrustedSdJwtIssuer {

    private final KeycloakSession session;
    private final HttpDataFetcher httpDataFetcher;
    private final CertificateChainValidator certificateChainValidator;

    JwtVcIssuerMetadataTrustedSdJwtIssuer(KeycloakSession session, String trustedIssuerCertificate,
                                          HttpDataFetcher httpDataFetcher, boolean allowSelfSigned) {
        this.session = session;
        this.httpDataFetcher = httpDataFetcher;
        this.certificateChainValidator = new CertificateChainValidator(trustedIssuerCertificate, allowSelfSigned);
    }

    @Override
    public List<SignatureVerifierContext> resolveIssuerVerifyingKeys(IssuerSignedJWT issuerSignedJWT)
            throws VerificationException {
        JWSHeader header = issuerSignedJWT.getJwsHeader();
        if (header != null && header.getX5c() != null && !header.getX5c().isEmpty()) {
            return List.of();
        }

        String issuer = issuer(issuerSignedJWT);
        if (isRealmIssuer(issuer)) {
            // The local realm issuer is verified with realm key material; metadata lookup is for remote JWT VC issuers.
            return List.of();
        }

        String algorithm = algorithm(issuerSignedJWT);
        URI issuerUri = validateIssuerUri(issuer);
        JwtVcMetadata metadata = fetchIssuerMetadata(issuerUri);
        if (!issuer.equals(metadata.getIssuer())) {
            throw new VerificationException("Unexpected JWT VC issuer metadata issuer");
        }

        JSONWebKeySet jwks = metadata.getJwks();
        if (jwks != null && metadata.getJwksUri() != null) {
            throw new VerificationException("JWT VC issuer metadata must not expose both jwks and jwks_uri");
        }
        if (jwks == null && metadata.getJwksUri() != null) {
            validateHttpsUri(metadata.getJwksUri());
            jwks = fetchJwks(metadata.getJwksUri());
        }
        if (jwks == null || jwks.getKeys() == null) {
            throw new VerificationException("JWT VC issuer metadata does not expose JWKS");
        }

        String kid = header != null ? header.getKeyId() : null;
        List<SignatureVerifierContext> verifiers = new ArrayList<>();
        for (JWK jwk : jwks.getKeys()) {
            if (kid != null && !kid.equals(jwk.getKeyId())) {
                continue;
            }
            if (jwk.getAlgorithm() != null && !algorithm.equals(jwk.getAlgorithm())) {
                continue;
            }

            String[] x5c = jwk.getX509CertificateChain();
            if (x5c == null || x5c.length == 0) {
                continue;
            }

            X509Certificate certificate = certificateChainValidator.validateTrustedEncodedChain(Arrays.asList(x5c));
            verifiers.add(certificateChainValidator.toVerifierContext(certificate, algorithm, jwk.getKeyId()));
        }
        if (verifiers.isEmpty()) {
            throw new VerificationException("No trusted certificate-backed JWT VC issuer key found");
        }

        return verifiers;
    }

    private JwtVcMetadata fetchIssuerMetadata(URI issuer) throws VerificationException {
        try {
            return JsonSerialization.mapper.treeToValue(
                    fetchData(buildJwtVcIssuerMetadataUri(issuer)),
                    JwtVcMetadata.class);
        } catch (Exception e) {
            throw new VerificationException("Failed to parse JWT VC issuer metadata", e);
        }
    }

    private JSONWebKeySet fetchJwks(String jwksUri) throws VerificationException {
        try {
            return JsonSerialization.mapper.treeToValue(fetchData(jwksUri), JSONWebKeySet.class);
        } catch (Exception e) {
            throw new VerificationException("Failed to parse JWT VC issuer JWKS", e);
        }
    }

    private JsonNode fetchData(String uri) throws VerificationException {
        try {
            return httpDataFetcher.fetchJsonData(uri);
        } catch (Exception e) {
            throw new VerificationException("Could not fetch data from URI: " + uri, e);
        }
    }

    private URI validateIssuerUri(String uri) throws VerificationException {
        URI parsed = validateHttpsUri(uri);
        if (parsed.getRawQuery() != null || parsed.getRawFragment() != null) {
            throw new VerificationException("JWT VC issuer URI must not contain query or fragment components");
        }
        return parsed;
    }

    private URI validateHttpsUri(String uri) throws VerificationException {
        try {
            URI parsed = URI.create(uri);
            if (!"https".equals(parsed.getScheme()) || parsed.getRawAuthority() == null || parsed.getRawAuthority().isBlank()) {
                throw new VerificationException("HTTPS URI required to retrieve JWT VC issuer metadata");
            }
            return parsed;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new VerificationException("HTTPS URI required to retrieve JWT VC issuer metadata");
        }
    }

    private String buildJwtVcIssuerMetadataUri(URI issuer) throws VerificationException {
        try {
            String issuerPath = Optional.ofNullable(issuer.getRawPath()).orElse("");
            URI metadata = new URI(
                    issuer.getScheme(),
                    issuer.getAuthority(),
                    OID4VCConstants.JWT_VC_ISSUER_END_POINT + issuerPath,
                    null,
                    null);
            return metadata.toString();
        } catch (URISyntaxException e) {
            throw new VerificationException("Invalid issuer URI", e);
        }
    }

    private String issuer(IssuerSignedJWT issuerSignedJWT) throws VerificationException {
        JsonNode issuerClaim = issuerSignedJWT.getPayload().get(OID4VCConstants.CLAIM_NAME_ISSUER);
        String issuer = issuerClaim != null ? issuerClaim.asText() : null;
        if (issuer == null || issuer.isBlank()) {
            throw new VerificationException("Missing SD-JWT issuer claim");
        }
        return issuer;
    }

    private String algorithm(IssuerSignedJWT issuerSignedJWT) throws VerificationException {
        JWSHeader header = issuerSignedJWT.getJwsHeader();
        String algorithm = header != null && header.getAlgorithm() != null ? header.getAlgorithm().name() : null;
        if (algorithm == null) {
            throw new VerificationException("Missing SD-JWT issuer signature algorithm");
        }
        return algorithm;
    }

    private boolean isRealmIssuer(String issuer) {
        if (session == null || session.getContext() == null || session.getContext().getRealm() == null
                || session.getContext().getUri() == null) {
            return false;
        }

        RealmModel realm = session.getContext().getRealm();
        String realmIssuer = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
        return realmIssuer.equals(issuer);
    }
}
