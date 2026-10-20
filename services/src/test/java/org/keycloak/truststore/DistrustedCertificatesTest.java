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

package org.keycloak.truststore;

import java.io.File;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.def.DefaultCryptoProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DistrustedCertificatesTest {

    // within the validity of every certificate in dv-g3b-emudhra-chain.pem
    private static final Date VALIDATION_DATE = Date.from(Instant.parse("2026-12-01T00:00:00Z"));

    private CryptoProvider originalCryptoProvider;

    @Before
    public void before() {
        originalCryptoProvider = CryptoIntegration.isInitialised() ? CryptoIntegration.getProvider() : null;
        CryptoIntegration.setProvider(new DefaultCryptoProvider());
    }

    @After
    public void after() {
        CryptoIntegration.setProvider(originalCryptoProvider);
    }

    @Test
    public void testCompromisedEmudhraIntermediatesAreDistrusted() throws Exception {
        List<X509Certificate> intermediates = load("emudhra-intermediates.pem");
        assertEquals(16, intermediates.size());
        for (X509Certificate intermediate : intermediates) {
            assertTrue(intermediate.getSubjectX500Principal().getName(), DistrustedCertificates.isDistrusted(intermediate));
        }
    }

    @Test
    public void testNewCertificateForCompromisedKeyIsDistrusted() throws Exception {
        X509Certificate compromised = load("emudhra-intermediates.pem").get(0);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(384);
        KeyPair caKeyPair = generator.generateKeyPair();
        X509Certificate ca = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "Test CA");

        X509Certificate crossSign = CertificateUtils.generateV3Certificate(new KeyPair(compromised.getPublicKey(), null),
                caKeyPair.getPrivate(), ca, "EM DV TLS CA - G3B");

        assertTrue(DistrustedCertificates.isDistrusted(crossSign));
        assertFalse(DistrustedCertificates.isDistrusted(ca));
    }

    @Test
    public void testOtherCertificatesAreNotDistrusted() throws Exception {
        assertFalse(DistrustedCertificates.isDistrusted(load("emsign-ecc-root-ca-g3.pem").get(0)));
        List<X509Certificate> chain = load("dv-g3b-emudhra-chain.pem");
        assertFalse(DistrustedCertificates.isDistrusted(chain.get(0))); // leaf
        assertTrue(DistrustedCertificates.isDistrusted(chain.get(1))); // EM DV TLS CA - G3B
        assertFalse(DistrustedCertificates.isDistrusted(chain.get(2))); // emSign Root TLS CA - G3
        assertFalse(DistrustedCertificates.isDistrusted(null));
    }

    @Test
    public void testCertPathValidationFailsThroughDistrustedIntermediate() throws Exception {
        List<X509Certificate> chain = load("dv-g3b-emudhra-chain.pem");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(load("emsign-ecc-root-ca-g3.pem").get(0), null)));
        params.setRevocationEnabled(false);
        params.setDate(VALIDATION_DATE);
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        // the chain served by the attacker is valid unless the intermediate is distrusted
        assertNotNull(validator.validate(cf.generateCertPath(chain), params));

        params.addCertPathChecker(DistrustedCertificates.certPathChecker());
        CertPathValidatorException e = assertThrows(CertPathValidatorException.class,
                () -> validator.validate(cf.generateCertPath(chain), params));
        assertTrue(e.getMessage(), e.getMessage().contains("EM DV TLS CA - G3B"));
    }

    @Test
    public void testCertPathBuildingFailsThroughDistrustedIntermediate() throws Exception {
        List<X509Certificate> chain = load("dv-g3b-emudhra-chain.pem");
        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(chain.get(0));
        PKIXBuilderParameters params = new PKIXBuilderParameters(Set.of(new TrustAnchor(load("emsign-ecc-root-ca-g3.pem").get(0), null)), selector);
        params.setRevocationEnabled(false);
        params.setDate(VALIDATION_DATE);
        params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(chain)));
        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");

        assertNotNull(builder.build(params));

        params.addCertPathChecker(DistrustedCertificates.certPathChecker());
        assertThrows(CertPathBuilderException.class, () -> builder.build(params));
    }

    @Test
    public void testExtendedTrustManagerRejectsDistrustedChain() throws Exception {
        X509Certificate[] chain = load("dv-g3b-emudhra-chain.pem").toArray(X509Certificate[]::new);
        X509Certificate[] otherChain = load("emsign-ecc-root-ca-g3.pem").toArray(X509Certificate[]::new);
        AcceptingExtendedTrustManager delegate = new AcceptingExtendedTrustManager();
        TrustManager[] wrapped = DistrustedCertificates.wrap(new TrustManager[] { delegate });
        assertEquals(1, wrapped.length);
        assertTrue(wrapped[0] instanceof X509ExtendedTrustManager);
        X509ExtendedTrustManager tm = (X509ExtendedTrustManager) wrapped[0];

        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain, "ECDHE_ECDSA"));
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain, "ECDHE_ECDSA", (Socket) null));
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain, "ECDHE_ECDSA", (SSLEngine) null));
        assertThrows(CertificateException.class, () -> tm.checkClientTrusted(chain, "ECDHE_ECDSA"));
        assertThrows(CertificateException.class, () -> tm.checkClientTrusted(chain, "ECDHE_ECDSA", (Socket) null));
        assertThrows(CertificateException.class, () -> tm.checkClientTrusted(chain, "ECDHE_ECDSA", (SSLEngine) null));
        assertEquals(0, delegate.calls.get());

        tm.checkServerTrusted(otherChain, "ECDHE_ECDSA");
        tm.checkServerTrusted(otherChain, "ECDHE_ECDSA", (Socket) null);
        tm.checkServerTrusted(otherChain, "ECDHE_ECDSA", (SSLEngine) null);
        tm.checkClientTrusted(otherChain, "ECDHE_ECDSA");
        tm.checkClientTrusted(otherChain, "ECDHE_ECDSA", (Socket) null);
        tm.checkClientTrusted(otherChain, "ECDHE_ECDSA", (SSLEngine) null);
        assertEquals(6, delegate.calls.get());
        assertSame(delegate.getAcceptedIssuers(), tm.getAcceptedIssuers());
    }

    @Test
    public void testTrustManagerRejectsDistrustedChain() throws Exception {
        X509Certificate[] chain = load("dv-g3b-emudhra-chain.pem").toArray(X509Certificate[]::new);
        AtomicInteger calls = new AtomicInteger();
        X509TrustManager delegate = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                calls.incrementAndGet();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                calls.incrementAndGet();
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        TrustManager[] wrapped = DistrustedCertificates.wrap(new TrustManager[] { delegate });
        // a plain trust manager must stay plain, so that JSSE still adds its own checks around it
        assertFalse(wrapped[0] instanceof X509ExtendedTrustManager);
        X509TrustManager tm = (X509TrustManager) wrapped[0];

        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain, "ECDHE_ECDSA"));
        assertThrows(CertificateException.class, () -> tm.checkClientTrusted(chain, "ECDHE_ECDSA"));
        assertEquals(0, calls.get());

        tm.checkServerTrusted(new X509Certificate[] { chain[0] }, "ECDHE_ECDSA");
        assertEquals(1, calls.get());
    }

    @Test
    public void testTrustManagersForTruststore() throws Exception {
        KeyStore truststore = TruststoreBuilder.createPkcs12KeyStore();
        truststore.setCertificateEntry("root", load("emsign-ecc-root-ca-g3.pem").get(0));
        X509Certificate[] chain = load("dv-g3b-emudhra-chain.pem").toArray(X509Certificate[]::new);

        TrustManager[] trustManagers = DistrustedCertificates.getTrustManagers(truststore);
        X509TrustManager tm = (X509TrustManager) trustManagers[0];
        CertificateException e = assertThrows(CertificateException.class, () -> tm.checkServerTrusted(chain, "ECDHE_ECDSA"));
        assertTrue(e.getMessage(), e.getMessage().contains("distrusted"));

        assertNotNull(DistrustedCertificates.getTrustManagers(null));
        assertEquals(null, DistrustedCertificates.wrap(null));
    }

    @Test
    public void testTrustManagersIgnoreDistrustedTrustAnchors() throws Exception {
        X509Certificate root = load("emsign-ecc-root-ca-g3.pem").get(0);
        List<X509Certificate> chain = load("dv-g3b-emudhra-chain.pem");
        // a truststore that was not merged by TruststoreBuilder, with the compromised intermediate as a trust anchor
        KeyStore truststore = TruststoreBuilder.createPkcs12KeyStore();
        truststore.setCertificateEntry("root", root);
        truststore.setCertificateEntry("intermediate", chain.get(1));

        X509TrustManager tm = (X509TrustManager) DistrustedCertificates.getTrustManagers(truststore)[0];

        assertEquals(List.of(root), List.of(tm.getAcceptedIssuers()));
        // the peer only sends its leaf, so the chain check alone would not see the intermediate
        assertThrows(CertificateException.class, () -> tm.checkServerTrusted(new X509Certificate[] { chain.get(0) }, "ECDHE_ECDSA"));
        assertEquals(2, truststore.size());
    }

    @Test
    public void testMergedTruststoreSkipsDistrustedCertificates() throws Exception {
        String intermediates = resource("emudhra-intermediates.pem").getAbsolutePath();
        String root = resource("emsign-ecc-root-ca-g3.pem").getAbsolutePath();

        KeyStore truststore = TruststoreBuilder.createMergedTruststore(new String[] { intermediates, root }, false);

        List<String> aliases = Collections.list(truststore.aliases());
        assertEquals(aliases.toString(), 1, aliases.size());
        assertEquals(load("emsign-ecc-root-ca-g3.pem").get(0), truststore.getCertificate(aliases.get(0)));
    }

    private static List<X509Certificate> load(String name) throws Exception {
        try (InputStream in = DistrustedCertificatesTest.class.getResourceAsStream("/distrusted-certificates/" + name)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream().map(X509Certificate.class::cast).toList();
        }
    }

    private static File resource(String name) throws Exception {
        return new File(DistrustedCertificatesTest.class.getResource("/distrusted-certificates/" + name).toURI());
    }

    private static class AcceptingExtendedTrustManager extends X509ExtendedTrustManager {

        private final AtomicInteger calls = new AtomicInteger();
        private final X509Certificate[] acceptedIssuers = new X509Certificate[0];

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            calls.incrementAndGet();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            calls.incrementAndGet();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            calls.incrementAndGet();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            calls.incrementAndGet();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            calls.incrementAndGet();
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            calls.incrementAndGet();
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return acceptedIssuers;
        }
    }
}
