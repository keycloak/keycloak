/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.function.Consumer;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyType;
import org.keycloak.http.HttpRequest;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.services.resteasy.HttpRequestImpl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;

/**
 * Tests for {@link TraefikProxySslClientCertificateLookup}.
 */
public class TraefikProxySslClientCertificateLookupTest {

    private static final String CLIENT_CERT_HEADER = "X-Forwarded-Tls-Client-Cert";

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static String singleCertHeaderValue;
    private static String multiCertHeaderValue;

    private static class UntrustedHttpRequestImpl extends HttpRequestImpl {
        public UntrustedHttpRequestImpl(MockHttpRequest delegate) {
            super(delegate);
        }

        @Override
        public boolean isProxyTrusted() {
            return false;
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
        kpg.initialize(2048);

        KeyPair keyPairClient = kpg.generateKeyPair();
        X509Certificate clientCert = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=test-client", new Date(),
                        new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365), keyPairClient);

        KeyPair keyPairCa = kpg.generateKeyPair();
        X509Certificate caCert = CryptoIntegration.getProvider().getCertificateUtils()
                .createServicesTestCertificate("CN=test-ca", new Date(),
                        new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365), keyPairCa);

        // Traefik URL-encodes each individual PEM (with BEGIN/END headers) and joins them with a comma (not URL-encoded).
        // PemUtils.encodeCertificate returns raw base64 without headers, so we wrap it in PEM headers.
        String pemClientCert = PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(clientCert));
        String pemCaCert = PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(caCert));
        String encodedClientCert = URLEncoder.encode(pemClientCert, StandardCharsets.UTF_8);
        String encodedCaCert = URLEncoder.encode(pemCaCert, StandardCharsets.UTF_8);

        singleCertHeaderValue = encodedClientCert;
        multiCertHeaderValue = encodedClientCert + "," + encodedCaCert;
    }

    @Test
    public void testRequestFromUntrustedProxyIsDiscarded() throws GeneralSecurityException {
        TraefikProxySslClientCertificateLookup subject = createSubject();
        HttpRequest httpRequest = createHttpRequest(headers -> headers.add(CLIENT_CERT_HEADER, singleCertHeaderValue), false);

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(nullValue()));
    }

    @Test
    public void testSingleCertInHeader() throws GeneralSecurityException {
        TraefikProxySslClientCertificateLookup subject = createSubject();
        HttpRequest httpRequest = createHttpRequest(headers -> headers.add(CLIENT_CERT_HEADER, singleCertHeaderValue));

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(arrayWithSize(1)));
    }

    @Test
    public void testMultipleCertsInHeader() throws GeneralSecurityException {
        TraefikProxySslClientCertificateLookup subject = createSubject();
        HttpRequest httpRequest = createHttpRequest(headers -> headers.add(CLIENT_CERT_HEADER, multiCertHeaderValue));

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(arrayWithSize(2)));
    }

    @Test
    public void testEmptyChainOnMissingHeader() throws GeneralSecurityException {
        TraefikProxySslClientCertificateLookup subject = createSubject();
        HttpRequest httpRequest = createHttpRequest(headers -> {});

        X509Certificate[] actualChain = subject.getCertificateChain(httpRequest);

        assertThat(actualChain, is(not(nullValue())));
        assertThat(actualChain, is(emptyArray()));
    }

    private static TraefikProxySslClientCertificateLookup createSubject() {
        return new TraefikProxySslClientCertificateLookup(CLIENT_CERT_HEADER);
    }

    private static HttpRequest createHttpRequest(Consumer<MultivaluedMap<String, String>> configurer) {
        return createHttpRequest(configurer, true);
    }

    private static HttpRequest createHttpRequest(Consumer<MultivaluedMap<String, String>> configurer, boolean fromTrustedProxy) {
        MockHttpRequest requestMock;
        try {
            requestMock = MockHttpRequest.get("foo");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        configurer.accept(requestMock.getMutableHeaders());
        if (fromTrustedProxy) {
            return new HttpRequestImpl(requestMock);
        } else {
            return new UntrustedHttpRequestImpl(requestMock);
        }
    }
}
