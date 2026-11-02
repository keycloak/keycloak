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

package org.keycloak.spi.infinispan.impl.embedded;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jgroups.certificates.Utils;

import org.jgroups.util.SocketFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class JGroupsTlsNamedGroupsTest {
    private static SSLContext context;

    @BeforeClass
    public static void init() throws Exception {
        CryptoIntegration.init(JGroupsTlsNamedGroupsTest.class.getClassLoader());
        // Use the same self-signed RSA identity generation and managers as the default provider.
        var certificate = Utils.generateSelfSignedCertificate(Duration.ofDays(1));
        context = SSLContext.getInstance("TLS");
        context.init(new KeyManager[]{Utils.createKeyManager(certificate)},
                new TrustManager[]{Utils.createTrustManager(certificate)}, null);
    }

    @Test
    public void logPolicyDistinguishesRequiredHybridFromFallback() {
        assertEquals("not explicitly required", JGroupsConfigurator.hybridPolicy(null));
        assertEquals("required", JGroupsConfigurator.hybridPolicy(new String[]{"X25519MLKEM768"}));
        assertEquals("allowed with classical fallback", JGroupsConfigurator.hybridPolicy(new String[]{"X25519MLKEM768", "x25519"}));
        assertEquals("not configured", JGroupsConfigurator.hybridPolicy(new String[]{"x25519"}));
    }

    @Test
    public void mldsaMutualTlsThroughBcWithDatabaseEncodedIdentity() throws Exception {
        assumeTrue("Per-connection TLS parameter APIs require Java 21 or later", Runtime.version().feature() >= 21);
        var tls = (java.security.Provider) Class.forName("org.bouncycastle.jsse.provider.BouncyCastleJsseProvider")
                .getConstructor(java.security.Provider.class).newInstance(java.security.Security.getProvider("BC"));
        var identity = Utils.generateSelfSignedCertificate(Duration.ofDays(1), "ML-DSA-65");
        identity = org.keycloak.jgroups.certificates.JGroupsCertificate.fromJson(
                org.keycloak.jgroups.certificates.JGroupsCertificate.toJson(identity));
        var pq = SSLContext.getInstance("TLS", tls);
        pq.init(new KeyManager[]{Utils.createKeyManager(identity, tls)},
                new TrustManager[]{Utils.createTrustManager(identity, tls)}, null);
        var factory = JGroupsConfigurator.createFromContext(pq, new String[]{"X25519MLKEM768"}, new String[]{"mldsa65"});
        // The transport helper checks RSA specifically; exercise PQ handshakes directly.
        var listener = (SSLServerSocket) factory.createServerSocket("test", 0, 1, InetAddress.getLoopbackAddress());
        var executor = Executors.newSingleThreadExecutor();
        try (listener) {
            listener.setSoTimeout(5000);
            var server = executor.submit(() -> {
                try (var peer = (SSLSocket) listener.accept()) {
                    peer.setSoTimeout(5000);
                    peer.startHandshake();
                    return peer.getSession().getPeerCertificates()[0].getPublicKey().getAlgorithm();
                }
            });
            try (var client = (SSLSocket) factory.createSocket("test")) {
                client.setSoTimeout(5000);
                client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), listener.getLocalPort()));
                client.startHandshake();
                assertEquals("ML-DSA-65", client.getSession().getPeerCertificates()[0].getPublicKey().getAlgorithm());
            }
            assertEquals("ML-DSA-65", server.get(6, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void defaultsStillAllowMutualTls() throws Exception {
        var factory = JGroupsConfigurator.createFromContext(context, null);
        exchange(factory, factory, true);
    }

    @Test
    public void configurationIsScopedToBothJGroupsSocketRoles() throws Exception {
        assumeSupports("x25519");
        var global = System.getProperty("jdk.tls.namedGroups");
        var defaults = namedGroups(context.getDefaultSSLParameters());
        var factory = JGroupsConfigurator.createFromContext(context, new String[]{" X25519 "});
        try (var client = (SSLSocket) factory.createSocket("test");
             var server = (SSLServerSocket) factory.createServerSocket("test", 0, 1, InetAddress.getLoopbackAddress())) {
            assertArrayEquals(new String[]{"x25519"}, namedGroups(client.getSSLParameters()));
            assertArrayEquals(new String[]{"x25519"}, namedGroups(server.getSSLParameters()));
            assertArrayEquals(new String[]{"TLSv1.3"}, client.getEnabledProtocols());
            assertTrue(server.getNeedClientAuth());
        }
        assertEquals(global, System.getProperty("jdk.tls.namedGroups"));
        assertArrayEquals(defaults, namedGroups(context.getDefaultSSLParameters()));
        // A separate socket from the same context retains its defaults.
        try (var unrelated = (SSLSocket) context.getSocketFactory().createSocket()) {
            assertArrayEquals(defaults, namedGroups(unrelated.getSSLParameters()));
        }
    }

    @Test
    public void rejectsUnknownEmptyAndDuplicateGroups() throws Exception {
        assumeSupports("x25519");
        for (String[] groups : new String[][]{{"not-a-tls-group"}, {"x25519", "not-a-tls-group"}, {}, {" "}, {"x25519", "X25519"}}) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> JGroupsConfigurator.createFromContext(context, groups));
            assertTrue(failure.getMessage().contains("cache-embedded-mtls-key-exchange-groups"));
        }
    }

    @Test
    public void enforcesGroupsInBothDirections() throws Exception {
        assumeSupports("x25519");
        assumeSupports("secp256r1");
        var x25519 = JGroupsConfigurator.createFromContext(context, new String[]{"x25519"});
        var p256 = JGroupsConfigurator.createFromContext(context, new String[]{"secp256r1"});
        exchange(x25519, x25519, true);
        exchange(p256, p256, true);
        exchange(x25519, p256, false);
        exchange(p256, x25519, false);
    }

    @Test
    public void rejectsHybridGroupWhenUnavailable() throws Exception {
        assumeFalse(supports("X25519MLKEM768"));
        var failure = assertThrows(IllegalArgumentException.class,
                () -> JGroupsConfigurator.createFromContext(context, new String[]{"X25519MLKEM768"}));
        assertTrue(failure.getMessage().contains("cache-embedded-mtls-key-exchange-groups"));
    }

    @Test
    public void hybridWithRsaCertificatesSupportsRollingUpdate() throws Exception {
        String hybrid = "X25519MLKEM768";
        assumeSupports(hybrid);
        assumeSupports("x25519");
        assumeSupports("secp256r1");
        var strict = JGroupsConfigurator.createFromContext(context, new String[]{hybrid});
        var classical = JGroupsConfigurator.createFromContext(context, new String[]{"x25519"});
        var mixed = JGroupsConfigurator.createFromContext(context, new String[]{hybrid, "x25519", "secp256r1"});
        // Both rolling-update phases work in either connection direction.
        exchange(mixed, classical, true);
        exchange(classical, mixed, true);
        exchange(mixed, strict, true);
        exchange(strict, mixed, true);
        exchange(strict, strict, true);
        exchange(classical, strict, false);
        exchange(strict, classical, false);
    }

    private static String[] namedGroups(SSLParameters parameters) throws Exception {
        return (String[]) SSLParameters.class.getMethod("getNamedGroups").invoke(parameters);
    }

    private static void assumeSupports(String group) throws Exception {
        assumeTrue("TLS provider must support " + group, supports(group));
    }

    private static boolean supports(String group) throws Exception {
        if (Runtime.version().feature() < 20) {
            return false;
        }
        var supported = namedGroups(context.getSupportedSSLParameters());
        return supported != null && Arrays.stream(supported).anyMatch(group::equalsIgnoreCase);
    }

    private static void exchange(SocketFactory clients, SocketFactory servers, boolean expected) throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var loopback = InetAddress.getLoopbackAddress();
        try (var listener = (SSLServerSocket) servers.createServerSocket("test", 0, 1, loopback)) {
            listener.setSoTimeout(5000);
            var server = executor.submit(() -> {
                try (var socket = (SSLSocket) listener.accept()) {
                    socket.setSoTimeout(5000);
                    socket.startHandshake();
                    verify(socket);
                    if (socket.getInputStream().read() != 42) {
                        throw new IOException("Missing application data");
                    }
                    socket.getOutputStream().write(43);
                    return true;
                } catch (IOException e) {
                    assertFalse("Timeout is not proof of rejection", e instanceof SocketTimeoutException);
                    if (expected) {
                        throw e;
                    }
                    return false;
                }
            });
            boolean clientOk = false;
            try (var client = (SSLSocket) clients.createSocket("test")) {
                client.setSoTimeout(5000);
                client.connect(new InetSocketAddress(loopback, listener.getLocalPort()), 5000);
                client.startHandshake();
                verify(client);
                client.getOutputStream().write(42);
                clientOk = client.getInputStream().read() == 43;
            } catch (IOException e) {
                assertFalse("Timeout is not proof of rejection", e instanceof SocketTimeoutException);
                if (expected) {
                    throw e;
                }
            }
            boolean serverOk = server.get(6, TimeUnit.SECONDS);
            assertEquals(expected, clientOk && serverOk);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(6, TimeUnit.SECONDS));
        }
    }

    private static void verify(SSLSocket socket) throws Exception {
        assertEquals("TLSv1.3", socket.getSession().getProtocol());
        assertEquals("RSA", socket.getSession().getPeerCertificates()[0].getPublicKey().getAlgorithm());
    }
}
