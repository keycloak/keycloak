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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// Not using org.keycloak.common.util.CertificateUtils as its leaf certificates are CA capable, which strict
// trust chain validation rejects for an issuer signing certificate
final class VciTestSigningKey {

    static final String KEY_ALIAS = "oid4vci-conformance-signing";
    static final String PASSWORD = "password";

    private static final String KEY_STORE_PATH;
    private static final String CA_CERTIFICATE_PEM;

    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        try {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }

            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeyPair = keyGenerator.generateKeyPair();
            KeyPair leafKeyPair = keyGenerator.generateKeyPair();

            X509Certificate caCertificate = generateCaCertificate(caKeyPair);
            X509Certificate leafCertificate = generateLeafCertificate(leafKeyPair, caKeyPair, caCertificate);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEY_ALIAS, leafKeyPair.getPrivate(), PASSWORD.toCharArray(),
                    new Certificate[] { leafCertificate, caCertificate });

            Path keyStorePath = Files.createTempFile("keycloak-oid4vci-conformance-signing", ".p12");
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

    private static X509Certificate generateCaCertificate(KeyPair caKeyPair) throws Exception {
        X500Name caName = new X500Name("CN=OID4VCI Conformance CA");
        X509v3CertificateBuilder builder = certificateBuilder(caName, caName, caKeyPair);
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return sign(builder, caKeyPair);
    }

    private static X509Certificate generateLeafCertificate(KeyPair leafKeyPair, KeyPair caKeyPair,
            X509Certificate caCertificate) throws Exception {
        X500Name caName = new X500Name(caCertificate.getSubjectX500Principal().getName());
        X500Name leafName = new X500Name("CN=OID4VCI Conformance Issuer");
        X509v3CertificateBuilder builder = certificateBuilder(caName, leafName, leafKeyPair);
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        return sign(builder, caKeyPair);
    }

    private static X509v3CertificateBuilder certificateBuilder(X500Name issuer, X500Name subject, KeyPair keyPair) {
        Instant now = Instant.now();
        return new X509v3CertificateBuilder(
                issuer,
                new BigInteger(160, RANDOM),
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
    }

    private static X509Certificate sign(X509v3CertificateBuilder builder, KeyPair signingKeyPair) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signingKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
