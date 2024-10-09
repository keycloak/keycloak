package org.keycloak.sdjwt.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.SdJwtVP;

import java.io.IOException;
import java.rmi.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
    public void shouldResolveIssuerVerifyingKeys() throws VerificationException {
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
    public void shouldResolveKeys_WhenIssuerTrustedOnRegexPattern() throws VerificationException {
        Pattern issuerUriRegex = Pattern.compile("https://.*\\.example\\.com");
        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUriRegex, mockHttpDataFetcher());

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
        trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT);
    }

    @Test
    public void shouldResolveKeys_WhenJwtSpecifiesKid() throws VerificationException {
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
    public void shouldRejectNonHttpsIssuerURIs_EvenIfIssuerBuiltWithRegexPattern() {
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
    public void shouldRejectJwtsWithUnexpectedIssuerClaims() {
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

        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUri, mockHttpDataFetcherWithMetadata(issuerUri, metadata));

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertThat(exception.getMessage(),
                endsWith("A potential JWK was retrieved but found invalid"));
    }

    @Test
    public void shouldFailOnUnexpectedIssuerExposed() {
        String issuerUri = "https://issuer.example.com";

        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", "https://another-issuer.example.com");

        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUri, mockHttpDataFetcherWithMetadata(issuerUri, metadata));

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertTrue(exception.getMessage().contains("Unexpected metadata's issuer"));
    }

    @Test
    public void shouldFailOnMissingOrMalformedJwksOnMetadataEndpoint() throws JsonProcessingException {
        List<JsonNode> malformedJwks = Arrays.asList(
                null,
                SdJwtUtils.mapper.readTree("{}"),
                SdJwtUtils.mapper.readTree("{\"keys\": {}}")
        );

        for (JsonNode jwks : malformedJwks) {
            String issuerUri = "https://issuer.example.com";

            ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
            metadata.put("issuer", issuerUri);
            metadata.set("jwks", jwks);

            TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                    issuerUri, mockHttpDataFetcherWithMetadata(issuerUri, metadata));

            IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
            VerificationException exception = assertThrows(VerificationException.class,
                    () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
            );

            assertThat(exception.getMessage(),
                    endsWith(String.format("Could not resolve issuer JWKs with URI: %s", issuerUri)));
        }
    }

    @Test
    public void shouldFailOnIOErrorWhileFetchingMetadata() {
        String issuerUri = "https://issuer.example.com";

        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", "https://issuer.example.com");

        TrustedSdJwtIssuer trustedIssuer = new JwtVcMetadataTrustedSdJwtIssuer(
                issuerUri,
                // HTTP can only mock an unrelated issuer
                mockHttpDataFetcherWithMetadata("https://another-issuer.example.com", metadata)
        );

        IssuerSignedJWT issuerSignedJWT = exampleIssuerSignedJwt();
        VerificationException exception = assertThrows(VerificationException.class,
                () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT)
        );

        assertTrue(exception.getMessage()
                .contains(String.format("Could not fetch data from URI: %s", issuerUri)));
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

    private HttpDataFetcher mockHttpDataFetcher() {
        ObjectNode metadata = SdJwtUtils.mapper.createObjectNode();
        metadata.put("issuer", "https://issuer.example.com");
        metadata.put("jwks_uri", "https://issuer.example.com/api/vci/jwks");

        return mockHttpDataFetcherWithMetadata(
                metadata.get("issuer").asText(),
                metadata
        );
    }

    private HttpDataFetcher mockHttpDataFetcherWithMetadata(String issuer, JsonNode metadata) {
        return new HttpDataFetcher() {
            @Override
            public JsonNode fetchJsonData(String uri) throws IOException {
                if (!uri.startsWith(issuer)) {
                    throw new UnknownHostException("Unavailable URI");
                }

                if (uri.endsWith("/.well-known/jwt-vc-issuer")) {
                    return metadata;
                } else if (uri.endsWith("/api/vci/jwks")) {
                    return SdJwtUtils.mapper.readTree(
                            TestUtils.readFileAsString(getClass(),
                                    "sdjwt/s30.1-jwt-vc-metadata-jwks.json")
                    );
                } else {
                    throw new UnknownHostException("Unavailable URI");
                }
            }
        };
    }
}
