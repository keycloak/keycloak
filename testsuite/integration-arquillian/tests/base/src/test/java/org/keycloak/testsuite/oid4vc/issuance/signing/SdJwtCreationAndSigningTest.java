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
 *
 */

package org.keycloak.testsuite.oid4vc.issuance.signing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.ECDSASignatureVerifierContext;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.sdjwt.ClaimVerifier;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.UndisclosedClaim;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.util.JsonSerialization;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pascal Knueppel
 * @since 13.11.2025
 */
public class SdJwtCreationAndSigningTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testCreateSdJwtWithoutKeybindingAndNoSignature() throws Exception {

        final long iat = Instant.now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        final long nbf = Instant.now().minus(5, ChronoUnit.SECONDS).getEpochSecond();
        final long exp = Instant.now().plus(60, ChronoUnit.SECONDS).getEpochSecond();

        String disclosurePayload = """
            {
              "given_name": "Carlos",
              "family_name": "Norris"
            }
            """;
        ObjectNode disclosures = JsonSerialization.readValue(disclosurePayload, ObjectNode.class);
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                                                      .withUndisclosedClaim("given_name", "123456789")
                                                      .withUndisclosedClaim("family_name", "987654321")
                                                      .build();
        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder()
                                                         /* body */
                                                         .withClaims(disclosures, disclosureSpec)
                                                         .withIat(iat)
                                                         .withNbf(nbf)
                                                         .withExp(exp)
                                                         .build();

        SdJwt sdJwt = SdJwt.builder()
                           .withIssuerSignedJwt(issuerSignedJWT)
                           .build();

        // validate object content
        {
            Assert.assertEquals(Format.SD_JWT_VC, sdJwt.getIssuerSignedJWT().getJwsHeader().getType());
            Assert.assertEquals(1,
                                JsonSerialization.mapper.convertValue(sdJwt.getIssuerSignedJWT().getJwsHeader(),
                                                                      ObjectNode.class).size());

            Assert.assertEquals(iat, sdJwt.getIssuerSignedJWT().getPayload().get("iat").longValue());
            Assert.assertEquals(nbf, sdJwt.getIssuerSignedJWT().getPayload().get("nbf").longValue());
            Assert.assertEquals(exp, sdJwt.getIssuerSignedJWT().getPayload().get("exp").longValue());
            Assert.assertEquals("sha-256",
                                sdJwt.getIssuerSignedJWT()
                                     .getPayload()
                                     .get(IssuerSignedJWT.CLAIM_NAME_SD_HASH_ALGORITHM)
                                     .textValue());

            List<String> disclosureHashes = sdJwt.getClaims()
                                                 .stream()
                                                 .flatMap(sdJwtClaim -> {
                                                     return sdJwtClaim.getDisclosureStrings().stream();
                                                 })
                                                 .map(b64String -> {
                                                     return SdJwtUtils.hashAndBase64EncodeNoPad(b64String,
                                                                                                JavaAlgorithm.SHA256);
                                                 })
                                                 .toList();
            List<String> decoyHashes = sdJwt.getIssuerSignedJWT()
                                            .getDecoyClaims()
                                            .stream()
                                            .map(decoy -> decoy.getDisclosureDigest(JavaAlgorithm.SHA256))
                                            .toList();
            List<String> expectedSdHashes = new ArrayList<>(disclosureHashes);
            expectedSdHashes.addAll(decoyHashes);
            expectedSdHashes = expectedSdHashes.stream().sorted().toList();
            JsonNode actualSdHashNode = sdJwt.getIssuerSignedJWT()
                                             .getPayload()
                                             .get(IssuerSignedJWT.CLAIM_NAME_SELECTIVE_DISCLOSURE);
            List<String> actualSdHashes = new ArrayList<>();
            for (JsonNode jsonNode : actualSdHashNode) {
                actualSdHashes.add(jsonNode.textValue());
            }
            actualSdHashes = actualSdHashes.stream().sorted().toList();
            Assert.assertEquals(expectedSdHashes, actualSdHashes);
            Assert.assertEquals(5, sdJwt.getIssuerSignedJWT().getPayload().size());
        }

        // make sure default ClaimVerifiers succeed
        {
            ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
            Assert.assertEquals(3, claimVerifier.getVerifiers().size());
            try {
                claimVerifier.verifyClaims(sdJwt.getIssuerSignedJWT().getPayload());
            } catch (VerificationException e) {
                throw new RuntimeException("Verification should have succeeded", e);
            }
        }

        final String sdJwtString = sdJwt.toSdJwtString();
        int disclosureStart = sdJwtString.indexOf(SdJwt.DELIMITER);
        int disclosureEnd = sdJwtString.lastIndexOf(SdJwt.DELIMITER);

        // validate applied disclosures
        {
            String disclosureString = sdJwtString.substring(disclosureStart + 1, disclosureEnd);
            String[] disclosureParts = disclosureString.split(SdJwt.DELIMITER);
            Assert.assertEquals(2, disclosureParts.length);
            List<String> sortedExpectedDisclosures = sdJwt.getIssuerSignedJWT()
                                                          .getDisclosureClaims()
                                                          .stream()
                                                          .filter(disclosure -> {
                                                              return disclosure instanceof UndisclosedClaim;
                                                          })
                                                          .map(UndisclosedClaim.class::cast)
                                                          .flatMap(disclosure -> {
                                                              return disclosure.getDisclosureStrings().stream();
                                                          })
                                                          .sorted(String::compareTo)
                                                          .toList();
            List<String> sortedActualDisclosures = Arrays.stream(disclosureParts).sorted(String::compareTo).toList();
            Assert.assertEquals(sortedExpectedDisclosures, sortedActualDisclosures);
        }
    }

    @Test
    public void testCreateSdJwtWithKeybindingJwt() throws Exception {
        final String authorizationServerUrl = "https://example.com";

        KeyWrapper issuerKeyPair = toKeyWrapper(createEcKey());
        JWK issuerJwk = JWKBuilder.create().ec(issuerKeyPair.getPublicKey());

        KeyWrapper holderKeyPair = toKeyWrapper(createEcKey());
        JWK holderKeybindingKey = JWKBuilder.create().ec(holderKeyPair.getPublicKey());

        SignatureSignerContext issuerSignerContext = new ECDSASignatureSignerContext(issuerKeyPair);
        SignatureSignerContext holderSignerContext = new ECDSASignatureSignerContext(holderKeyPair);

        final long iat = Instant.now().minus(10, ChronoUnit.SECONDS).getEpochSecond();
        final long nbf = Instant.now().minus(5, ChronoUnit.SECONDS).getEpochSecond();
        final long exp = Instant.now().plus(60, ChronoUnit.SECONDS).getEpochSecond();
        final String nonce = "123456789";
        final String audience = String.format("x509_san_dns:%s", authorizationServerUrl);

        KeyBindingJWT keyBindingJWT = KeyBindingJWT.builder()
                                                   /* header */
                                                   .withKid(holderKeybindingKey.getKeyId())
                                                   /* body */
                                                   .withIat(iat)
                                                   .withNbf(nbf)
                                                   .withExp(exp)
                                                   .withNonce(nonce)
                                                   .withAudience(audience)
                                                   .build();

        String disclosurePayload = """
            {
              "given_name": "Carlos",
              "family_name": "Norris"
            }
            """;
        ObjectNode disclosures = JsonSerialization.readValue(disclosurePayload, ObjectNode.class);
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                                                      .withUndisclosedClaim("given_name", "123456789")
                                                      .withUndisclosedClaim("family_name", "987654321")
                                                      .build();
        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder()
                                                         /* header */
                                                         .withKid(issuerJwk.getKeyId())
                                                         /* body */
                                                         .withClaims(disclosures, disclosureSpec)
                                                         .withKeyBinding(holderKeybindingKey)
                                                         .withIat(iat)
                                                         .withNbf(nbf)
                                                         .withExp(exp)
                                                         .build();

        SdJwt sdJwt = SdJwt.builder()
                           .withIssuerSignedJwt(issuerSignedJWT)
                           .withKeybindingJwt(keyBindingJWT)
                           .build(issuerSignerContext, holderSignerContext);

        // validate object content
        {
            Assert.assertEquals(Algorithm.ES256, sdJwt.getIssuerSignedJWT().getJwsHeader().getAlgorithm().name());
            Assert.assertEquals(Format.SD_JWT_VC, sdJwt.getIssuerSignedJWT().getJwsHeader().getType());
            Assert.assertEquals(issuerKeyPair.getKid(), sdJwt.getIssuerSignedJWT().getJwsHeader().getKeyId());
            Assert.assertEquals(3,
                                JsonSerialization.mapper.convertValue(sdJwt.getIssuerSignedJWT().getJwsHeader(),
                                                                      ObjectNode.class).size());

            ObjectNode expectedCnf = JsonNodeFactory.instance.objectNode();
            expectedCnf.set("jwk", JsonSerialization.mapper.convertValue(holderKeybindingKey, ObjectNode.class));
            Assert.assertEquals(expectedCnf,
                                sdJwt.getIssuerSignedJWT().getPayload().get("cnf"));
            Assert.assertEquals(iat, sdJwt.getIssuerSignedJWT().getPayload().get("iat").longValue());
            Assert.assertEquals(nbf, sdJwt.getIssuerSignedJWT().getPayload().get("nbf").longValue());
            Assert.assertEquals(exp, sdJwt.getIssuerSignedJWT().getPayload().get("exp").longValue());
            Assert.assertEquals("sha-256",
                                sdJwt.getIssuerSignedJWT()
                                     .getPayload()
                                     .get(IssuerSignedJWT.CLAIM_NAME_SD_HASH_ALGORITHM)
                                     .textValue());

            List<String> disclosureHashes = sdJwt.getClaims()
                                                 .stream()
                                                 .flatMap(sdJwtClaim -> {
                                                     return sdJwtClaim.getDisclosureStrings().stream();
                                                 })
                                                 .map(b64String -> {
                                                     return SdJwtUtils.hashAndBase64EncodeNoPad(b64String,
                                                                                                JavaAlgorithm.SHA256);
                                                 })
                                                 .toList();
            List<String> decoyHashes = sdJwt.getIssuerSignedJWT()
                                            .getDecoyClaims()
                                            .stream()
                                            .map(decoy -> decoy.getDisclosureDigest(JavaAlgorithm.SHA256))
                                            .toList();
            List<String> expectedSdHashes = new ArrayList<>(disclosureHashes);
            expectedSdHashes.addAll(decoyHashes);
            expectedSdHashes = expectedSdHashes.stream().sorted().toList();
            JsonNode actualSdHashNode = sdJwt.getIssuerSignedJWT()
                                             .getPayload()
                                             .get(IssuerSignedJWT.CLAIM_NAME_SELECTIVE_DISCLOSURE);
            List<String> actualSdHashes = new ArrayList<>();
            for (JsonNode jsonNode : actualSdHashNode) {
                actualSdHashes.add(jsonNode.textValue());
            }
            actualSdHashes = actualSdHashes.stream().sorted().toList();
            Assert.assertEquals(expectedSdHashes, actualSdHashes);
            Assert.assertEquals(6, sdJwt.getIssuerSignedJWT().getPayload().size());

            Assert.assertEquals(holderKeyPair.getKid(), sdJwt.getKeybindingJwt().getJwsHeader().getKeyId());
            Assert.assertEquals(KeyBindingJWT.TYP, sdJwt.getKeybindingJwt().getJwsHeader().getType());
            Assert.assertEquals(Algorithm.ES256, sdJwt.getKeybindingJwt().getJwsHeader().getAlgorithm().name());
            Assert.assertEquals(3,
                                JsonSerialization.mapper.convertValue(sdJwt.getKeybindingJwt().getJwsHeader(),
                                                                      ObjectNode.class).size());

            Assert.assertEquals(iat, sdJwt.getKeybindingJwt().getPayload().get("iat").longValue());
            Assert.assertEquals(nbf, sdJwt.getKeybindingJwt().getPayload().get("nbf").longValue());
            Assert.assertEquals(exp, sdJwt.getKeybindingJwt().getPayload().get("exp").longValue());
            Assert.assertEquals(nonce, sdJwt.getKeybindingJwt().getPayload().get("nonce").textValue());
            Assert.assertEquals(audience, sdJwt.getKeybindingJwt().getPayload().get("aud").textValue());
            // check sd_hash
            {
                List<String> parts = new ArrayList<>();
                parts.add(sdJwt.getIssuerSignedJWT().getJws());
                parts.addAll(sdJwt.getDisclosures());
                parts.add("");
                String sdHashString = String.join(SdJwt.DELIMITER, parts);
                String expectedSdHash = SdJwtUtils.hashAndBase64EncodeNoPad(sdHashString, JavaAlgorithm.SHA256);
                Assert.assertEquals(expectedSdHash, sdJwt.getKeybindingJwt().getPayload().get("sd_hash").textValue());
            }
            Assert.assertEquals(6, sdJwt.getKeybindingJwt().getPayload().size());
        }

        // make sure default ClaimVerifiers succeed
        {
            ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
            Assert.assertEquals(3, claimVerifier.getVerifiers().size());
            try {
                claimVerifier.verifyClaims(sdJwt.getIssuerSignedJWT().getPayload());
            } catch (VerificationException e) {
                throw new RuntimeException("Verification should have succeeded", e);
            }
            try {
                claimVerifier.verifyClaims(sdJwt.getKeybindingJwt().getPayload());
            } catch (VerificationException e) {
                throw new RuntimeException("Verification should have succeeded", e);
            }
        }

        final String sdJwtString = sdJwt.toSdJwtString();
        int disclosureStart = sdJwtString.indexOf(SdJwt.DELIMITER);
        int disclosureEnd = sdJwtString.lastIndexOf(SdJwt.DELIMITER);

        // validate applied disclosures
        {
            String disclosureString = sdJwtString.substring(disclosureStart + 1, disclosureEnd);
            String[] disclosureParts = disclosureString.split(SdJwt.DELIMITER);
            Assert.assertEquals(2, disclosureParts.length);
            List<String> sortedExpectedDisclosures = sdJwt.getIssuerSignedJWT()
                                                          .getDisclosureClaims()
                                                          .stream()
                                                          .filter(disclosure -> {
                                                              return disclosure instanceof UndisclosedClaim;
                                                          })
                                                          .map(UndisclosedClaim.class::cast)
                                                          .flatMap(disclosure -> {
                                                              return disclosure.getDisclosureStrings().stream();
                                                          })
                                                          .sorted(String::compareTo)
                                                          .toList();
            List<String> sortedActualDisclosures = Arrays.stream(disclosureParts).sorted(String::compareTo).toList();
            Assert.assertEquals(sortedExpectedDisclosures, sortedActualDisclosures);
        }

        // validate applied signatures
        {

            {
                SignatureVerifierContext issuerVerifier = new ECDSASignatureVerifierContext(issuerKeyPair);
                String issuerSignedJwtString = sdJwtString.substring(0, disclosureStart);
                JWSInput issuerToken = new JWSInput(issuerSignedJwtString);
                issuerVerifier.verify(issuerToken.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8),
                                      issuerToken.getSignature());
            }

            {
                SignatureVerifierContext holderVerifier = new ECDSASignatureVerifierContext(holderKeyPair);
                String keybindingJwtString = sdJwtString.substring(disclosureEnd + 1);
                JWSInput keybindingToken = new JWSInput(keybindingJwtString);
                holderVerifier.verify(keybindingToken.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8),
                                      keybindingToken.getSignature());
            }
        }
    }

    public KeyPair createEcKey() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp521r1"), new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public KeyWrapper toKeyWrapper(KeyPair keyPair) {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        keyWrapper.setAlgorithm(Algorithm.ES256);
        keyWrapper.setPrivateKey(keyPair.getPrivate());
        keyWrapper.setPublicKey(keyPair.getPublic());
        keyWrapper.setType(keyPair.getPublic().getAlgorithm());
        return keyWrapper;
    }
}
