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

package org.keycloak.sdjwt;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
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
import org.keycloak.representations.IDToken;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;

/**
 * @author Pascal Knueppel
 * @since 13.11.2025
 */
public abstract class SdJwtCreationAndSigningTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testCreateSdJwtWithoutKeybindingAndNoSignature() throws Exception {

        Instant now = Instant.ofEpochSecond(Time.currentTime());
        final long iat = now.minus(10, ChronoUnit.SECONDS).getEpochSecond();
        final long nbf = now.minus(5, ChronoUnit.SECONDS).getEpochSecond();
        final long exp = now.plus(60, ChronoUnit.SECONDS).getEpochSecond();

        String disclosurePayload = "{\n" +
            "  \"given_name\": \"Carlos\",\n" +
            "  \"family_name\": \"Norris\"\n" +
            "}";
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
            Assert.assertEquals(VCFormat.SD_JWT_VC, sdJwt.getIssuerSignedJWT().getJwsHeader().getType());
            Assert.assertEquals(1,
                                JsonSerialization.mapper.convertValue(sdJwt.getIssuerSignedJWT().getJwsHeader(),
                                                                      ObjectNode.class).size());

            Assert.assertEquals(iat,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_IAT).longValue());
            Assert.assertEquals(nbf,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_NBF).longValue());
            Assert.assertEquals(exp,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_EXP).longValue());
            Assert.assertEquals(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM,
                                sdJwt.getIssuerSignedJWT()
                                     .getPayload()
                                     .get(OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM)
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
                                                 .collect(Collectors.toList());
            List<String> decoyHashes = sdJwt.getIssuerSignedJWT()
                                            .getDecoyClaims()
                                            .stream()
                                            .map(decoy -> decoy.getDisclosureDigest(JavaAlgorithm.SHA256))
                                            .collect(Collectors.toList());
            List<String> expectedSdHashes = new ArrayList<>(disclosureHashes);
            expectedSdHashes.addAll(decoyHashes);
            expectedSdHashes = expectedSdHashes.stream().sorted().collect(Collectors.toList());
            JsonNode actualSdHashNode = sdJwt.getIssuerSignedJWT()
                                             .getPayload()
                                             .get(OID4VCConstants.CLAIM_NAME_SD);
            List<String> actualSdHashes = new ArrayList<>();
            for (JsonNode jsonNode : actualSdHashNode) {
                actualSdHashes.add(jsonNode.textValue());
            }
            actualSdHashes = actualSdHashes.stream().sorted().collect(Collectors.toList());
            Assert.assertEquals(expectedSdHashes, actualSdHashes);
            Assert.assertEquals(5, sdJwt.getIssuerSignedJWT().getPayload().size());
        }

        // make sure default ClaimVerifiers succeed
        {
            ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
            Assert.assertEquals(3, claimVerifier.getContentVerifiers().size());
            try {
                claimVerifier.verifyClaims(sdJwt.getIssuerSignedJWT().getJwsHeaderAsNode(),
                                           sdJwt.getIssuerSignedJWT().getPayload());
                Assert.fail("Verification must fail due to missing 'alg' header");
            } catch (VerificationException e) {
                Assert.assertEquals("Missing claim 'alg' in token", e.getMessage());
            }
        }

        final String sdJwtString = sdJwt.toSdJwtString();
        int disclosureStart = sdJwtString.indexOf(OID4VCConstants.SDJWT_DELIMITER);
        int disclosureEnd = sdJwtString.lastIndexOf(OID4VCConstants.SDJWT_DELIMITER);

        // validate applied disclosures
        {
            String disclosureString = sdJwtString.substring(disclosureStart + 1, disclosureEnd);
            String[] disclosureParts = disclosureString.split(OID4VCConstants.SDJWT_DELIMITER);
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
                                                          .collect(Collectors.toList());
            List<String> sortedActualDisclosures = Arrays.stream(disclosureParts).sorted(String::compareTo).collect(
                Collectors.toList());
            Assert.assertEquals(sortedExpectedDisclosures, sortedActualDisclosures);
        }
    }

    @Test
    public void testCreateSdJwtWithKeybindingJwt() throws Exception {
        final String authorizationServerUrl = "https://example.com";

        KeyWrapper issuerKeyPair = toKeyWrapper(KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1));
        JWK issuerJwk = JWKBuilder.create().ec(issuerKeyPair.getPublicKey());

        KeyWrapper holderKeyPair = toKeyWrapper(KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1));
        JWK holderKeybindingKey = JWKBuilder.create().ec(holderKeyPair.getPublicKey());

        SignatureSignerContext issuerSignerContext = new ECDSASignatureSignerContext(issuerKeyPair);
        SignatureSignerContext holderSignerContext = new ECDSASignatureSignerContext(holderKeyPair);

        Instant now = Instant.ofEpochSecond(Time.currentTime());
        final long iat = now.minus(10, ChronoUnit.SECONDS).getEpochSecond();
        final long nbf = now.minus(5, ChronoUnit.SECONDS).getEpochSecond();
        final long exp = now.plus(60, ChronoUnit.SECONDS).getEpochSecond();
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

        String disclosurePayload = "{\n" +
            "  \"given_name\": \"Carlos\",\n" +
            "  \"family_name\": \"Norris\"\n" +
            "}";
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
                                                         .withKeyBindingKey(holderKeybindingKey)
                                                         .withIat(iat)
                                                         .withNbf(nbf)
                                                         .withExp(exp)
                                                         .build();

        SdJwt sdJwt = SdJwt.builder()
                .withIssuerSignedJwt(issuerSignedJWT)
                .withKeybindingJwt(keyBindingJWT)
                .withIssuerSigningContext(issuerSignerContext)
                .withKeyBindingSigningContext(holderSignerContext)
                .build();

        // validate object content
        {
            Assert.assertEquals(Algorithm.ES256, sdJwt.getIssuerSignedJWT().getJwsHeader().getAlgorithm().name());
            Assert.assertEquals(VCFormat.SD_JWT_VC, sdJwt.getIssuerSignedJWT().getJwsHeader().getType());
            Assert.assertEquals(issuerKeyPair.getKid(), sdJwt.getIssuerSignedJWT().getJwsHeader().getKeyId());
            Assert.assertEquals(3,
                                JsonSerialization.mapper.convertValue(sdJwt.getIssuerSignedJWT().getJwsHeader(),
                                                                      ObjectNode.class).size());

            ObjectNode expectedCnf = JsonNodeFactory.instance.objectNode();
            expectedCnf.set("jwk", JsonSerialization.mapper.convertValue(holderKeybindingKey, ObjectNode.class));
            Assert.assertEquals(expectedCnf,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_CNF));
            Assert.assertEquals(iat,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_IAT).longValue());
            Assert.assertEquals(nbf,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_NBF).longValue());
            Assert.assertEquals(exp,
                                sdJwt.getIssuerSignedJWT().getPayload().get(OID4VCConstants.CLAIM_NAME_EXP).longValue());
            Assert.assertEquals(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM,
                                sdJwt.getIssuerSignedJWT()
                                     .getPayload()
                                     .get(OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM)
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
                                                 .collect(Collectors.toList());
            List<String> decoyHashes = sdJwt.getIssuerSignedJWT()
                                            .getDecoyClaims()
                                            .stream()
                                            .map(decoy -> decoy.getDisclosureDigest(JavaAlgorithm.SHA256))
                                            .collect(Collectors.toList());
            List<String> expectedSdHashes = new ArrayList<>(disclosureHashes);
            expectedSdHashes.addAll(decoyHashes);
            expectedSdHashes = expectedSdHashes.stream().sorted().collect(Collectors.toList());
            JsonNode actualSdHashNode = sdJwt.getIssuerSignedJWT()
                                             .getPayload()
                                             .get(OID4VCConstants.CLAIM_NAME_SD);
            List<String> actualSdHashes = new ArrayList<>();
            for (JsonNode jsonNode : actualSdHashNode) {
                actualSdHashes.add(jsonNode.textValue());
            }
            actualSdHashes = actualSdHashes.stream().sorted().collect(Collectors.toList());
            Assert.assertEquals(expectedSdHashes, actualSdHashes);
            Assert.assertEquals(6, sdJwt.getIssuerSignedJWT().getPayload().size());

            Assert.assertEquals(holderKeyPair.getKid(), sdJwt.getKeybindingJwt().getJwsHeader().getKeyId());
            Assert.assertEquals(OID4VCConstants.KEYBINDING_JWT_TYP, sdJwt.getKeybindingJwt().getJwsHeader().getType());
            Assert.assertEquals(Algorithm.ES256, sdJwt.getKeybindingJwt().getJwsHeader().getAlgorithm().name());
            Assert.assertEquals(3,
                                JsonSerialization.mapper.convertValue(sdJwt.getKeybindingJwt().getJwsHeader(),
                                                                      ObjectNode.class).size());

            Assert.assertEquals(iat,
                                sdJwt.getKeybindingJwt().getPayload().get(OID4VCConstants.CLAIM_NAME_IAT).longValue());
            Assert.assertEquals(nbf,
                                sdJwt.getKeybindingJwt().getPayload().get(OID4VCConstants.CLAIM_NAME_NBF).longValue());
            Assert.assertEquals(exp,
                                sdJwt.getKeybindingJwt().getPayload().get(OID4VCConstants.CLAIM_NAME_EXP).longValue());
            Assert.assertEquals(nonce, sdJwt.getKeybindingJwt().getPayload().get(IDToken.NONCE).textValue());
            Assert.assertEquals(audience, sdJwt.getKeybindingJwt().getPayload().get(IDToken.AUD).textValue());
            // check sd_hash
            {
                List<String> parts = new ArrayList<>();
                parts.add(sdJwt.getIssuerSignedJWT().getJws());
                parts.addAll(sdJwt.getDisclosures());
                parts.add("");
                String sdHashString = String.join(OID4VCConstants.SDJWT_DELIMITER, parts);
                String expectedSdHash = SdJwtUtils.hashAndBase64EncodeNoPad(sdHashString, JavaAlgorithm.SHA256);
                Assert.assertEquals(expectedSdHash, sdJwt.getKeybindingJwt().getPayload().get(OID4VCConstants.SD_HASH)
                                                         .textValue());
            }
            Assert.assertEquals(6, sdJwt.getKeybindingJwt().getPayload().size());
        }

        // make sure default ClaimVerifiers succeed
        {
            ClaimVerifier claimVerifier = ClaimVerifier.builder().build();
            Assert.assertEquals(3, claimVerifier.getContentVerifiers().size());
            try {
                claimVerifier.verifyClaims(sdJwt.getIssuerSignedJWT().getJwsHeaderAsNode(),
                                           sdJwt.getIssuerSignedJWT().getPayload());
            } catch (VerificationException e) {
                throw new RuntimeException("Verification should have succeeded", e);
            }
            try {
                claimVerifier.verifyClaims(sdJwt.getKeybindingJwt().getJwsHeaderAsNode(),
                                           sdJwt.getKeybindingJwt().getPayload());
            } catch (VerificationException e) {
                throw new RuntimeException("Verification should have succeeded", e);
            }
        }

        final String sdJwtString = sdJwt.toSdJwtString();
        int disclosureStart = sdJwtString.indexOf(OID4VCConstants.SDJWT_DELIMITER);
        int disclosureEnd = sdJwtString.lastIndexOf(OID4VCConstants.SDJWT_DELIMITER);

        // validate applied disclosures
        {
            String disclosureString = sdJwtString.substring(disclosureStart + 1, disclosureEnd);
            String[] disclosureParts = disclosureString.split(OID4VCConstants.SDJWT_DELIMITER);
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
                                                          .collect(Collectors.toList());
            List<String> sortedActualDisclosures = Arrays.stream(disclosureParts).sorted(String::compareTo).collect(
                Collectors.toList());
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
