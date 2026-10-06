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

package org.keycloak.tests.oid4vc.presentation;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OID4VCConstants;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderFactory;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderConfig;
import org.keycloak.broker.trust.DefaultTrustIdentityProviderFactory;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpDirectPostResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drives the OID4VP wallet login with the trusted issuer material delegated to external trust
 * material identity providers by alias instead of the inline trusted issuer JWKS. Covers both
 * trusted JWKS delegation and X.509 chain validation of the credential's x5c header against
 * delegated trust anchors.
 */
@KeycloakIntegrationTest(config = PresentationServerConfig.class)
public class OID4VPTrustMaterialDelegationTest extends OID4VPVerifierTestBase {

    private static final String TRUST_IDP_ALIAS = "issuer-trust";
    private static final String WRONG_KEYS_TRUST_IDP_ALIAS = "wrong-issuer-trust";
    private static final String X509_TRUST_IDP_ALIAS = "x509-issuer-trust";
    private static final String UNRELATED_X509_TRUST_IDP_ALIAS = "unrelated-x509-issuer-trust";
    private static final String EXTERNAL_TRUST_IDP_ALIAS = "external-issuer-trust";

    private static final IssuerCertificateChain CHAIN = IssuerCertificateChain.create();

    @Override
    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        addChainBackedSigningKey();
        createTrustIdp(TRUST_IDP_ALIAS, realmSigningJwks());
        createTrustIdp(WRONG_KEYS_TRUST_IDP_ALIAS, unrelatedJwks());
        createX509TrustIdp(X509_TRUST_IDP_ALIAS, CHAIN.caCertificatePem());
        createX509TrustIdp(UNRELATED_X509_TRUST_IDP_ALIAS, CHAIN.unrelatedCaCertificatePem());
        createVerifierIdp(Map.of(
                OID4VPIdentityProviderConfig.DCQL_QUERY, dcqlQuery(),
                OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "email",
                OID4VPIdentityProviderConfig.TRUST_MATERIAL_IDPS, TRUST_IDP_ALIAS));
    }

    @Test
    public void delegatedTrustMaterialVerifiesThePresentation() throws Exception {
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));

        driver.open(response.getRedirectUri());
        assertLoginContinued();
    }

    @Test
    public void keyMismatchedTrustIdpRejectsThePresentationEvenWithMatchingInlineJwks() throws Exception {
        // The delegated trust material takes precedence, a matching inline JWKS must not rescue a
        // presentation the delegated keys cannot verify.
        updateIdpConfig(OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, realmSigningJwks());
        updateIdpConfig(OID4VPIdentityProviderConfig.TRUST_MATERIAL_IDPS, WRONG_KEYS_TRUST_IDP_ALIAS);
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void disabledTrustIdpRejectsThePresentation() throws Exception {
        testRealm.updateIdentityProvider(TRUST_IDP_ALIAS, idp -> idp.setEnabled(false));
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void delegatedX509TrustAnchorValidatesTheCredentialChain() throws Exception {
        updateIdpConfig(OID4VPIdentityProviderConfig.TRUST_MATERIAL_IDPS, X509_TRUST_IDP_ALIAS);
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));

        driver.open(response.getRedirectUri());
        assertLoginContinued();
    }

    @Test
    public void unanchoredCredentialChainRejectsThePresentation() throws Exception {
        updateIdpConfig(OID4VPIdentityProviderConfig.TRUST_MATERIAL_IDPS, UNRELATED_X509_TRUST_IDP_ALIAS);
        OID4VCTestContext credential = issueCredential();
        JsonNode request = requestObject();

        Oid4vpDirectPostResponse response = wallet.directPost(request, wallet.present(credential, request));

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void mandatedChainValidationRejectsACredentialWithoutChain() throws Exception {
        // Keycloak's own issuer always emits an x5c header, so an externally issued credential
        // exercises the chainless case. The external JWKS trust idp alone would verify it, but the
        // X.509 trust idp mandates chain validation per HAIP 6.1.1, so it must be rejected.
        KeyWrapper issuerKey = ecKey();
        KeyWrapper holderKey = ecKey();
        createTrustIdp(EXTERNAL_TRUST_IDP_ALIAS, jwksOf(issuerKey));
        updateIdpConfig(OID4VPIdentityProviderConfig.TRUST_MATERIAL_IDPS,
                EXTERNAL_TRUST_IDP_ALIAS + "," + X509_TRUST_IDP_ALIAS);
        JsonNode request = requestObject();

        String vpToken = wallet.present(chainlessSdJwtVc(issuerKey, holderKey), holderKey,
                request.path("client_id").asText(), request.path("nonce").asText());
        Oid4vpDirectPostResponse response = wallet.directPost(request, vpToken);

        assertEquals(400, response.getStatusCode());
        assertEquals("access_denied", response.getError());
    }

    @Test
    public void verifierIdpRequiresTrustedJwksOrTrustIdps() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias("oid4vp-without-trust");
        idp.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setConfig(Map.of(
                OID4VPIdentityProviderConfig.DCQL_QUERY, dcqlQuery(),
                OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, "email"));

        try (Response response = testRealm.admin().identityProviders().create(idp)) {
            assertEquals(400, response.getStatus(),
                    "An oid4vp idp without trusted issuer JWKS and without trust material idps must be rejected");
        }
    }

    private void createTrustIdp(String alias, String trustedJwks) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId(DefaultTrustIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setConfig(Map.of(DefaultTrustIdentityProviderConfig.TRUSTED_JWKS, trustedJwks));
        try (Response response = testRealm.admin().identityProviders().create(idp)) {
            assertEquals(201, response.getStatus(), "Failed to create the trust material identity provider " + alias);
        }
    }

    // No extended key usages are configured, the trust anchors alone decide trust.
    private void createX509TrustIdp(String alias, String caCertificatePem) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId(DefaultTrustIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setConfig(Map.of(
                DefaultTrustIdentityProviderConfig.USE_X509, "true",
                DefaultTrustIdentityProviderConfig.TRUSTED_CERTIFICATES, caCertificatePem));
        try (Response response = testRealm.admin().identityProviders().create(idp)) {
            assertEquals(201, response.getStatus(), "Failed to create the X.509 trust material identity provider " + alias);
        }
    }

    // Signs the issued credentials with a key whose certificate chains to the fixture CA, so the
    // presented SD JWT carries an x5c header that can only be validated through the delegated trust
    // anchors. Priority 200 outranks the ES256 key added by the base class.
    private void addChainBackedSigningKey() {
        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName("oid4vp-chain-backed-signing-key");
        keyProvider.setParentId(testRealm.getId());
        keyProvider.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        keyProvider.setProviderType(KeyProvider.class.getName());
        keyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                Attributes.PRIORITY_KEY, List.of("200"),
                Attributes.ENABLED_KEY, List.of("true"),
                Attributes.ACTIVE_KEY, List.of("true"),
                GeneratedEcdsaKeyProviderFactory.ECDSA_PRIVATE_KEY_KEY, List.of(
                        Base64.getEncoder().encodeToString(CHAIN.leafKeyPair().getPrivate().getEncoded())),
                GeneratedEcdsaKeyProviderFactory.ECDSA_PUBLIC_KEY_KEY, List.of(
                        Base64.getEncoder().encodeToString(CHAIN.leafKeyPair().getPublic().getEncoded())),
                GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, List.of("P-256"),
                Attributes.CERTIFICATE_KEY, List.of(CHAIN.leafCertificatePem()),
                Attributes.ALGORITHM_KEY, List.of(Algorithm.ES256),
                Attributes.KEY_USE, List.of(KeyUse.SIG.name()))));
        try (Response response = testRealm.admin().components().add(keyProvider)) {
            assertEquals(201, response.getStatus(), "Failed to add the chain backed signing key");
        }
    }

    // A valid JWKS whose key never signed the issued credential.
    private static String unrelatedJwks() {
        return jwksOf(ecKey());
    }

    private static KeyWrapper ecKey() {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        KeyWrapper key = new KeyWrapper();
        key.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        key.setType(KeyType.EC);
        key.setAlgorithm(Algorithm.ES256);
        key.setUse(KeyUse.SIG);
        key.setPrivateKey(keyPair.getPrivate());
        key.setPublicKey(keyPair.getPublic());
        return key;
    }

    private static String jwksOf(KeyWrapper key) {
        try {
            JWK jwk = JWKBuilder.create()
                    .kid(key.getKid())
                    .algorithm(Algorithm.ES256)
                    .ec(key.getPublicKey(), KeyUse.SIG);
            JSONWebKeySet jwks = new JSONWebKeySet();
            jwks.setKeys(new JWK[] {jwk});
            return JsonSerialization.writeValueAsString(jwks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // An SD JWT VC as an external issuer without a certificate chain would produce it, all claims
    // disclosed in the plain payload and no x5c header.
    private String chainlessSdJwtVc(KeyWrapper issuerKey, KeyWrapper holderKey) {
        ObjectNode claims = JsonSerialization.mapper.createObjectNode();
        claims.put("vct", sdJwtTypeCredentialVct);
        claims.put("iss", "https://external-issuer.example.com");
        claims.put("iat", Time.currentTimeSeconds());
        claims.put("exp", Time.currentTimeSeconds() + 300);
        claims.put("email", "external-holder@example.com");
        claims.put("lastName", "External");
        claims.set("cnf", JsonSerialization.mapper.createObjectNode()
                .set("jwk", JsonSerialization.mapper.valueToTree(
                        JWKBuilder.create().algorithm(Algorithm.ES256).ec(holderKey.getPublicKey(), KeyUse.SIG))));
        return SdJwt.builder()
                .withIssuerSignedJwt(IssuerSignedJWT.builder()
                        .withClaims(claims, DisclosureSpec.builder().build())
                        .withJwsType(OID4VCConstants.FORMAT_SD_JWT_VC)
                        .withKid(issuerKey.getKid())
                        .build())
                .withUseDefaultDecoys(false)
                .withIssuerSigningContext(new ECDSASignatureSignerContext(issuerKey))
                .build()
                .toSdJwtString();
    }

    // An end entity signing certificate issued by a CA root that qualifies as a trust anchor, plus
    // an unrelated CA root the chain does not lead to. The leaf carries no extended key usage
    // because issuer signing certificates are not bound to an attestation purpose.
    private record IssuerCertificateChain(KeyPair leafKeyPair, String leafCertificatePem,
                                          String caCertificatePem, String unrelatedCaCertificatePem) {

        private static final SecureRandom RANDOM = new SecureRandom();

        private static IssuerCertificateChain create() {
            try {
                if (!CryptoIntegration.isInitialised()) {
                    CryptoIntegration.setProvider(new DefaultCryptoProvider());
                }

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
                keyGen.initialize(new ECGenParameterSpec("secp256r1"));
                KeyPair caKeyPair = keyGen.generateKeyPair();
                KeyPair leafKeyPair = keyGen.generateKeyPair();
                KeyPair unrelatedCaKeyPair = keyGen.generateKeyPair();

                X509Certificate caCertificate = generateCaCertificate(caKeyPair, "OID4VP Issuer CA");
                X509Certificate unrelatedCaCertificate = generateCaCertificate(unrelatedCaKeyPair,
                        "OID4VP Unrelated CA");
                X509Certificate leafCertificate = generateLeafCertificate(leafKeyPair, caKeyPair, caCertificate);

                return new IssuerCertificateChain(leafKeyPair,
                        PemUtils.encodeCertificate(leafCertificate),
                        PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(caCertificate)),
                        PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(unrelatedCaCertificate)));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create the issuer certificate chain", e);
            }
        }

        private static X509Certificate generateCaCertificate(KeyPair caKeyPair, String commonName) throws Exception {
            X500Name caName = new X500Name("CN=" + commonName);
            X509v3CertificateBuilder builder = certificateBuilder(caName, caName, caKeyPair);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            return sign(builder, caKeyPair);
        }

        private static X509Certificate generateLeafCertificate(KeyPair leafKeyPair, KeyPair caKeyPair,
                                                               X509Certificate caCertificate) throws Exception {
            X500Name issuer = new X500Name(caCertificate.getSubjectX500Principal().getName());
            X509v3CertificateBuilder builder = certificateBuilder(issuer,
                    new X500Name("CN=OID4VP Issuer Signing Key"), leafKeyPair);
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            return sign(builder, caKeyPair);
        }

        private static X509v3CertificateBuilder certificateBuilder(X500Name issuer, X500Name subject,
                                                                   KeyPair subjectKeyPair) {
            Instant now = Instant.now();
            return new X509v3CertificateBuilder(
                    issuer,
                    new BigInteger(160, RANDOM),
                    Date.from(now.minus(1, ChronoUnit.DAYS)),
                    Date.from(now.plus(365, ChronoUnit.DAYS)),
                    subject,
                    SubjectPublicKeyInfo.getInstance(subjectKeyPair.getPublic().getEncoded()));
        }

        private static X509Certificate sign(X509v3CertificateBuilder builder, KeyPair signingKeyPair)
                throws Exception {
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .build(signingKeyPair.getPrivate());
            return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        }
    }
}
