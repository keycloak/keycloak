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
import java.util.EnumMap;
import java.util.Map;

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

    /** The different certificate-chain scenarios, each stored in its own keystore. */
    public enum Variant {
        /** Leaf issued by a self-signed root CA: {@code [leaf, self-signed root]}. */
        V1("self-signed-root"),
        /** Leaf issued by a non-self-signed CA:TRUE intermediate: {@code [leaf, CA:TRUE intermediate]}. */
        V2("ca-intermediate"),
        /** Leaf issued by an intermediate CA, issued by a self-signed root: {@code [leaf, intermediate, root]}. */
        V3("full-chain"),
        /** Leaf issued by a non-CA intermediate: {@code [leaf, non-CA intermediate]}. */
        V4("non-ca-intermediate");

        private final String fileName;
        Variant(String fileName) {
            this.fileName = fileName;
        }
    }

    private static final Path KEYSTORES_BASE_DIR;
    private static final Map<Variant, Path> KEY_STORE_PATHS = new EnumMap<>(Variant.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));

            KEYSTORES_BASE_DIR = Files.createTempDirectory("keycloak-mdoc-keystores");
            KEYSTORES_BASE_DIR.toFile().deleteOnExit();
            Path realmDir = Files.createDirectory(
                    KEYSTORES_BASE_DIR.resolve(OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME));
            realmDir.toFile().deleteOnExit();

            // Each variant is stored in its own PKCS12 keystore under a single KEY_ALIAS entry. Using a separate
            // keystore per chain avoids PKCS12 dropping a trailing self-signed root when several chains share one store.
            KEY_STORE_PATHS.put(Variant.V1, writeKeystore(realmDir, Variant.V1, selfSignedRootChain(keyGenerator)));
            KEY_STORE_PATHS.put(Variant.V2, writeKeystore(realmDir, Variant.V2, caIntermediateChain(keyGenerator)));
            KEY_STORE_PATHS.put(Variant.V3, writeKeystore(realmDir, Variant.V3, fullChain(keyGenerator)));
            KEY_STORE_PATHS.put(Variant.V4, writeKeystore(realmDir, Variant.V4, nonCaIntermediateChain(keyGenerator)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mdoc test signing key", e);
        }
    }

    private MdocTestSigningKey() {
    }

    static String keystoresBaseDir() {
        return KEYSTORES_BASE_DIR.toString();
    }

    static String keyStorePath(Variant variant) {
        return KEY_STORE_PATHS.get(variant).toString();
    }

    /** Stores a key entry under {@code KEY_ALIAS} in a new PKCS12 keystore. */
    private static Path writeKeystore(Path realmDir, Variant variant, KeyChain keyChain) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(KEY_ALIAS, keyChain.leafKeyPair().getPrivate(), PASSWORD.toCharArray(), keyChain.chain());

        Path keyStorePath = Files.createTempFile(realmDir, "keycloak-mdoc-test-signing-" + variant.fileName, ".p12");
        try (OutputStream output = Files.newOutputStream(keyStorePath)) {
            keyStore.store(output, PASSWORD.toCharArray());
        }
        keyStorePath.toFile().deleteOnExit();
        return keyStorePath;
    }

    /** A leaf issued by a self-signed root CA: {@code [leaf, root]}. */
    private static KeyChain selfSignedRootChain(KeyPairGenerator keyGenerator) throws Exception {
        KeyPair rootKeyPair = keyGenerator.generateKeyPair();
        KeyPair leafKeyPair = keyGenerator.generateKeyPair();
        X500Name rootName = new X500Name("CN=Mdoc Test CA");
        X509Certificate root = generateCaCertificate(rootName, rootName, rootKeyPair.getPublic(), rootKeyPair);
        X509Certificate leaf = generateLeafCertificate(rootName, leafKeyPair, rootKeyPair);
        return new KeyChain(leafKeyPair, new Certificate[] { leaf, root });
    }

    /** A leaf issued by a non-self-signed CA:TRUE intermediate (external root omitted): {@code [leaf, intermediate]}. */
    private static KeyChain caIntermediateChain(KeyPairGenerator keyGenerator) throws Exception {
        KeyPair externalRootKeyPair = keyGenerator.generateKeyPair();
        KeyPair intermediateKeyPair = keyGenerator.generateKeyPair();
        KeyPair leafKeyPair = keyGenerator.generateKeyPair();
        X500Name rootName = new X500Name("CN=Mdoc Test External Root");
        X500Name intermediateName = new X500Name("CN=Mdoc Test Intermediate CA");
        X509Certificate intermediate = generateCaCertificate(rootName, intermediateName,
                intermediateKeyPair.getPublic(), externalRootKeyPair);
        X509Certificate leaf = generateLeafCertificate(intermediateName, leafKeyPair, intermediateKeyPair);
        return new KeyChain(leafKeyPair, new Certificate[] { leaf, intermediate });
    }

    /** A leaf -> intermediate CA -> self-signed root chain: {@code [leaf, intermediate, root]}. */
    private static KeyChain fullChain(KeyPairGenerator keyGenerator) throws Exception {
        KeyPair rootKeyPair = keyGenerator.generateKeyPair();
        KeyPair intermediateKeyPair = keyGenerator.generateKeyPair();
        KeyPair leafKeyPair = keyGenerator.generateKeyPair();
        X500Name rootName = new X500Name("CN=Mdoc Test Root");
        X500Name intermediateName = new X500Name("CN=Mdoc Test Intermediate CA");
        X509Certificate root = generateCaCertificate(rootName, rootName, rootKeyPair.getPublic(), rootKeyPair);
        X509Certificate intermediate = generateCaCertificate(rootName, intermediateName,
                intermediateKeyPair.getPublic(), rootKeyPair);
        X509Certificate leaf = generateLeafCertificate(intermediateName, leafKeyPair, intermediateKeyPair);
        return new KeyChain(leafKeyPair, new Certificate[] { leaf, intermediate, root });
    }

    /** A leaf issued by a non-CA intermediate (external root omitted): {@code [leaf, intermediate]}. */
    private static KeyChain nonCaIntermediateChain(KeyPairGenerator keyGenerator) throws Exception {
        KeyPair externalRootKeyPair = keyGenerator.generateKeyPair();
        KeyPair intermediateKeyPair = keyGenerator.generateKeyPair();
        KeyPair leafKeyPair = keyGenerator.generateKeyPair();
        X500Name rootName = new X500Name("CN=Mdoc Test External Root");
        X500Name intermediateName = new X500Name("CN=Mdoc Test Intermediate");
        X509Certificate intermediate = generateNonCaCertificate(rootName, intermediateName,
                intermediateKeyPair.getPublic(), externalRootKeyPair);
        X509Certificate leaf = generateLeafCertificate(intermediateName, leafKeyPair, intermediateKeyPair);
        return new KeyChain(leafKeyPair, new Certificate[] { leaf, intermediate });
    }

    /**
     * Creates a CA certificate (marked CA:TRUE) issued by the given issuer.
     * <p>
     * Pass the same issuer and subject to produce a self-signed CA, or a different subject to produce an
     * intermediate CA issued by {@code issuerName}.
     *
     * @param issuerName the issuer of the certificate
     * @param subjectName the subject of the certificate
     * @param publicKey the public key to bind
     * @param issuerKeyPair the key pair signing the certificate
     */
    private static X509Certificate generateCaCertificate(X500Name issuerName, X500Name subjectName,
            PublicKey publicKey, KeyPair issuerKeyPair) throws Exception {
        X509v3CertificateBuilder builder = certificateBuilder(issuerName, subjectName, publicKey);
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return sign(builder, issuerKeyPair);
    }

    /**
     * Creates a non-CA certificate that is still allowed to sign (keyCertSign) but is not marked CA:TRUE.
     * <p>
     * Used to model intermediates that wallets should treat as part of the chain rather than as a trust anchor.
     *
     * @param issuerName the issuer of the certificate
     * @param subjectName the subject of the certificate
     * @param publicKey the public key to bind
     * @param issuerKeyPair the key pair signing the certificate
     */
    private static X509Certificate generateNonCaCertificate(X500Name issuerName, X500Name subjectName,
            PublicKey publicKey, KeyPair issuerKeyPair) throws Exception {
        X509v3CertificateBuilder builder = certificateBuilder(issuerName, subjectName, publicKey);
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign));
        return sign(builder, issuerKeyPair);
    }

    /**
     * Creates a leaf certificate for digital signatures, issued by the given issuer.
     *
     * @param issuerName the issuer of the certificate
     * @param leafKeyPair the key pair bound to the leaf certificate
     * @param issuerKeyPair the key pair signing the certificate
     */
    private static X509Certificate generateLeafCertificate(X500Name issuerName, KeyPair leafKeyPair,
            KeyPair issuerKeyPair) throws Exception {
        X500Name leafName = new X500Name("CN=Mdoc Test Signer");
        X509v3CertificateBuilder builder = certificateBuilder(issuerName, leafName, leafKeyPair.getPublic());
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        return sign(builder, issuerKeyPair);
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

    /** A leaf private key paired with the certificate chain stored in the keystore. */
    private record KeyChain(KeyPair leafKeyPair, Certificate[] chain) {
    }
}
