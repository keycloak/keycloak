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

package org.keycloak.tests.oid4vc;

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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// ES256 mdoc signing needs a CA issued certificate, and only the java keystore key provider can supply
// an EC key with one. Its keystore must live in a realm folder under the configured keystores path option.
final class MdocTestSigningKey {

    static final String KEY_ALIAS = "mdoc-test-signing";
    static final String PASSWORD = "password";

    private static final Path KEYSTORES_BASE_DIR;
    private static final String KEY_STORE_PATH;

    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeyPair = keyGenerator.generateKeyPair();
            KeyPair leafKeyPair = keyGenerator.generateKeyPair();

            X500Name caName = new X500Name("CN=Mdoc Test CA");
            X509Certificate caCertificate = generateCaCertificate(caName, caKeyPair);
            X509Certificate leafCertificate = generateLeafCertificate(caName, leafKeyPair, caKeyPair);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEY_ALIAS, leafKeyPair.getPrivate(), PASSWORD.toCharArray(),
                    new Certificate[] { leafCertificate, caCertificate });

            KEYSTORES_BASE_DIR = Files.createTempDirectory("keycloak-mdoc-keystores");
            KEYSTORES_BASE_DIR.toFile().deleteOnExit();
            Path realmDir = Files.createDirectory(
                    KEYSTORES_BASE_DIR.resolve(OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME));
            realmDir.toFile().deleteOnExit();
            Path keyStorePath = Files.createTempFile(realmDir, "keycloak-mdoc-test-signing", ".p12");
            try (OutputStream output = Files.newOutputStream(keyStorePath)) {
                keyStore.store(output, PASSWORD.toCharArray());
            }
            keyStorePath.toFile().deleteOnExit();
            KEY_STORE_PATH = keyStorePath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mdoc test signing key", e);
        }
    }

    private MdocTestSigningKey() {
    }

    static String keystoresBaseDir() {
        return KEYSTORES_BASE_DIR.toString();
    }

    static String keyStorePath() {
        return KEY_STORE_PATH;
    }

    private static X509Certificate generateCaCertificate(X500Name caName, KeyPair caKeyPair) throws Exception {
        X509v3CertificateBuilder builder = certificateBuilder(caName, caName, caKeyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return sign(builder, caKeyPair);
    }

    private static X509Certificate generateLeafCertificate(X500Name caName, KeyPair leafKeyPair, KeyPair caKeyPair)
            throws Exception {
        X500Name leafName = new X500Name("CN=Mdoc Test Signer");
        X509v3CertificateBuilder builder = certificateBuilder(caName, leafName, leafKeyPair.getPublic());
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        return sign(builder, caKeyPair);
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
