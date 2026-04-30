package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.mdoc.MdocIssuerSignedDocument;
import org.keycloak.mdoc.MdocValidityInfo;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MdocCredentialBuilderTest {

    private static final String DOC_TYPE = "org.iso.18013.5.1.mDL";
    private static final String NAMESPACE = "org.iso.18013.5.1";

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(MdocCredentialBuilderTest.class.getClassLoader());
    }

    @Test
    public void shouldBuildMdocBodyWithNamespaceClaims() throws Exception {
        MdocCredentialBuilder builder = new MdocCredentialBuilder();

        VerifiableCredential credential = new VerifiableCredential()
                .setIssuanceDate(Instant.parse("2026-04-24T10:15:30Z"))
                .setExpirationDate(Instant.parse("2027-04-24T10:15:30Z"));

        Map<String, Object> namespaceClaims = new LinkedHashMap<>();
        namespaceClaims.put("given_name", "John");
        namespaceClaims.put("family_name", "Doe");
        namespaceClaims.put("document_number", "did:key:1234");
        credential.getCredentialSubject().setClaims(NAMESPACE, namespaceClaims);

        MdocCredentialBody body = builder.buildCredentialBody(
                credential,
                new CredentialBuildConfig().setCredentialType(DOC_TYPE)
        );

        assertThat(body.getDocType(), equalTo(DOC_TYPE));
        assertThat(body.getValidityInfo(), notNullValue());
        assertTrue(body.getClaims().containsKey(NAMESPACE));
        assertThat(body.getClaims().get(NAMESPACE), instanceOf(Map.class));
        assertThat(body.getClaims().get(NAMESPACE), equalTo(namespaceClaims));
    }

    @Test
    public void shouldRequireDocType() throws Exception {
        MdocCredentialBuilder builder = new MdocCredentialBuilder();

        assertThrows(CredentialBuilderException.class,
                () -> builder.buildCredentialBody(new VerifiableCredential(), new CredentialBuildConfig()));
    }

    @Test
    public void shouldRequireExpirationDate() throws Exception {
        MdocCredentialBuilder builder = new MdocCredentialBuilder();

        VerifiableCredential credential = new VerifiableCredential()
                .setIssuanceDate(Instant.parse("2026-04-24T10:15:30Z"));

        assertThrows(CredentialBuilderException.class,
                () -> builder.buildCredentialBody(credential, new CredentialBuildConfig().setCredentialType(DOC_TYPE)));
    }

    @Test
    public void shouldAcceptProofKeyWithoutAlgorithm() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair issuerKeyPair = keyPairGenerator.generateKeyPair();
        KeyPair proofKeyPair = keyPairGenerator.generateKeyPair();
        JWK proofJwk = JWKBuilder.create().kid("proof-key").ec(proofKeyPair.getPublic(), KeyUse.SIG);
        proofJwk.setAlgorithm(null);

        MdocCredentialBody body = new MdocCredentialBody(
                DOC_TYPE,
                Map.of(NAMESPACE, Map.of("given_name", "John")),
                builderValidityInfo()
        );

        body.addKeyBinding(proofJwk);

        KeyWrapper issuerKey = createIssuerKey(issuerKeyPair, Algorithm.ES384, "EC");
        MdocIssuerSignedDocument issuerSignedDocument = body.signAsIssuerSignedDocument(
                new ECDSASignatureSignerContext(issuerKey)
        );
        assertSignedMdoc(issuerSignedDocument);
    }

    @Test
    public void shouldSignMdocCredentialWithEcIssuerKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair issuerKeyPair = keyPairGenerator.generateKeyPair();
        KeyPair proofKeyPair = keyPairGenerator.generateKeyPair();

        JWK proofJwk = JWKBuilder.create()
                .kid("proof-key")
                .algorithm(Algorithm.ES256)
                .ec(proofKeyPair.getPublic(), KeyUse.SIG);

        MdocCredentialBody body = new MdocCredentialBody(
                DOC_TYPE,
                Map.of(NAMESPACE, Map.of("given_name", "John")),
                builderValidityInfo()
        );
        body.addKeyBinding(proofJwk);

        KeyWrapper issuerKey = createIssuerKey(issuerKeyPair, Algorithm.ES256, "EC");
        MdocIssuerSignedDocument issuerSignedDocument = body.signAsIssuerSignedDocument(
                new ECDSASignatureSignerContext(issuerKey)
        );
        assertSignedMdoc(issuerSignedDocument);
    }

    @Test
    public void shouldSignMdocCredentialWithRsaIssuerKey() throws Exception {
        KeyPair issuerKeyPair = KeyUtils.generateRsaKeyPair(2048);
        KeyPair proofKeyPair = KeyUtils.generateRsaKeyPair(2048);

        JWK proofJwk = JWKBuilder.create()
                .kid("proof-key")
                .algorithm(Algorithm.PS256)
                .rsa(proofKeyPair.getPublic(), KeyUse.SIG);

        MdocCredentialBody body = new MdocCredentialBody(
                DOC_TYPE,
                Map.of(NAMESPACE, Map.of("given_name", "John")),
                builderValidityInfo()
        );
        body.addKeyBinding(proofJwk);

        KeyWrapper issuerKey = createIssuerKey(issuerKeyPair, Algorithm.RS256, "RSA");
        MdocIssuerSignedDocument issuerSignedDocument = body.signAsIssuerSignedDocument(
                new AsymmetricSignatureSignerContext(issuerKey)
        );
        assertSignedMdoc(issuerSignedDocument);
    }

    @Test
    public void shouldRequireCertificateChainForEdDsaIssuerKey() throws Exception {
        KeyPair issuerKeyPair = KeyUtils.generateEddsaKeyPair(Algorithm.Ed25519);
        KeyPair proofKeyPair = KeyUtils.generateEddsaKeyPair(Algorithm.Ed25519);

        JWK proofJwk = JWKBuilder.create()
                .kid("proof-key")
                .algorithm(Algorithm.EdDSA)
                .okp(proofKeyPair.getPublic(), KeyUse.SIG);

        MdocCredentialBody body = new MdocCredentialBody(
                DOC_TYPE,
                Map.of(NAMESPACE, Map.of("given_name", "John")),
                builderValidityInfo()
        );
        body.addKeyBinding(proofJwk);

        KeyWrapper issuerKey = createIssuerKey(issuerKeyPair, Algorithm.EdDSA, "OKP", false);
        assertThrows(CredentialSignerException.class,
                () -> body.signAsIssuerSignedDocument(new AsymmetricSignatureSignerContext(issuerKey)));
    }

    @Test
    public void shouldRequireIssuerCertificateChain() throws Exception {
        KeyPair issuerKeyPair = KeyUtils.generateRsaKeyPair(2048);

        MdocCredentialBody body = new MdocCredentialBody(
                DOC_TYPE,
                Map.of(NAMESPACE, Map.of("given_name", "John")),
                builderValidityInfo()
        );

        KeyWrapper issuerKey = createIssuerKey(issuerKeyPair, Algorithm.RS256, "RSA", false);
        assertThrows(CredentialSignerException.class,
                () -> body.signAsIssuerSignedDocument(new AsymmetricSignatureSignerContext(issuerKey)));
    }

    private static KeyWrapper createIssuerKey(KeyPair issuerKeyPair, String algorithm, String type) throws Exception {
        return createIssuerKey(issuerKeyPair, algorithm, type, true);
    }

    private static KeyWrapper createIssuerKey(KeyPair issuerKeyPair, String algorithm, String type, boolean includeCertificate) throws Exception {
        KeyWrapper issuerKey = new KeyWrapper();
        issuerKey.setAlgorithm(algorithm);
        issuerKey.setUse(KeyUse.SIG);
        issuerKey.setType(type);
        issuerKey.setKid("issuer-key");
        issuerKey.setPrivateKey(issuerKeyPair.getPrivate());
        issuerKey.setPublicKey(issuerKeyPair.getPublic());
        if (includeCertificate) {
            issuerKey.setCertificate((X509Certificate) CertificateUtils.generateV1SelfSignedCertificate(issuerKeyPair, "issuer-key"));
        }
        return issuerKey;
    }

    private void assertSignedMdoc(MdocIssuerSignedDocument issuerSignedDocument) {
        assertThat(issuerSignedDocument.getEncodedIssuerSigned(), notNullValue());
        assertTrue(issuerSignedDocument.getNamespaces().containsKey(NAMESPACE));
        assertThat(issuerSignedDocument.getIssuerAuthCertificateChain().isEmpty(), equalTo(false));
        assertThat(issuerSignedDocument.getDocType(), equalTo(DOC_TYPE));
    }

    private static MdocValidityInfo builderValidityInfo() {
        return new MdocValidityInfo(
                Instant.parse("2026-04-24T10:15:30Z"),
                Instant.parse("2026-04-24T10:15:30Z"),
                Instant.parse("2027-04-24T10:15:30Z")
        );
    }
}
