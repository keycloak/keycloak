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

package org.keycloak.quarkus.runtime.services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PfxOptions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BouncyCastleTlsEngineOptionsTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void rsaHttpsWithAlpn() throws Exception {
        handshake("RSA", false);
    }

    @Test
    public void rsaHttp2Negotiation() throws Exception {
        handshake("RSA", false, true);
    }

    @Test
    public void nativeMlDsaServerAuthentication() throws Exception {
        Assume.assumeTrue("keytool needs ML-DSA support", Runtime.version().feature() >= 24);
        handshake("ML-DSA-65", false);
    }

    @Test
    public void nativeMlDsaKeyWithMutualTls() throws Exception {
        Assume.assumeTrue("keytool needs ML-DSA support", Runtime.version().feature() >= 24);
        handshake("ML-DSA-65", true);
    }

    @Test
    public void bcSelectsRsaIdentityBySni() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("sni.p12");
        for (String host : List.of("one.example", "two.example")) {
            var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                    "-genkeypair", "-alias", host, "-keyalg", "RSA", "-dname", "CN=" + host,
                    "-ext", "SAN=dns:" + host, "-validity", "1", "-storetype", "PKCS12",
                    "-keystore", file.toString(), "-storepass", "changeit", "-noprompt")
                    .redirectErrorStream(true).start();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS));
            assertEquals(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8), 0, process.exitValue());
        }
        var store = KeyStore.getInstance("PKCS12");
        try (var in = java.nio.file.Files.newInputStream(file)) {
            store.load(in, "changeit".toCharArray());
        }
        var trust = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
        trust.init(store);
        Vertx vertx = Vertx.vertx();
        try {
            // BC handles SNI inside the engine; Vert.x must not reduce the keystore
            // to a single certificate before BC evaluates signature capabilities.
            var options = new HttpServerOptions().setSsl(true).setSni(false).setHost("127.0.0.1").setPort(0)
                    .setKeyCertOptions(new PfxOptions().setPath(file.toString()).setPassword("changeit"))
                    .setSslEngineOptions(new BouncyCastleTlsEngineOptions());
            var server = vertx.createHttpServer(options).requestHandler(request -> request.response().end())
                    .listen().toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
            for (String host : List.of("one.example", "two.example")) {
                var context = SSLContext.getInstance("TLSv1.3", "SunJSSE");
                context.init(null, trust.getTrustManagers(), null);
                try (var tcp = new java.net.Socket("127.0.0.1", server.actualPort());
                     var socket = (SSLSocket) context.getSocketFactory().createSocket(tcp, host, server.actualPort(), true)) {
                    socket.setSoTimeout(10000);
                    var parameters = socket.getSSLParameters();
                    parameters.setServerNames(List.of(new javax.net.ssl.SNIHostName(host)));
                    parameters.setEndpointIdentificationAlgorithm("HTTPS");
                    socket.setSSLParameters(parameters);
                    socket.startHandshake();
                    org.junit.Assert.assertArrayEquals(store.getCertificate(host).getEncoded(),
                            socket.getSession().getPeerCertificates()[0].getEncoded());
                }
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
        }
    }

    private void handshake(String algorithm, boolean mutual) throws Exception {
        handshake(algorithm, mutual, false);
    }

    private void handshake(String algorithm, boolean mutual, boolean http2) throws Exception {
        Path store = temporary.getRoot().toPath().resolve("identity.p12");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "identity", "-keyalg", algorithm, "-dname", "CN=localhost",
                "-validity", "1", "-storetype", "PKCS12", "-keystore", store.toString(),
                "-storepass", "changeit", "-noprompt").redirectErrorStream(true).start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        assertEquals(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8), 0, process.exitValue());

        var crypto = new BouncyCastleProvider();
        java.security.Provider tls = algorithm.equals("RSA")
                ? java.security.Security.getProvider("SunJSSE") : new BouncyCastleJsseProvider(crypto);
        var trusted = KeyStore.getInstance("PKCS12", crypto);
        try (var in = java.nio.file.Files.newInputStream(store)) {
            trusted.load(in, "changeit".toCharArray());
        }
        var trust = TrustManagerFactory.getInstance("PKIX", tls);
        trust.init(trusted);
        var keys = javax.net.ssl.KeyManagerFactory.getInstance("PKIX", tls);
        keys.init(trusted, "changeit".toCharArray());
        var context = SSLContext.getInstance("TLSv1.3", tls);
        context.init(mutual ? keys.getKeyManagers() : null, trust.getTrustManagers(), null);

        Vertx vertx = Vertx.vertx();
        try {
            var pfx = new PfxOptions().setPath(store.toString()).setPassword("changeit");
            var options = new HttpServerOptions().setSsl(true).setHost("127.0.0.1").setPort(0)
                    .setKeyCertOptions(pfx).setSslEngineOptions(new BouncyCastleTlsEngineOptions())
                    .setUseAlpn(true).setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            if (mutual) {
                options.setTrustOptions(pfx).setClientAuth(ClientAuth.REQUIRED);
            }
            var server = vertx.createHttpServer(options).exceptionHandler(Throwable::printStackTrace).requestHandler(request -> request.response().end("BC HTTPS"))
                    .listen().toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
            try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket("127.0.0.1", server.actualPort())) {
                socket.setSoTimeout(10000);
                var parameters = socket.getSSLParameters();
                parameters.setApplicationProtocols(new String[] {http2 ? "h2" : "http/1.1"});
                socket.setSSLParameters(parameters);
                if (algorithm.startsWith("ML-DSA")) {
                    var bcSocket = (org.bouncycastle.jsse.BCSSLSocket) socket;
                    var bcParameters = bcSocket.getParameters();
                    bcParameters.setSignatureSchemes(new String[] {"mldsa65"});
                    bcParameters.setSignatureSchemesCert(new String[] {"mldsa65"});
                    bcSocket.setParameters(bcParameters);
                }
                socket.startHandshake();
                assertEquals("TLSv1.3", socket.getSession().getProtocol());
                assertEquals(http2 ? "h2" : "http/1.1", socket.getApplicationProtocol());
                assertTrue(socket.getSession().getPeerCertificates()[0].getPublicKey().getAlgorithm()
                        .startsWith(algorithm.equals("RSA") ? "RSA" : "ML-DSA"));
                if (http2) {
                    return;
                }
                socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(response, response.contains("200 OK"));
                assertTrue(response, response.endsWith("BC HTTPS"));
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
        }
    }
}
