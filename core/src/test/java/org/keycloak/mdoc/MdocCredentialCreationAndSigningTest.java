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
package org.keycloak.mdoc;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSAAlgorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.rule.CryptoInitRule;

import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class MdocCredentialCreationAndSigningTest {

    private static final String DOC_TYPE = "org.iso.18013.5.1.mDL";
    private static final String NAMESPACE = "org.iso.18013.5.1";
    private static final String GIVEN_NAME = "given_name";
    private static final String FAMILY_NAME = "family_name";

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void shouldCreateSignedMdocCredentialWithHolderBinding() throws Exception {
        KeyPair issuerKeyPair = KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1);
        X509Certificate issuerCertificate = selfSignedCertificate(issuerKeyPair);
        MdocCredential credential = mdocCredential();
        credential.addKeyBinding(proofJwk());

        MdocIssuerSignedDocument document = credential.signAsIssuerSignedDocument(
                new ECDSASignatureSignerContext(issuerKey(issuerKeyPair, issuerCertificate))
        );

        assertIssuedCredential(document, issuerCertificate);
        assertNotNull(document.getMobileSecurityObject().get("deviceKeyInfo"));
        assertIssuerAuthSignature(document, issuerKeyPair.getPublic());
    }

    @Test
    public void shouldCreateSignedMdocCredentialWithoutHolderBinding() throws Exception {
        KeyPair issuerKeyPair = KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1);
        X509Certificate issuerCertificate = selfSignedCertificate(issuerKeyPair);

        MdocIssuerSignedDocument document = mdocCredential().signAsIssuerSignedDocument(
                new ECDSASignatureSignerContext(issuerKey(issuerKeyPair, issuerCertificate))
        );

        assertIssuedCredential(document, issuerCertificate);
        assertNull(document.getMobileSecurityObject().get("deviceKeyInfo"));
        assertIssuerAuthSignature(document, issuerKeyPair.getPublic());
    }

    private static MdocCredential mdocCredential() {
        Map<String, Object> nameSpaceClaims = new LinkedHashMap<>();
        nameSpaceClaims.put(GIVEN_NAME, "Erika");
        nameSpaceClaims.put(FAMILY_NAME, "Mustermann");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(NAMESPACE, nameSpaceClaims);

        return new MdocCredential(
                DOC_TYPE,
                claims,
                MdocValidityInfo.issuedAt(
                        Instant.parse("2026-04-24T10:15:30Z"),
                        Instant.parse("2027-04-24T10:15:30Z")
                )
        );
    }

    private static JWK proofJwk() throws Exception {
        KeyPair proofKeyPair = KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1);
        return JWKBuilder.create()
                .kid("proof-key")
                .algorithm(Algorithm.ES256)
                .ec(proofKeyPair.getPublic(), KeyUse.SIG);
    }

    private static KeyWrapper issuerKey(KeyPair issuerKeyPair, X509Certificate issuerCertificate) {
        KeyWrapper issuerKey = new KeyWrapper();
        issuerKey.setAlgorithm(Algorithm.ES256);
        issuerKey.setUse(KeyUse.SIG);
        issuerKey.setType("EC");
        issuerKey.setKid("issuer-key");
        issuerKey.setPrivateKey(issuerKeyPair.getPrivate());
        issuerKey.setPublicKey(issuerKeyPair.getPublic());
        issuerKey.setCertificate(issuerCertificate);
        return issuerKey;
    }

    private static X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        return (X509Certificate) CertificateUtils.generateV1SelfSignedCertificate(keyPair, "issuer-key");
    }

    private static void assertIssuedCredential(MdocIssuerSignedDocument document, X509Certificate issuerCertificate)
            throws Exception {
        assertNotNull(document.getEncodedIssuerSigned());
        assertEquals(DOC_TYPE, document.getDocType());
        assertEquals(1, document.getIssuerAuthCertificateChain().size());
        assertArrayEquals(issuerCertificate.getEncoded(), document.getIssuerAuthCertificateChain().get(0).getEncoded());

        assertTrue(document.getNamespaces().get(NAMESPACE) instanceof Map);
        Map<?, ?> namespaceClaims = (Map<?, ?>) document.getNamespaces().get(NAMESPACE);
        assertEquals("Erika", namespaceClaims.get(GIVEN_NAME));
        assertEquals("Mustermann", namespaceClaims.get(FAMILY_NAME));

        Map<String, Object> mobileSecurityObject = document.getMobileSecurityObject();
        assertEquals("1.0", mobileSecurityObject.get("version"));
        assertEquals("SHA-256", mobileSecurityObject.get("digestAlgorithm"));
        assertEquals(DOC_TYPE, mobileSecurityObject.get("docType"));
        assertTrue(mobileSecurityObject.get("valueDigests") instanceof Map);
        assertNotNull(mobileSecurityObject.get("validityInfo"));
    }

    private static void assertIssuerAuthSignature(MdocIssuerSignedDocument document, PublicKey publicKey)
            throws Exception {
        Map<Object, Object> issuerSigned = CborUtil.asMap(
                CborUtil.decode(Base64.getUrlDecoder().decode(document.getEncodedIssuerSigned())),
                "issuerSigned"
        );
        MdocCose.ParsedSign1 issuerAuth = MdocCose.Sign1.fromCbor(issuerSigned.get("issuerAuth"), "issuerAuth");
        byte[] sigStructure = CborUtil.encode(new MdocCose.SigStructure(
                "Signature1",
                issuerAuth.protectedHeader(),
                new byte[0],
                issuerAuth.payload()
        ));

        Signature signature = Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(Algorithm.ES256));
        signature.initVerify(publicKey);
        signature.update(sigStructure);

        byte[] derSignature = ECDSAAlgorithm.concatenatedRSToASN1DER(
                issuerAuth.signature(),
                ECDSAAlgorithm.getSignatureLength(Algorithm.ES256)
        );
        assertTrue(signature.verify(derSignature));
    }
}
