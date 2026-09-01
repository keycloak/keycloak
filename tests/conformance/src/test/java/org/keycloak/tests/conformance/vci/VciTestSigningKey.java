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

package org.keycloak.tests.conformance.vci;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.tests.conformance.ConformanceSigningKey;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// Not using org.keycloak.common.util.CertificateUtils as its leaf certificates are CA capable, which strict
// trust chain validation rejects for an issuer signing certificate. The certificates follow the ISO/IEC 18013
// part 5 certificate profiles the suite validates against.
final class VciTestSigningKey {

    static final String KEY_ALIAS = "oid4vci-conformance-signing";
    static final String PASSWORD = "password";

    private static final String KEY_STORE_PATH;
    private static final String CA_CERTIFICATE_PEM;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final KeyPurposeId MDOC_DS_KEY_PURPOSE =
            KeyPurposeId.getInstance(new ASN1ObjectIdentifier("1.0.18013.5.1.2"));

    private static final GeneralNames ISSUER_CONTACT = new GeneralNames(
            new GeneralName(GeneralName.rfc822Name, "oid4vci-conformance@example.com"));

    static {
        try {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }

            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeyPair = keyGenerator.generateKeyPair();
            KeyPair leafKeyPair = keyGenerator.generateKeyPair();

            X500Name caName = new X500Name("C=DE,CN=OID4VCI Conformance CA");
            X509Certificate caCertificate = generateCaCertificate(caName, caKeyPair);
            X509Certificate leafCertificate = generateLeafCertificate(caName, leafKeyPair, caKeyPair, caCertificate);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEY_ALIAS, leafKeyPair.getPrivate(), PASSWORD.toCharArray(),
                    new Certificate[] { leafCertificate, caCertificate });

            Path keyStorePath = Files.createTempFile(
                    ConformanceSigningKey.realmKeystoreDir(VciConformanceRealmConfig.REALM),
                    "keycloak-oid4vci-conformance-signing", ".p12");
            try (OutputStream output = Files.newOutputStream(keyStorePath)) {
                keyStore.store(output, PASSWORD.toCharArray());
            }
            keyStorePath.toFile().deleteOnExit();

            KEY_STORE_PATH = keyStorePath.toString();
            CA_CERTIFICATE_PEM = PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(caCertificate));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OID4VCI conformance signing key", e);
        }
    }

    private VciTestSigningKey() {
    }

    static String keyStorePath() {
        return KEY_STORE_PATH;
    }

    static String caCertificatePem() {
        return CA_CERTIFICATE_PEM;
    }

    private static X509Certificate generateCaCertificate(X500Name caName, KeyPair caKeyPair) throws Exception {
        X509v3CertificateBuilder builder = certificateBuilder(caName, caName, caKeyPair.getPublic());
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        builder.addExtension(Extension.subjectKeyIdentifier, false,
                extensionUtils.createSubjectKeyIdentifier(caKeyPair.getPublic()));
        builder.addExtension(Extension.issuerAlternativeName, false, ISSUER_CONTACT);
        builder.addExtension(Extension.cRLDistributionPoints, false, crlDistributionPoints());
        return sign(builder, caKeyPair);
    }

    private static X509Certificate generateLeafCertificate(X500Name caName, KeyPair leafKeyPair, KeyPair caKeyPair,
            X509Certificate caCertificate) throws Exception {
        X500Name leafName = new X500Name("C=DE,CN=OID4VCI Conformance Issuer");
        X509v3CertificateBuilder builder = certificateBuilder(caName, leafName, leafKeyPair.getPublic());
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        builder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(MDOC_DS_KEY_PURPOSE));
        builder.addExtension(Extension.subjectKeyIdentifier, false,
                extensionUtils.createSubjectKeyIdentifier(leafKeyPair.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false,
                extensionUtils.createAuthorityKeyIdentifier(caCertificate));
        builder.addExtension(Extension.issuerAlternativeName, false, ISSUER_CONTACT);
        builder.addExtension(Extension.cRLDistributionPoints, false, crlDistributionPoints());
        return sign(builder, caKeyPair);
    }

    private static CRLDistPoint crlDistributionPoints() {
        GeneralNames crlUri = new GeneralNames(new GeneralName(
                GeneralName.uniformResourceIdentifier, "https://example.com/oid4vci-conformance.crl"));
        return new CRLDistPoint(new DistributionPoint[] {
                new DistributionPoint(new DistributionPointName(crlUri), null, null) });
    }

    private static X509v3CertificateBuilder certificateBuilder(X500Name issuer, X500Name subject, PublicKey publicKey) {
        Instant now = Instant.now();
        return new JcaX509v3CertificateBuilder(
                issuer,
                new BigInteger(159, RANDOM),
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                publicKey);
    }

    private static X509Certificate sign(X509v3CertificateBuilder builder, KeyPair signingKeyPair) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signingKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
