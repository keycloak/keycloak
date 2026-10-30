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

package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.common.util.CertificateUtils;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Checks that the client certificate is presented in a real TLS handshake, also when the trust manager is disabled.
 */
public class HttpClientBuilderKeyManagersTest {

    private static final char[] PASSWORD = "secret".toCharArray();

    private static HttpsServer server;
    private static KeyStore serverTrustStoreForClient;
    private static KeyManager[] clientKeyManagers;
    private static KeyManager[] chainKeyManagers;
    private static KeyManager[] leafOnlyKeyManagers;
    private static String url;

    @BeforeClass
    public static void startServer() throws Exception {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());

        KeyPair serverKeyPair = generateKeyPair();
        X509Certificate serverCertificate = CertificateUtils.generateV1SelfSignedCertificate(serverKeyPair, "localhost");
        KeyPair clientKeyPair = generateKeyPair();
        X509Certificate clientCertificate = CertificateUtils.generateV1SelfSignedCertificate(clientKeyPair, "the-client");

        KeyStore serverKeyStore = newKeyStore();
        serverKeyStore.setKeyEntry("server", serverKeyPair.getPrivate(), PASSWORD, new X509Certificate[] { serverCertificate });
        KeyPair issuerKeyPair = generateKeyPair();
        X509Certificate issuerCertificate = CertificateUtils.generateV1SelfSignedCertificate(issuerKeyPair, "issuer");
        KeyPair chainClientKeyPair = generateKeyPair();
        X509Certificate chainClientCertificate = CertificateUtils.generateV3Certificate(chainClientKeyPair,
                issuerKeyPair.getPrivate(), issuerCertificate, "chain-client");

        KeyStore serverTrustStore = newKeyStore();
        serverTrustStore.setCertificateEntry("client", clientCertificate);
        serverTrustStore.setCertificateEntry("chain-client", chainClientCertificate);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(serverKeyStore, PASSWORD);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(serverTrustStore);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters sslParameters = getSSLContext().getDefaultSSLParameters();
                sslParameters.setNeedClientAuth(true);
                params.setSSLParameters(sslParameters);
            }
        });
        server.createContext("/", exchange -> {
            try (exchange) {
                int status = 200;
                String client;
                try {
                    SSLSession session = ((HttpsExchange) exchange).getSSLSession();
                    client = session.getPeerPrincipal().getName() + ",certificates=" + session.getPeerCertificates().length;
                } catch (Exception e) {
                    status = 500;
                    client = e.toString();
                }
                byte[] body = client.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        });
        server.start();
        url = "https://localhost:" + server.getAddress().getPort() + "/";

        serverTrustStoreForClient = newKeyStore();
        serverTrustStoreForClient.setCertificateEntry("server", serverCertificate);
        clientKeyManagers = new KeyManager[] {
                new SingleIdentityKeyManager(clientKeyPair.getPrivate(), new X509Certificate[] { clientCertificate }) };
        chainKeyManagers = new KeyManager[] {
                new SingleIdentityKeyManager(chainClientKeyPair.getPrivate(),
                        new X509Certificate[] { chainClientCertificate, issuerCertificate }) };
        leafOnlyKeyManagers = new KeyManager[] {
                new SingleIdentityKeyManager(chainClientKeyPair.getPrivate(), new X509Certificate[] { chainClientCertificate }) };
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static KeyStore newKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        return keyStore;
    }

    private static String get(HttpClientBuilder builder) throws IOException {
        try (CloseableHttpClient client = builder.build(); CloseableHttpResponse response = client.execute(new HttpGet(url))) {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            assertEquals(body, 200, response.getStatusLine().getStatusCode());
            return body;
        }
    }

    @Test
    public void wholeCertificateChainIsPresented() throws Exception {
        assertEquals("CN=chain-client,certificates=2", get(new HttpClientBuilder().keyManagers(chainKeyManagers).disableTrustManager()));
        assertEquals("CN=chain-client,certificates=1", get(new HttpClientBuilder().keyManagers(leafOnlyKeyManagers).disableTrustManager()));
    }

    @Test
    public void clientCertificateIsPresentedWhenTheServerIsTrusted() throws Exception {
        String client = get(new HttpClientBuilder()
                .keyManagers(clientKeyManagers)
                .trustStore(serverTrustStoreForClient)
                .hostnameVerification(HostnameVerificationPolicy.ANY));

        assertEquals("CN=the-client,certificates=1", client);
    }

    @Test
    public void clientCertificateIsPresentedWhenTheTrustManagerIsDisabled() throws Exception {
        String client = get(new HttpClientBuilder()
                .keyManagers(clientKeyManagers)
                .disableTrustManager());

        assertEquals("CN=the-client,certificates=1", client);
    }

}
