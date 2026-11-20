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

package org.keycloak.jgroups.certificates;

import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.keycloak.common.crypto.CryptoIntegration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A node that starts with a different key replaces the stored certificate, while the running nodes still use the old one.
 * Verifies that the running node notices the mismatch, so it can reload the certificate, and that a retry succeeds afterwards.
 * When the running node connects to the new node, its trust manager rejects the certificate.
 * When the new node connects to the running node, the new node aborts the handshake, and the running node only sees the request for its accepted issuers.
 */
public class CertificateMismatchHandshakeTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CertificateMismatchHandshakeTest.class.getClassLoader());
    }

    @Test
    public void testRunningNodeReloadsWhenItConnectsToTheNewNode() throws Exception {
        var running = new Node(Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(2048)));
        var joining = new Node(Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(3072)));

        Assert.assertFalse(handshake(running, joining));
        Assert.assertEquals(1, running.exceptions.get());

        running.reload(joining.certificate);
        Assert.assertTrue(handshake(running, joining));
    }

    @Test
    public void testRunningNodeReloadsWhenTheNewNodeConnectsToIt() throws Exception {
        var running = new Node(Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(2048)));
        var joining = new Node(Utils.generateSelfSignedCertificate(Duration.ofDays(1), new KeySpec.Rsa(3072)));
        Assert.assertFalse(handshake(joining, running));
        Assert.assertEquals("The running node must be asked for its accepted issuers", 1, running.acceptedIssuers.get());
    }

    // Returns true if the handshake of the client with the server succeeds.
    private static boolean handshake(Node client, Node server) throws Exception {
        try (var serverSocket = server.serverSocketFactory.createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            var sslServer = (javax.net.ssl.SSLServerSocket) serverSocket;
            sslServer.setNeedClientAuth(true);
            var serverThread = new Thread(() -> {
                try (var socket = (SSLSocket) sslServer.accept()) {
                    socket.startHandshake();
                } catch (Exception e) {
                    // the failure is observed by the client and the trust managers
                }
            });
            serverThread.start();
            try (var socket = (SSLSocket) client.context.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
                socket.startHandshake();
                // TLS 1.3 reports a rejected client certificate on the first read only
                socket.getInputStream().read();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                serverThread.join();
            }
        }
    }

    private static final class Node {
        final JGroupsCertificate certificate;
        final AtomicInteger exceptions = new AtomicInteger();
        final ReloadingX509ExtendedKeyManager keyManager = new ReloadingX509ExtendedKeyManager();
        final ReloadingX509ExtendedTrustManager trustManager = new ReloadingX509ExtendedTrustManager();
        final SSLContext context;
        final AtomicInteger acceptedIssuers = new AtomicInteger();
        final javax.net.ServerSocketFactory serverSocketFactory;

        Node(JGroupsCertificate certificate) throws Exception {
            this.certificate = certificate;
            reload(certificate);
            trustManager.setExceptionHandler(exceptions::incrementAndGet);
            trustManager.setAcceptedIssuersHandler(acceptedIssuers::incrementAndGet);
            context = SSLContext.getInstance("TLS");
            context.init(new javax.net.ssl.KeyManager[]{keyManager}, new javax.net.ssl.TrustManager[]{trustManager}, null);
            serverSocketFactory = context.getServerSocketFactory();
        }

        void reload(JGroupsCertificate certificate) throws Exception {
            keyManager.reload(Utils.createKeyManager(certificate));
            trustManager.reload(Utils.createTrustManager(certificate));
        }
    }
}
