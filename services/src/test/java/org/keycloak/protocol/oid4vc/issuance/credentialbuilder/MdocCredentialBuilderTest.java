package org.keycloak.protocol.oid4vc.issuance.credentialbuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
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

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORTaggedItem;
import com.authlete.cose.COSESign1;
import com.authlete.cose.COSEVerifier;
import com.authlete.mdoc.IssuerNameSpaces;
import com.authlete.mdoc.IssuerNameSpacesEntry;
import com.authlete.mdoc.IssuerSigned;
import com.authlete.mdoc.IssuerSignedItem;
import com.authlete.mdoc.IssuerSignedItemBytes;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MdocCredentialBuilderTest {

    private static final String DOC_TYPE = "org.iso.18013.5.1.mDL";
    private static final String NAMESPACE = "org.iso.18013.5.1";
    private static final int HEADER_ALGORITHM = 1;

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
    public void shouldProduceCborReadableIssuerSignedStructure() throws Exception {
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

        assertAuthleteCanParseIssuerSigned(issuerSignedDocument);
        assertThat(issuerSignedDocument.getIssuerAuthCertificateChain().isEmpty(), equalTo(false));

        Map<String, Object> mobileSecurityObject = issuerSignedDocument.getMobileSecurityObject();
        assertThat(mobileSecurityObject.get("version"), equalTo("1.0"));
        assertThat(mobileSecurityObject.get("digestAlgorithm"), equalTo("SHA-256"));
        assertThat(mobileSecurityObject.get("docType"), equalTo(DOC_TYPE));
        assertNotNull(mobileSecurityObject.get("valueDigests"));
        assertNotNull(mobileSecurityObject.get("validityInfo"));
        assertNotNull(mobileSecurityObject.get("deviceKeyInfo"));
    }

    @Test
    public void shouldRejectUnsupportedOkpProofKeyCurve() throws Exception {
        KeyPair proofKeyPair = KeyUtils.generateEddsaKeyPair(Algorithm.Ed448);
        JWK proofJwk = JWKBuilder.create()
                .kid("proof-key")
                .algorithm(Algorithm.EdDSA)
                .okp(proofKeyPair.getPublic(), KeyUse.SIG);

        MdocCredentialBody body = new MdocCredentialBody(
                DOC_TYPE,
                Map.of(NAMESPACE, Map.of("given_name", "John")),
                builderValidityInfo()
        );

        assertThrows(CredentialBuilderException.class, () -> body.addKeyBinding(proofJwk));
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

    private static void assertAuthleteCanParseIssuerSigned(MdocIssuerSignedDocument issuerSignedDocument) throws Exception {
        CBORPairList selfBuiltIssuerSigned = decodeAuthleteIssuerSigned(
                Base64.getUrlDecoder().decode(issuerSignedDocument.getEncodedIssuerSigned()));
        COSESign1 selfBuiltIssuerAuth = readIssuerAuth(selfBuiltIssuerSigned);
        IssuerSigned authleteBuiltIssuerSigned = buildAuthleteIssuerSigned(selfBuiltIssuerSigned);
        CBORPairList authleteBuiltIssuerSignedView = decodeAuthleteIssuerSigned(authleteBuiltIssuerSigned.encode());

        AuthleteIssuerSignedView selfBuilt = toAuthleteIssuerSignedView(selfBuiltIssuerSigned);
        AuthleteIssuerSignedView authleteBuilt = toAuthleteIssuerSignedView(authleteBuiltIssuerSignedView);

        assertThat(authleteBuilt.namespaceClaims(), equalTo(selfBuilt.namespaceClaims()));
        assertThat(authleteBuilt.docType(), equalTo(selfBuilt.docType()));
        assertThat(authleteBuilt.issuerAuthAlgorithm(), equalTo(selfBuilt.issuerAuthAlgorithm()));
        assertThat(authleteBuilt.issuerAuthX5Chain(), equalTo(selfBuilt.issuerAuthX5Chain()));
        assertArrayEquals(selfBuilt.issuerAuthPayload(), authleteBuilt.issuerAuthPayload());
        assertArrayEquals(selfBuilt.issuerAuthSignature(), authleteBuilt.issuerAuthSignature());
        assertArrayEquals(new byte[] { (byte) 0xA1, HEADER_ALGORITHM, 0x26 },
                selfBuiltIssuerAuth.getProtectedHeader().getValue());
        assertThat(new COSEVerifier(issuerAuthPublicKey(selfBuiltIssuerAuth)).verify(selfBuiltIssuerAuth),
                equalTo(true));
    }

    private static CBORPairList decodeAuthleteIssuerSigned(byte[] encodedIssuerSigned) throws Exception {
        CBORItem issuerSigned = new CBORDecoder(encodedIssuerSigned).next();
        assertThat(issuerSigned, instanceOf(CBORPairList.class));
        return (CBORPairList) issuerSigned;
    }

    private static AuthleteIssuerSignedView toAuthleteIssuerSignedView(CBORPairList issuerSigned) throws Exception {
        COSESign1 issuerAuth = readIssuerAuth(issuerSigned);
        return new AuthleteIssuerSignedView(
                readNamespaceClaims(issuerSigned),
                readMobileSecurityObject(issuerAuth).findByKey("docType").getValue().parse().toString(),
                issuerAuth.getProtectedHeader().getAlg(),
                encodedX5Chain(issuerAuth),
                ((CBORByteArray) issuerAuth.getPayload()).getValue(),
                issuerAuth.getSignature().getValue()
        );
    }

    private static Map<String, Map<String, Object>> readNamespaceClaims(CBORPairList issuerSigned) throws Exception {
        CBORPair nameSpacesPair = issuerSigned.findByKey("nameSpaces");
        assertNotNull(nameSpacesPair);
        assertThat(nameSpacesPair.getValue(), instanceOf(CBORPairList.class));

        Map<String, Map<String, Object>> namespaceClaims = new LinkedHashMap<>();
        for (CBORPair nameSpacePair : ((CBORPairList) nameSpacesPair.getValue()).getPairs()) {
            assertThat(nameSpacePair.getValue(), instanceOf(CBORItemList.class));
            Map<String, Object> claims = new LinkedHashMap<>();
            for (CBORItem item : ((CBORItemList) nameSpacePair.getValue()).getItems()) {
                CBORPairList issuerSignedItem = readIssuerSignedItem(item);
                claims.put(
                        issuerSignedItem.findByKey("elementIdentifier").getValue().parse().toString(),
                        issuerSignedItem.findByKey("elementValue").getValue().parse()
                );
            }
            namespaceClaims.put(nameSpacePair.getKey().parse().toString(), claims);
        }

        return namespaceClaims;
    }

    private static CBORPairList readMobileSecurityObject(COSESign1 issuerAuth) throws Exception {
        assertThat(issuerAuth.getPayload(), instanceOf(CBORByteArray.class));
        CBORItem mobileSecurityObjectBytes = new CBORDecoder(((CBORByteArray) issuerAuth.getPayload()).getValue()).next();
        CBORItem mobileSecurityObject = decodeAuthleteEncodedCbor(mobileSecurityObjectBytes);
        assertThat(mobileSecurityObject, instanceOf(CBORPairList.class));
        return (CBORPairList) mobileSecurityObject;
    }

    private static IssuerSigned buildAuthleteIssuerSigned(CBORPairList issuerSigned) throws Exception {
        return new IssuerSigned(
                new IssuerNameSpaces(buildAuthleteNameSpaces(issuerSigned)),
                readIssuerAuth(issuerSigned)
        );
    }

    private static List<IssuerNameSpacesEntry> buildAuthleteNameSpaces(CBORPairList issuerSigned) throws Exception {
        CBORPair nameSpacesPair = issuerSigned.findByKey("nameSpaces");
        assertNotNull(nameSpacesPair);
        assertThat(nameSpacesPair.getValue(), instanceOf(CBORPairList.class));

        List<IssuerNameSpacesEntry> authleteNameSpaces = new ArrayList<>();
        for (CBORPair nameSpacePair : ((CBORPairList) nameSpacesPair.getValue()).getPairs()) {
            assertThat(nameSpacePair.getValue(), instanceOf(CBORItemList.class));
            List<IssuerSignedItemBytes> itemBytes = new ArrayList<>();
            for (CBORItem item : ((CBORItemList) nameSpacePair.getValue()).getItems()) {
                itemBytes.add(buildAuthleteIssuerSignedItemBytes(item));
            }
            authleteNameSpaces.add(new IssuerNameSpacesEntry(nameSpacePair.getKey().parse().toString(), itemBytes));
        }
        return authleteNameSpaces;
    }

    private static IssuerSignedItemBytes buildAuthleteIssuerSignedItemBytes(CBORItem item) throws Exception {
        CBORPairList issuerSignedItemPairs = readIssuerSignedItem(item);
        int digestId = ((Number) issuerSignedItemPairs.findByKey("digestID").getValue().parse()).intValue();
        byte[] random = ((CBORByteArray) issuerSignedItemPairs.findByKey("random").getValue()).getValue();
        String elementIdentifier = issuerSignedItemPairs.findByKey("elementIdentifier").getValue().parse().toString();
        Object elementValue = issuerSignedItemPairs.findByKey("elementValue").getValue().parse();

        return new IssuerSignedItemBytes(new IssuerSignedItem(digestId, random, elementIdentifier, elementValue));
    }

    private static CBORPairList readIssuerSignedItem(CBORItem item) throws Exception {
        CBORItem issuerSignedItem = decodeAuthleteEncodedCbor(item);
        assertThat(issuerSignedItem, instanceOf(CBORPairList.class));
        return (CBORPairList) issuerSignedItem;
    }

    private static COSESign1 readIssuerAuth(CBORPairList issuerSigned) throws Exception {
        CBORPair issuerAuthPair = issuerSigned.findByKey("issuerAuth");
        assertNotNull(issuerAuthPair);
        COSESign1 issuerAuth = COSESign1.build(issuerAuthPair.getValue());
        assertThat(issuerAuth.getSignature().getValue().length > 0, equalTo(true));
        return issuerAuth;
    }

    private static List<String> encodedX5Chain(COSESign1 issuerAuth) throws Exception {
        List<String> encodedCertificates = new ArrayList<>();
        for (X509Certificate certificate : issuerAuth.getUnprotectedHeader().getX5Chain()) {
            encodedCertificates.add(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        }
        return encodedCertificates;
    }

    private static PublicKey issuerAuthPublicKey(COSESign1 issuerAuth) throws Exception {
        return issuerAuth.getUnprotectedHeader().getX5Chain().get(0).getPublicKey();
    }

    private static CBORItem decodeAuthleteEncodedCbor(CBORItem item) throws Exception {
        assertThat(item, instanceOf(CBORTaggedItem.class));
        CBORTaggedItem taggedItem = (CBORTaggedItem) item;
        assertThat(taggedItem.getTagNumber().intValue(), equalTo(24));
        assertThat(taggedItem.getTagContent(), instanceOf(CBORByteArray.class));
        return new CBORDecoder(((CBORByteArray) taggedItem.getTagContent()).getValue()).next();
    }

    private record AuthleteIssuerSignedView(Map<String, Map<String, Object>> namespaceClaims,
                                            String docType,
                                            Object issuerAuthAlgorithm,
                                            List<String> issuerAuthX5Chain,
                                            byte[] issuerAuthPayload,
                                            byte[] issuerAuthSignature) {
    }

    private static MdocValidityInfo builderValidityInfo() {
        return new MdocValidityInfo(
                Instant.parse("2026-04-24T10:15:30Z"),
                Instant.parse("2026-04-24T10:15:30Z"),
                Instant.parse("2027-04-24T10:15:30Z")
        );
    }

}
