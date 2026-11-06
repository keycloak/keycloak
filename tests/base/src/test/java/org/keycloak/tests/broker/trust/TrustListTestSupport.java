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

package org.keycloak.tests.broker.trust;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.ECDSASignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpServer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Creates test certificates and signed ETSI TS 119 602 trust lists, and publishes those lists on
 * the HTTP server of the test, so tests depend on no external trust list provider.
 */
public final class TrustListTestSupport {

    public static final String LOTE_TYPE = "http://uri.etsi.org/19602/LoTEType/EUWalletProvidersList";
    public static final String ISSUANCE_SERVICE = "http://uri.etsi.org/19602/SvcType/WalletSolution/Issuance";
    public static final String REVOCATION_SERVICE = "http://uri.etsi.org/19602/SvcType/WalletSolution/Revocation";

    private TrustListTestSupport() {
    }

    public static KeyPair ecKeyPair() {
        if (!CryptoIntegration.isInitialised()) {
            CryptoIntegration.setProvider(new DefaultCryptoProvider());
        }
        return KeyUtils.generateEcKeyPair("secp256r1");
    }

    public static X509Certificate caCertificate(KeyPair key, String name) {
        return certificate(key.getPublic(), name, name, key, true, KeyUsage.keyCertSign | KeyUsage.cRLSign);
    }

    public static X509Certificate expiredCaCertificate(KeyPair key, String name) {
        return certificate(key.getPublic(), name, name, key, true, KeyUsage.keyCertSign | KeyUsage.cRLSign, Instant.now().minus(1, ChronoUnit.HOURS));
    }

    public static X509Certificate caCertificateWithoutCertificateSigning(KeyPair key, String name) {
        return certificate(key.getPublic(), name, name, key, true, KeyUsage.cRLSign);
    }

    public static X509Certificate subordinateCaCertificate(PublicKey key, String name, X509Certificate issuer, KeyPair issuerKey) {
        return certificate(key, name, commonName(issuer), issuerKey, true, KeyUsage.keyCertSign | KeyUsage.cRLSign);
    }

    public static X509Certificate endEntityCertificate(PublicKey key, String name, X509Certificate issuer, KeyPair issuerKey) {
        return certificate(key, name, commonName(issuer), issuerKey, false, KeyUsage.digitalSignature);
    }

    public static String encode(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, Object> service(String serviceType, X509Certificate... certificates) {
        return Map.of("ServiceInformation", Map.of(
                "ServiceTypeIdentifier", serviceType,
                "ServiceDigitalIdentity", digitalIdentity(certificates)));
    }

    public static Map<String, Object> serviceWithStatus(String serviceType, String status, X509Certificate... certificates) {
        return Map.of("ServiceInformation", Map.of(
                "ServiceTypeIdentifier", serviceType,
                "ServiceStatus", status,
                "ServiceDigitalIdentity", digitalIdentity(certificates)));
    }

    private static Map<String, Object> digitalIdentity(X509Certificate... certificates) {
        return Map.of("X509Certificates", List.of(certificates).stream()
                .map(certificate -> Map.of("val", encode(certificate)))
                .toList());
    }

    /**
     * Publishes the list at the path and returns its URL. The list can be replaced by publishing
     * to the same path again through the returned holder.
     */
    public static PublishedTrustList publish(HttpServer httpServer, String path, String trustList) {
        PublishedTrustList published = new PublishedTrustList(
                "http://" + httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort() + path, trustList);
        httpServer.createContext(path, exchange -> HttpServerUtil.sendResponse(exchange, 200,
                Map.of("Content-Type", List.of("application/jwt")), published.content));
        return published;
    }

    public static final class PublishedTrustList {

        private final String url;
        private volatile String content;

        private PublishedTrustList(String url, String content) {
            this.url = url;
            this.content = content;
        }

        public String url() {
            return url;
        }

        public void replace(String content) {
            this.content = content;
        }
    }

    public static String trustList(KeyPair signer, String loteType, Instant nextUpdate, List<Map<String, Object>> services) {
        Map<String, Object> scheme = new HashMap<>();
        scheme.put("LoTEType", loteType);
        if (nextUpdate != null) {
            scheme.put("NextUpdate", nextUpdate.toString());
        }

        KeyWrapper signingKey = new KeyWrapper();
        signingKey.setAlgorithm(Algorithm.ES256);
        signingKey.setPrivateKey(signer.getPrivate());

        return new JWSBuilder()
                .type("trustlist+jwt")
                .jsonContent(Map.of("LoTE", Map.of(
                        "ListAndSchemeInformation", scheme,
                        "TrustedEntitiesList", List.of(Map.of("TrustedEntityServices", services)))))
                .sign(new ECDSASignatureSignerContext(signingKey));
    }

    private static String commonName(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().getName().substring("CN=".length());
    }

    private static X509Certificate certificate(PublicKey key, String subject, String issuer, KeyPair issuerKey, boolean ca, int keyUsage) {
        return certificate(key, subject, issuer, issuerKey, ca, keyUsage, Instant.now().plus(1, ChronoUnit.DAYS));
    }

    private static X509Certificate certificate(PublicKey key, String subject, String issuer, KeyPair issuerKey, boolean ca, int keyUsage, Instant notAfter) {
        try {
            Instant now = Instant.now();
            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                    new X500Name("CN=" + issuer),
                    new BigInteger(64, new SecureRandom()),
                    Date.from(now.minus(1, ChronoUnit.DAYS)),
                    Date.from(notAfter),
                    new X500Name("CN=" + subject),
                    SubjectPublicKeyInfo.getInstance(key.getEncoded()));
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
            return new JcaX509CertificateConverter().getCertificate(builder.build(
                    new JcaContentSignerBuilder("SHA256withECDSA").build(issuerKey.getPrivate())));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create the test certificate", e);
        }
    }
}
