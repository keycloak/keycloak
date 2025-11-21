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

import java.rmi.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.SdJwtVP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_ISSUER;
import static org.keycloak.OID4VCConstants.JWT_VC_ISSUER_END_POINT;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public abstract class JwtVcMetadataTrustedSdJwtIssuerTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void shouldResolveIssuerVerifyingKeys() throws Exception {
        String issuerUri = "https://issuer.example.com";
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUri, mockHttpDataFetcher());

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
        List<SignatureVerifierContext> keys = trustedIssuer
                .resolveIssuerVerifyingKeys(issuerSignedJWT);

        // There three keys exposed on the metadata endpoint.
        assertEquals(3, keys.size());
    }

    @Test
    public void shouldResolveKeys_WhenIssuerTrustedOnRegexPattern() throws Exception {
        Pattern issuerUriRegex = Pattern.compile("https://.*\\.example\\.com");
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUriRegex, mockHttpDataFetcher());

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
        trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT);
    }

    @Test
    public void shouldResolveKeys_WhenJwtSpecifiesKid() throws VerificationException, JsonProcessingException {
        String issuerUri = "https://issuer.example.com";
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUri, mockHttpDataFetcher());

        // This JWT specifies a key ID in its header
        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt("sdjwt/s20.1-sdjwt+kb--explicit-kid.txt");

        // Act
        List<SignatureVerifierContext> keys = trustedIssuer
                .resolveIssuerVerifyingKeys(issuerSignedJWT);

        // Despite three keys exposed on the metadata endpoint,
        // only the key matching the JWT's kid is resolved.
        assertEquals(1, keys.size());
        assertEquals("doc-signer-05-25-2022", keys.get(0).getKid());
    }

    @Test
    public void shouldRejectNonHttpsIssuerURIs() {
        String issuerUri = "http://issuer.example.com"; // not https

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new JwtVcMetadataTrustedSdJwtIssuer(issuerUri, mockHttpDataFetcher())
        );

        assertThat(exception.getMessage(), endsWith("HTTPS URI required to retrieve JWT VC Issuer Metadata"));
    }

    @Test
    public void shouldRejectNonHttpsIssuerURIs_EvenIfIssuerBuiltWithRegexPattern() throws JsonProcessingException {
        Pattern issuerUriRegex = Pattern.compile(".*\\.example\\.com");
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUriRegex, mockHttpDataFetcher());

        String issuerUri = "http://issuer.example.com"; // not https
        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwtWithIssuer(issuerUri);
        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertThat(exception.getMessage(), endsWith("HTTPS URI required to retrieve JWT VC Issuer Metadata"));
    }

    @Test
    public void shouldRejectJwtsWithUnexpectedIssuerClaims() throws JsonProcessingException {
        String regex = "https://.*\\.example\\.com";
        Pattern issuerUriRegex = Pattern.compile(regex);
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUriRegex, mockHttpDataFetcher());

        String issuerUri = "https://trial.authlete.com"; // does not match the regex above
        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwtWithIssuer(issuerUri);
        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertThat(exception.getMessage(),
                endsWith(String.format("Unexpected Issuer URI claim. Expected=/%s/, Got=%s", regex, issuerUri)));
    }

    @Test
    public void shouldFailOnInvalidJwkExposed() throws JsonProcessingException {
        String issuerUri = "https://issuer.example.com";

        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", issuerUri);
        metadata.set("jwks", SdJwtUtils.mapper.readTree(
                "{\n" +
                        "  \"keys\": [\n" +
                        "    {\n" +
                        "      \"kty\": \"EC\",\n" +
                        "      \"x\": \"invalid\",\n" +
                        "      \"y\": \"invalid\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        ));

        genericTestShouldFail(
                exampleIssuerSignedJwt(),
                mockHttpDataFetcherWithMetadata(issuerUri, metadata),
                "A potential JWK was retrieved but found invalid",
                "Unsupported or invalid JWK"
        );
    }

    @Test
    public void shouldFailOnUnexpectedIssuerExposed() throws JsonProcessingException {
        String issuerUri = "https://issuer.example.com";

        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", "https://another-issuer.example.com");

        genericTestShouldFail(
                exampleIssuerSignedJwt(),
                mockHttpDataFetcherWithMetadata(issuerUri, metadata),
                "Unexpected metadata's issuer",
                null
        );
    }

    @Test
    public void shouldFailOnMalformedJwtVcMetadataExposed() throws JsonProcessingException {
        String issuerUri = "https://issuer.example.com";

        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        // This is malformed because the issuer must be a string
        metadata.set("issuer", SdJwtUtils.mapper.readTree("{}"));

        genericTestShouldFail(
                exampleIssuerSignedJwt(),
                mockHttpDataFetcherWithMetadata(issuerUri, metadata),
                "Failed to parse exposed JWT VC Metadata",
                null
        );
    }

    @Test
    public void shouldFailOnMalformedJwksExposed() throws JsonProcessingException {
        List<JsonNode> malformedJwks = Arrays.asList(
                SdJwtUtils.mapper.readTree("[]"),
                SdJwtUtils.mapper.readTree("{\"keys\": {}}")
        );

        for (JsonNode jwks : malformedJwks) {
            String issuerUri = "https://issuer.example.com";

            ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
            metadata.put("issuer", issuerUri);
            metadata.put("jwks_uri", "https://issuer.example.com/api/vci/jwks");

            genericTestShouldFail(
                    exampleIssuerSignedJwt(),
                    mockHttpDataFetcherWithMetadataAndJwks(issuerUri, metadata, jwks),
                    "Failed to parse exposed JWKS",
                    null
            );
        }
    }

    @Test
    public void shouldFailOnMissingJwksOnMetadataEndpoint() throws JsonProcessingException {
        String issuerUri = "https://issuer.example.com";

        // There are no means to resolve JWKS with these metadata
        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", issuerUri);

        genericTestShouldFail(
                exampleIssuerSignedJwt(),
                mockHttpDataFetcherWithMetadata(issuerUri, metadata),
                String.format("Could not resolve issuer JWKs with URI: %s", issuerUri),
                null
        );
    }

    @Test
    public void shouldFailOnEmptyPublishedJwkList() throws Exception {
        // This JWKS to publish has an empty list of keys, which is unexpected.
        JsonNode jwks = SdJwtUtils.mapper.readTree("{\"keys\": []}");

        // JWT VC Metadata
        String issuerUri = "https://issuer.example.com";
        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", issuerUri);
        metadata.set("jwks", jwks);

        // Act and assert
        genericTestShouldFail(
                exampleIssuerSignedJwt(),
                mockHttpDataFetcherWithMetadataAndJwks(issuerUri, metadata, jwks),
                String.format("Issuer JWKs were unexpectedly resolved to an empty list. Issuer URI: %s", issuerUri),
                null
        );
    }

    @Test
    public void shouldFailOnNoPublishedJwkMatchingJwtKid() throws Exception {
        // Alter kid fields of all JWKs to publish, so as none is a match
        JsonNode jwks = exampleJwks();
        for (JsonNode jwk : jwks.get("keys")) {
            ((ObjectNode) jwk).put("kid", jwk.get("kid").asText() + "-wont-match");
        }

        // JWT VC Metadata
        String issuerUri = "https://issuer.example.com";
        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", issuerUri);
        metadata.set("jwks", jwks);

        // This JWT specifies a key ID in its header
        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt("sdjwt/s20.1-sdjwt+kb--explicit-kid.txt");
        String kid = issuerSignedJWT.getHeader().getKeyId();

        // Act and assert
        genericTestShouldFail(
                issuerSignedJWT,
                mockHttpDataFetcherWithMetadataAndJwks(issuerUri, metadata, jwks),
                String.format("No published JWK was found to match kid: %s", kid),
                null
        );
    }

    @Test
    public void shouldFailOnPublishedJwksWithDuplicateKid() throws JsonProcessingException {
        // This JWT specifies a key ID in its header

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt("sdjwt/s20.1-sdjwt+kb--explicit-kid.txt");

        // Set the same kid to all JWKs to publish, which is problematic

        String kid = issuerSignedJWT.getHeader().getKeyId();
        JsonNode jwks = exampleJwks();
        for (JsonNode jwk : jwks.get("keys")) {
            ((ObjectNode) jwk).put("kid", kid);
        }

        // JWT VC Metadata

        String issuerUri = "https://issuer.example.com";
        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", issuerUri);
        metadata.set("jwks", jwks);

        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUri, mockHttpDataFetcherWithMetadata(issuerUri, metadata));

        // Act and assert

        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertThat(exception.getMessage(),
                endsWith(String.format("Cannot choose between multiple exposed JWKs with same kid: %s", kid)));
    }

    @Test
    public void shouldFailOnIOErrorWhileFetchingMetadata() throws JsonProcessingException {
        String issuerUri = "https://issuer.example.com";

        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", issuerUri);

        HttpDataFetcher mockFetcher = mockHttpDataFetcherWithMetadata(
                // HTTP can only mock an unrelated issuer
                "https://another-issuer.example.com",
                metadata
        );

        genericTestShouldFail(
                exampleIssuerSignedJwt(),
                mockFetcher,
                String.format("Could not fetch data from URI: %s", issuerUri),
                null
        );
    }

    private void genericTestShouldFail(
            IssuerSignedJWT issuerSignedJWT,
            HttpDataFetcher mockFetcher,
            String errorMessage,
            String causeErrorMessage
    ) {
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerSignedJWT.getPayload().get(CLAIM_NAME_ISSUER).asText(),
                mockFetcher
        );

        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertTrue(exception.getMessage().contains(errorMessage));
        if (causeErrorMessage != null) {
            assertTrue(exception.getCause().getMessage().contains(causeErrorMessage));
        }
    }

    private IssuerSignedJWT exampleIssuerSignedJwt() {
        return exampleIssuerSignedJwt("sdjwt/s20.1-sdjwt+kb.txt");
    }

    private IssuerSignedJWT exampleIssuerSignedJwt(String sdJwtVector) {
        return exampleIssuerSignedJwt(sdJwtVector, "https://issuer.example.com");
    }

    private IssuerSignedJWT exampleIssuerSignedJwtWithIssuer(String issuerUri) {
        return exampleIssuerSignedJwt("sdjwt/s20.1-sdjwt+kb.txt", issuerUri);
    }

    private IssuerSignedJWT exampleIssuerSignedJwt(String sdJwtVector, String issuerUri) {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), sdJwtVector);
        IssuerSignedJWT issuerSignedJWT = SdJwtVP.of(sdJwtVPString).getIssuerSignedJWT();
        ((ObjectNode) issuerSignedJWT.getPayload()).put("iss", issuerUri);
        return issuerSignedJWT;
    }

    private JsonNode exampleJwks() throws JsonProcessingException {
        return SdJwtUtils.mapper.readTree(
                TestUtils.readFileAsString(getClass(),
                        "sdjwt/s30.1-jwt-vc-metadata-jwks.json"
                )
        );
    }

    private HttpDataFetcher mockHttpDataFetcher() throws JsonProcessingException {
        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", "https://issuer.example.com");
        metadata.put("jwks_uri", "https://issuer.example.com/api/vci/jwks");

        return mockHttpDataFetcherWithMetadata(
                metadata.get("issuer").asText(),
                metadata
        );
    }

    private HttpDataFetcher mockHttpDataFetcherWithMetadata(
            String issuer, JsonNode metadata
    ) throws JsonProcessingException {
        return mockHttpDataFetcherWithMetadataAndJwks(issuer, metadata, exampleJwks());
    }

    private HttpDataFetcher mockHttpDataFetcherWithMetadataAndJwks(
            String issuer, JsonNode metadata, JsonNode jwks
    ) {
        return uri -> {
            if (!uri.startsWith(issuer)) {
                throw new UnknownHostException("Unavailable URI");
            }

            if (uri.endsWith(JWT_VC_ISSUER_END_POINT)) {
                return metadata;
            } else if (uri.endsWith("/api/vci/jwks")) {
                return jwks;
            } else {
                throw new UnknownHostException("Unavailable URI");
            }
        };
    }
}
