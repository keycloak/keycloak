package org.keycloak.tests.oid4vc.issuance.signing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.LDCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.issuance.signing.SdJwtCredentialSigner;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;
import static org.keycloak.OID4VCConstants.SDJWT_DELIMITER;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class SdJwtCredentialSignerTest extends OID4VCTest {

    @InjectRunOnServer
    private RunOnServerClient runOnServer;

    @Test
    public void testUnsupportedCredentialBody() {
        try {
            withCausePropagation(() -> runOnServer.run(session -> new SdJwtCredentialSigner(session).signCredential(
                    new LDCredentialBody(getTestCredential(Map.of())),
                    new CredentialBuildConfig()
            )));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testUnsupportedAlgorithm() {
        try {
            withCausePropagation(() -> runOnServer.run(session ->
                    testSignSDJwtCredential(
                            session,
                            getKeyIdFromSession(session),
                            null,
                            "unsupported-algorithm",
                            Map.of(),
                            0,
                            List.of())));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testFailIfNoKey() {
        try {
            withCausePropagation(() -> runOnServer.run(session ->
                    testSignSDJwtCredential(
                            session,
                            "no-such-key",
                            null,
                            Algorithm.RS256,
                            Map.of(),
                            0,
                            List.of())));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testFailWhenSigningCertificateIsSelfSigned() {
        try {
            withCausePropagation(() -> runOnServer.run(session -> {
                KeyWrapper keyWrapper = getKeyFromSession(session);
                X509Certificate selfSigned = CertificateUtils.generateV1SelfSignedCertificate(
                        new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()),
                        "SelfSignedSigningCert");
                keyWrapper.setCertificate(selfSigned);
                keyWrapper.setCertificateChain(List.of(selfSigned));

                CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                        .setCredentialIssuer(TEST_DID.toString())
                        .setCredentialType("https://credentials.example.com/test-credential")
                        .setTokenJwsType("example+sd-jwt")
                        .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                        .setNumberOfDecoys(0)
                        .setSdJwtVisibleClaims(List.of())
                        .setSigningKeyId(keyWrapper.getKid())
                        .setSigningAlgorithm(Algorithm.RS256);

                SdJwtCredentialSigner signer = new SdJwtCredentialSigner(session);
                SdJwtCredentialBody body = new SdJwtCredentialBuilder()
                        .buildCredentialBody(getTestCredential(Map.of()), credentialBuildConfig);
                signer.signCredential(body, credentialBuildConfig);
            }));
            fail("Expected CredentialSignerException");
        } catch (Throwable t) {
            assertTrue(t instanceof CredentialSignerException);
        }
    }

    @Test
    public void testRsaSignedCredentialWithClaims() {
        runOnServer.run(session ->
                testSignSDJwtCredential(
                        session,
                        getKeyIdFromSession(session),
                        null,
                        Algorithm.RS256,
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c")),
                        0,
                        List.of()));
    }

    @Test
    public void testRsaSignedCredentialWithVisibleClaims() {
        runOnServer.run(session ->
                testSignSDJwtCredential(
                        session,
                        getKeyIdFromSession(session),
                        null,
                        Algorithm.RS256,
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c")),
                        0,
                        List.of("test")));
    }

    @Test
    public void testRsaSignedCredentialWithClaimsAndDecoys() {
        runOnServer.run(session ->
                testSignSDJwtCredential(
                        session,
                        getKeyIdFromSession(session),
                        null,
                        Algorithm.RS256,
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c")),
                        6,
                        List.of()));
    }

    @Test
    public void testRsaSignedCredentialWithKeyId() {
        runOnServer.run(session ->
                testSignSDJwtCredential(
                        session,
                        getKeyIdFromSession(session),
                        "did:web:test.org#key-id",
                        Algorithm.RS256,
                        Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                                "test", "test",
                                "arrayClaim", List.of("a", "b", "c")),
                        0,
                        List.of()));
    }

    @Test
    public void testRsaSignedCredentialWithoutAdditionalClaims() {
        runOnServer.run(session ->
                testSignSDJwtCredential(
                        session,
                        getKeyIdFromSession(session),
                        null,
                        Algorithm.RS256,
                        Map.of(),
                        0,
                        List.of()));
    }

    @Test
    public void testIssuedSdJwtContainsX5cHeader() throws Exception {
        runOnServer.run(session -> {
            String sdJwtString = issueSignedSdJwtForHeaderValidation(session);
            SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtString);
            JsonWebToken jwt = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().getJws(), JsonWebToken.class).getToken();

            assertNotNull(jwt, "Issued SD-JWT should be parseable");
            assertNotNull(sdJwtVP.getIssuerSignedJWT().getJwsHeader().getX5c(), "x5c must be present");
            assertFalse(sdJwtVP.getIssuerSignedJWT().getJwsHeader().getX5c().isEmpty(), "x5c must not be empty");
        });
    }

    @Test
    public void testIssuedSdJwtX5cDoesNotContainSelfSignedTrustAnchor() throws Exception {
        runOnServer.run(session -> {
            String sdJwtString = issueSignedSdJwtForHeaderValidation(session);
            SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtString);
            List<String> x5c = sdJwtVP.getIssuerSignedJWT().getJwsHeader().getX5c();
            assertNotNull(x5c, "x5c must be present");
            assertFalse(x5c.isEmpty(), "x5c must not be empty");

            for (String certDerB64 : x5c) {
                X509Certificate cert = decodeDerBase64Certificate(certDerB64);
                assertTrue(!cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal()),
                        "x5c must not contain self-signed trust anchor certificate");
            }
        });
    }

    private static String issueSignedSdJwtForHeaderValidation(KeycloakSession session) {
        KeyWrapper keyWrapper = getKeyFromSession(session);
        ensureHaipCompliantCertificateChain(keyWrapper);

        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_DID.toString())
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType("example+sd-jwt")
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(0)
                .setSdJwtVisibleClaims(List.of())
                .setSigningKeyId(keyWrapper.getKid())
                .setSigningAlgorithm(Algorithm.RS256);

        VerifiableCredential testCredential = getTestCredential(Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID())));
        SdJwtCredentialBody credentialBody = new SdJwtCredentialBuilder().buildCredentialBody(testCredential, credentialBuildConfig);
        return new SdJwtCredentialSigner(session).signCredential(credentialBody, credentialBuildConfig);
    }

    public static void testSignSDJwtCredential(KeycloakSession session, String signingKeyId, String overrideKeyId, String
            algorithm, Map<String, Object> claims, int decoys, List<String> visibleClaims) {
        KeyWrapper keyWrapper = getKeyFromSession(session);
        ensureHaipCompliantCertificateChain(keyWrapper);

        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(TEST_DID.toString())
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType("example+sd-jwt")
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(decoys)
                .setSdJwtVisibleClaims(visibleClaims)
                .setSigningKeyId(signingKeyId)
                .setSigningAlgorithm(algorithm)
                .setOverrideKeyId(overrideKeyId);

        SdJwtCredentialSigner sdJwtCredentialSigner = new SdJwtCredentialSigner(session);

        VerifiableCredential testCredential = getTestCredential(claims);
        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);

        String sdJwt = sdJwtCredentialSigner.signCredential(sdJwtCredentialBody, credentialBuildConfig);

        SignatureVerifierContext verifierContext = null;
        switch (algorithm) {
            case Algorithm.ES256:
                verifierContext = new ServerECDSASignatureVerifierContext(keyWrapper);
                break;
            case Algorithm.RS256:
                verifierContext = new AsymmetricSignatureVerifierContext(keyWrapper);
                break;
            default:
                fail("Algorithm not supported.");
        }
        String[] splittedSdToken = sdJwt.split(SDJWT_DELIMITER);
        String[] splittedToken = splittedSdToken[0].split("\\.");

        String jwt = new StringJoiner(".")
                .add(splittedToken[0])
                .add(splittedToken[1])
                .add(splittedToken[2])
                .toString();
        TokenVerifier<JsonWebToken> verifier = TokenVerifier
                .create(jwt, JsonWebToken.class)
                .verifierContext(verifierContext);
        verifier.publicKey((PublicKey) keyWrapper.getPublicKey());
        try {
            verifier.verify();
            JsonWebToken theToken = verifier.getToken();

            assertEquals(TEST_DID.toString(), theToken.getIssuer(), "The issuer should be set in the token.");
            assertEquals("https://credentials.example.com/test-credential", theToken.getOtherClaims().get("vct"), "The type should be included");
            List<String> sds = (List<String>) theToken.getOtherClaims().get(CLAIM_NAME_SD);
            if (sds != null && !sds.isEmpty()) {
                assertEquals(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM, theToken.getOtherClaims().get(CLAIM_NAME_SD_HASH_ALGORITHM), "The algorithm should be included");
            }
            List<String> disclosed = Arrays.asList(splittedSdToken).subList(1, splittedSdToken.length);
            int numSds = sds != null ? sds.size() : 0;
            assertEquals(disclosed.size() + (decoys == 0 ? decoys + SdJwt.DEFAULT_NUMBER_OF_DECOYS : decoys),
                    numSds,
                    "All undisclosed claims and decoys should be provided.");
            verifyDisclosures(sds, disclosed);
            visibleClaims
                    .forEach(vc -> assertTrue(theToken.getOtherClaims().containsKey(vc), "The visible claims should be present within the token."));
        } catch (VerificationException e) {
            fail("The credential should successfully be verified.");
        }
    }

    private static void verifyDisclosures(List<String> undisclosed, List<String> disclosedList) {
        disclosedList.stream()
                .map(disclosed -> new String(Base64.getUrlDecoder().decode(disclosed)))
                .map(disclosedString -> {
                    try {
                        return JsonSerialization.mapper.readValue(disclosedString, List.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(dl -> new DisclosedClaim((String) dl.get(0), (String) dl.get(1), dl.get(2)))
                .forEach(dc -> assertTrue(undisclosed.contains(dc.getHash()), "Every disclosure claim should be provided in the undisclosures."));
    }

    private static void ensureHaipCompliantCertificateChain(KeyWrapper keyWrapper) {
        List<X509Certificate> existingChain = keyWrapper.getCertificateChain();
        if (existingChain != null && !existingChain.isEmpty()) {
            X509Certificate signingCert = existingChain.get(0);
            if (signingCert != null
                    && !signingCert.getSubjectX500Principal().equals(signingCert.getIssuerX500Principal())) {
                return;
            }
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair caKeyPair = kpg.generateKeyPair();
            X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "Test CA");

            KeyPair leafKeyPair = new KeyPair(
                    (PublicKey) keyWrapper.getPublicKey(),
                    (PrivateKey) keyWrapper.getPrivateKey()
            );
            X509Certificate leafCert = CertificateUtils.generateV3Certificate(
                    leafKeyPair,
                    caKeyPair.getPrivate(),
                    caCert,
                    "TestKey"
            );

            keyWrapper.setCertificateChain(List.of(leafCert, caCert));
            keyWrapper.setCertificate(leafCert);
        } catch (Exception e) {
            fail("Failed to prepare HAIP-compliant certificate chain: " + e.getMessage());
        }
    }

    static class DisclosedClaim {
        private final String salt;
        private final String key;
        private final Object value;
        private final String hash;

        DisclosedClaim(String salt, String key, Object value) {
            this.salt = salt;
            this.key = key;
            this.value = value;
            this.hash = createHash(salt, key, value);
        }

        private String createHash(String salt, String key, Object value) {
            try {
                return SdJwtUtils.encodeNoPad(
                        HashUtils.hash(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM,
                                SdJwtUtils.encodeNoPad(
                                        SdJwtUtils.printJsonArray(List.of(salt, key, value).toArray()).getBytes()).getBytes()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public String getHash() {
            return hash;
        }
    }

    private static X509Certificate decodeDerBase64Certificate(String certificateDerBase64) {
        try {
            byte[] certBytes = Base64.getDecoder().decode(certificateDerBase64);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode x5c certificate", e);
        }
    }
}
