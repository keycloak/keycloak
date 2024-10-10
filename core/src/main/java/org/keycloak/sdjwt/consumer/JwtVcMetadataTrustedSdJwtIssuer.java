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

package org.keycloak.sdjwt.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.JwkParsingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A trusted Issuer for running SD-JWT VP verification.
 *
 * <p>
 * This implementation targets issuers exposing verifying keys on a normalized JWT VC Issuer metadata endpoint.
 * </p>
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-sd-jwt-vc-05#name-issuer-signed-jwt-verificat">
 * JWT VC Issuer Metadata
 * </a>
 */
public class JwtVcMetadataTrustedSdJwtIssuer implements TrustedSdJwtIssuer {

    private static final String JWT_VC_ISSUER_END_POINT = "/.well-known/jwt-vc-issuer";

    private final Pattern issuerUriPattern;
    private final HttpDataFetcher httpDataFetcher;

    /**
     * @param issuerUri a trusted issuer URI
     */
    public JwtVcMetadataTrustedSdJwtIssuer(String issuerUri, HttpDataFetcher httpDataFetcher) {
        try {
            validateHttpsIssuerUri(issuerUri);
        } catch (VerificationException e) {
            throw new IllegalArgumentException(e);
        }

        // Build a Regex pattern to only match the argument URI
        this.issuerUriPattern = Pattern.compile(Pattern.quote(issuerUri));

        // Assign HttpDataFetcher implementation
        this.httpDataFetcher = httpDataFetcher;
    }

    /**
     * @param issuerUriPattern a regex pattern for trusted issuer URIs
     */
    public JwtVcMetadataTrustedSdJwtIssuer(Pattern issuerUriPattern, HttpDataFetcher httpDataFetcher) {
        this.issuerUriPattern = issuerUriPattern;
        this.httpDataFetcher = httpDataFetcher;
    }

    @Override
    public List<SignatureVerifierContext> resolveIssuerVerifyingKeys(IssuerSignedJWT issuerSignedJWT)
            throws VerificationException {
        // Read iss (claim) and kid (header)
        String iss = Optional.ofNullable(issuerSignedJWT.getPayload().get("iss"))
                .map(JsonNode::asText)
                .orElse("");
        String kid = issuerSignedJWT.getHeader().getKeyId();

        // Match the read iss claim against the trusted pattern
        Matcher matcher = issuerUriPattern.matcher(iss);
        if (!matcher.matches()) {
            throw new VerificationException(String.format(
                    "Unexpected Issuer URI claim. Expected=/%s/, Got=%s",
                    issuerUriPattern.pattern(), iss
            ));
        }

        // As per specs, only HTTPS URIs are supported
        validateHttpsIssuerUri(iss);

        // Fetch and collect exposed JWKs
        List<JsonNode> jwks = new ArrayList<>();
        for (JsonNode jwk : fetchIssuerMetadata(iss)) {
            jwks.add(jwk);
        }

        // If kid specified, only consider the first matching key
        if (kid != null) {
            jwks = jwks.stream()
                    .filter(jwk -> {
                        JsonNode jwkKid = jwk.get("kid");
                        return jwkKid != null && jwkKid.asText().equals(kid);
                    })
                    .findFirst()
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }

        // Build JWSVerifier's
        List<SignatureVerifierContext> verifiers = new ArrayList<>();
        for (JsonNode jwk : jwks) {
            try {
                verifiers.add(JwkParsingUtils.convertJwkToVerifierContext(jwk));
            } catch (Exception e) {
                throw new VerificationException("A potential JWK was retrieved but found invalid");
            }
        }

        return verifiers;
    }

    private void validateHttpsIssuerUri(String issuerUri) throws VerificationException {
        if (!issuerUri.startsWith("https://")) {
            throw new VerificationException(
                    "HTTPS URI required to retrieve JWT VC Issuer Metadata"
            );
        }
    }

    private ArrayNode fetchIssuerMetadata(String issuerUri) throws VerificationException {
        // Build full URL to JWT VC metadata endpoint

        issuerUri = normalizeUri(issuerUri);
        String jwtVcIssuerUri = issuerUri
                .concat(JWT_VC_ISSUER_END_POINT); // Append well-known path

        // Fetch and validate metadata

        JsonNode issuerMetadata = fetchData(jwtVcIssuerUri);
        String exposedIssuerUri = normalizeUri(issuerMetadata.get("issuer").asText());

        if (!issuerUri.equals(exposedIssuerUri)) {
            throw new VerificationException(String.format(
                    "Unexpected metadata's issuer. Expected=%s, Got=%s",
                    issuerUri, exposedIssuerUri
            ));
        }

        // Parse metadata

        JsonNode jwksUri = issuerMetadata.get("jwks_uri");
        JsonNode jwks = issuerMetadata.get("jwks");

        if (jwks == null && jwksUri != null) {
            jwks = fetchData(jwksUri.textValue());
        }

        if (jwks == null || jwks.get("keys") == null || !jwks.get("keys").isArray()) {
            throw new VerificationException(
                    String.format("Could not resolve issuer JWKs with URI: %s", issuerUri));
        }

        return (ArrayNode) jwks.get("keys");
    }

    private JsonNode fetchData(String uri) throws VerificationException {
        try {
            return Objects.requireNonNull(httpDataFetcher.fetchJsonData(uri));
        } catch (Exception exception) {
            throw new VerificationException(
                    String.format("Could not fetch data from URI: %s", uri),
                    exception
            );
        }
    }

    private String normalizeUri(String uri) {
        // Remove any trailing slash
        return uri.replaceAll("/$", "");
    }
}
