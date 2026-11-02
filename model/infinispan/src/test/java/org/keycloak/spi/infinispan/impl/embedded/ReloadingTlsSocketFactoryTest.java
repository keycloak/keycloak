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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jgroups.certificates.Utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class ReloadingTlsSocketFactoryTest {
    @Test
    public void changesProviderWithoutRebindingOrClosingEstablishedConnections() throws Exception {
        assumeTrue(Runtime.version().feature() >= 21);
        CryptoIntegration.init(getClass().getClassLoader());
        var nativeContext = SSLContext.getInstance("TLS");
        nativeContext.init(null, null, null);
        var groups = (String[]) SSLParameters.class.getMethod("getNamedGroups").invoke(nativeContext.getSupportedSSLParameters());
        assumeTrue(groups != null && Arrays.stream(groups).anyMatch("X25519MLKEM768"::equalsIgnoreCase));
        var bc = (java.security.Provider) Class.forName("org.bouncycastle.jsse.provider.BouncyCastleJsseProvider")
                .getConstructor(java.security.Provider.class).newInstance(java.security.Security.getProvider("BC"));
        var rsa = state("RSA", null);
        var pq = state("ML-DSA-65", bc);
        var rollback = state("RSA", null);
        assertEquals("SunJSSE", rsa.context().getProvider().getName());
        assertEquals("BCJSSE", pq.context().getProvider().getName());
        var current = new AtomicReference<>(rsa);
        var factory = new ReloadingTlsSocketFactory(current::get);
        var executor = Executors.newCachedThreadPool();
        try (var listener = factory.createServerSocket("test", 0, 10, InetAddress.getLoopbackAddress());
             var original = connect(factory, listener, executor, "RSA")) {
            exchange(original);
            current.set(pq);
            try (var upgraded = connect(factory, listener, executor, "ML-DSA-65")) {
                exchange(upgraded);
                exchange(original);
                current.set(rollback);
                try (var reverted = connect(factory, listener, executor, "RSA")) {
                    exchange(reverted);
                    exchange(upgraded);
                    exchange(original);
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(6, TimeUnit.SECONDS));
        }
    }

    private static ReloadingTlsSocketFactory.State state(String algorithm, java.security.Provider provider) throws Exception {
        var identity = Utils.generateSelfSignedCertificate(Duration.ofDays(1), algorithm);
        var context = provider == null ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", provider);
        context.init(new KeyManager[]{Utils.createKeyManager(identity, provider)}, new TrustManager[]{Utils.createTrustManager(identity, provider)}, null);
        var parameters = new SSLParameters();
        parameters.setProtocols(new String[]{"TLSv1.3"});
        parameters.setNeedClientAuth(true);
        SSLParameters.class.getMethod("setNamedGroups", String[].class).invoke(parameters, (Object) new String[]{"X25519MLKEM768"});
        if (provider != null) {
            SSLParameters.class.getMethod("setSignatureSchemes", String[].class).invoke(parameters,
                    (Object) new String[]{"mldsa65", "rsa_pss_rsae_sha256", "rsa_pkcs1_sha256"});
        }
        return new ReloadingTlsSocketFactory.State(context, parameters);
    }

    private static Socket connect(ReloadingTlsSocketFactory factory, ServerSocket listener, ExecutorService executor, String algorithm) throws Exception {
        listener.setSoTimeout(5000);
        var authenticated = new java.util.concurrent.CompletableFuture<Void>();
        executor.submit(() -> {
            try (var socket = (SSLSocket) listener.accept()) {
                socket.setSoTimeout(5000);
                socket.startHandshake();
                assertEquals(algorithm, socket.getSession().getPeerCertificates()[0].getPublicKey().getAlgorithm());
                authenticated.complete(null);
                int value;
                while ((value = socket.getInputStream().read()) >= 0) {
                    socket.getOutputStream().write(value + 1);
                    socket.getOutputStream().flush();
                }
            } catch (Throwable e) {
                authenticated.completeExceptionally(e);
            }
        });
        var socket = (SSLSocket) factory.createSocket("test");
        try {
            socket.setSoTimeout(5000);
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), listener.getLocalPort()), 5000);
            socket.startHandshake();
            assertEquals(algorithm, socket.getSession().getPeerCertificates()[0].getPublicKey().getAlgorithm());
            authenticated.get(6, TimeUnit.SECONDS);
            return socket;
        } catch (Throwable e) {
            socket.close();
            throw e;
        }
    }

    private static void exchange(Socket socket) throws Exception {
        socket.getOutputStream().write(42);
        socket.getOutputStream().flush();
        assertEquals(43, socket.getInputStream().read());
    }
}
