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

package org.keycloak.services.x509;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.http.HttpRequest;
import org.keycloak.services.resteasy.HttpRequestImpl;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class DistrustedClientCertificateLookupTest {

    private static final String CLIENT_CERT_HEADER = "ssl-client-cert";

    private static X509Certificate ca;
    private static X509Certificate clientCert;
    private static X509Certificate distrustedClientCert;

    @BeforeClass
    public static void init() throws Exception {
        CryptoIntegration.init(DistrustedClientCertificateLookupTest.class.getClassLoader());

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair caKeyPair = generator.generateKeyPair();
        ca = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, "Test CA");
        clientCert = CertificateUtils.generateV3Certificate(generator.generateKeyPair(), caKeyPair.getPrivate(), ca, "test-client");

        // a certificate for the key of a compromised intermediate, issued by a CA that is trusted
        X509Certificate compromised;
        try (InputStream in = DistrustedClientCertificateLookupTest.class.getResourceAsStream("/distrusted-certificates/emudhra-intermediates.pem")) {
            compromised = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        distrustedClientCert = CertificateUtils.generateV3Certificate(new KeyPair(compromised.getPublicKey(), null),
                caKeyPair.getPrivate(), ca, "EM DV TLS CA - G3B");
    }

    @Test
    public void testNginxLookupRejectsDistrustedChain() throws Exception {
        assertArrayEquals(new X509Certificate[] { clientCert }, nginxLookup().getCertificateChain(headerRequest(clientCert)));

        // without trust validation by the proxy or a revalidation of the certificate, the rebuilt chain is all there is
        assertThrows(CertificateException.class, () -> nginxLookup().getCertificateChain(headerRequest(distrustedClientCert)));
    }

    @Test
    public void testNginxLookupRejectsLeafIssuedByDistrustedCertificate() throws Exception {
        List<X509Certificate> chain = loadChain();
        // the compromised intermediate is not in the truststore, so the chain cannot be rebuilt beyond the leaf
        NginxProxySslClientCertificateLookup lookup = new NginxProxySslClientCertificateLookup(CLIENT_CERT_HEADER, "ssl-client-cert-chain", 2,
                new HashSet<>(Set.of(chain.get(2))), new HashSet<>(Set.of(loadRoot())), true, false);

        assertThrows(CertificateException.class, () -> lookup.getCertificateChain(headerRequest(chain.get(0))));
    }

    @Test
    public void testRfc9440LookupRejectsLeafIssuedByDistrustedCertificate() throws Exception {
        Rfc9440ClientCertificateLookup lookup = new Rfc9440ClientCertificateLookup("Client-Cert", "Client-Cert-Chain", 1);

        assertArrayEquals(new X509Certificate[] { clientCert }, lookup.getCertificateChain(rfc9440Request(clientCert)));

        // the chain header is optional, so the proxy may forward only the leaf
        assertThrows(CertificateException.class, () -> lookup.getCertificateChain(rfc9440Request(loadChain().get(0))));
    }

    @Test
    public void testDefaultLookupRejectsDistrustedChain() throws Exception {
        DefaultClientCertificateLookup lookup = new DefaultClientCertificateLookup();

        X509Certificate[] chain = { clientCert, ca };
        assertArrayEquals(chain, lookup.getCertificateChain(tlsRequest(chain)));

        assertThrows(CertificateException.class, () -> lookup.getCertificateChain(tlsRequest(distrustedClientCert, ca)));
    }

    private static NginxProxySslClientCertificateLookup nginxLookup() {
        return new NginxProxySslClientCertificateLookup(CLIENT_CERT_HEADER, "ssl-client-cert-chain", 1,
                new HashSet<>(), new HashSet<>(Set.of(ca)), true, false);
    }

    private static HttpRequest headerRequest(X509Certificate cert) throws Exception {
        MockHttpRequest request = MockHttpRequest.get("foo");
        request.getMutableHeaders().add(CLIENT_CERT_HEADER, Base64.getEncoder().encodeToString(cert.getEncoded()));
        return new HttpRequestImpl(request);
    }

    private static HttpRequest rfc9440Request(X509Certificate cert) throws Exception {
        MockHttpRequest request = MockHttpRequest.get("foo");
        request.getMutableHeaders().add("Client-Cert", ":" + Base64.getEncoder().encodeToString(cert.getEncoded()) + ":");
        return new HttpRequestImpl(request);
    }

    private static List<X509Certificate> loadChain() throws Exception {
        try (InputStream in = DistrustedClientCertificateLookupTest.class.getResourceAsStream("/distrusted-certificates/dv-g3b-emudhra-chain.pem")) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in).stream().map(X509Certificate.class::cast).toList();
        }
    }

    private static X509Certificate loadRoot() throws Exception {
        try (InputStream in = DistrustedClientCertificateLookupTest.class.getResourceAsStream("/distrusted-certificates/emsign-ecc-root-ca-g3.pem")) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    private static HttpRequest tlsRequest(X509Certificate... chain) throws Exception {
        MockHttpRequest request = MockHttpRequest.get("foo");
        request.setAttribute("jakarta.servlet.request.X509Certificate", chain);
        return new HttpRequestImpl(request);
    }
}
