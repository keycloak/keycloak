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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Assert;
import org.junit.Test;

public class LDAPStartTlsCloseTest {

    @Test
    public void trackingFactoryCapturesNegotiatedSocket() throws Exception {
        TestSSLSocket socket = new TestSSLSocket();
        AtomicReference<SSLSocket> captured = new AtomicReference<>();
        AtomicReference<Socket> transport = new AtomicReference<>();
        SSLSocketFactory factory = LDAPStartTlsClose.trackingFactory(new TestSSLSocketFactory(socket), captured, transport);
        Socket rawSocket = new Socket();

        Assert.assertSame(socket, factory.createSocket(rawSocket, "localhost", 389, true));
        Assert.assertSame(socket, captured.get());
        Assert.assertSame(rawSocket, transport.get());
    }

    @Test
    public void closeTransportBeforeTlsResponse() {
        TestSSLSocket socket = new TestSSLSocket();
        TestTransportSocket transport = new TestTransportSocket();
        TestStartTlsResponse response = new TestStartTlsResponse(() ->
                Assert.assertTrue("transport must be closed before tlsResponse.close()", transport.closed));

        LDAPStartTlsClose.close(response, socket, transport);

        Assert.assertEquals(1, response.closeCalls);
        Assert.assertTrue(transport.closed);
    }

    @Test
    public void closeSocketWhenNoTransport() {
        TestSSLSocket socket = new TestSSLSocket();
        TestStartTlsResponse response = new TestStartTlsResponse(() ->
                Assert.assertTrue("socket must be closed before tlsResponse.close()", socket.closed));

        LDAPStartTlsClose.close(response, socket, null);

        Assert.assertEquals(1, response.closeCalls);
        Assert.assertTrue(socket.closed);
    }

    @Test
    public void closeTlsResponseCalledEvenWhenTransportCloseThrows() {
        TestSSLSocket socket = new TestSSLSocket() {
            @Override public void close() { throw new RuntimeException("close failed"); }
        };
        TestStartTlsResponse response = new TestStartTlsResponse(() -> {});

        LDAPStartTlsClose.close(response, socket, null);

        Assert.assertEquals(1, response.closeCalls);
    }

    @Test
    public void realTlsShutdownUnblocksBlockedReader() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = getClass().getResourceAsStream("/test-keystore.p12")) {
            Assert.assertNotNull("test-keystore.p12 must exist in test resources", in);
            ks.load(in, "password".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("test-server", ks.getCertificate("test"));
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext serverContext = SSLContext.getInstance("TLSv1.2");
        serverContext.init(kmf.getKeyManagers(), null, null);

        SSLContext clientContext = SSLContext.getInstance("TLSv1.2");
        clientContext.init(null, tmf.getTrustManagers(), null);

        ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Socket clientTcp = null;
        Socket serverTcp = null;
        SSLSocket clientTls = null;
        SSLSocket serverTls = null;

        try {
            int port = serverSocket.getLocalPort();
            clientTcp = new Socket(InetAddress.getLoopbackAddress(), port);
            serverTcp = serverSocket.accept();

            serverTls = (SSLSocket) serverContext.getSocketFactory().createSocket(serverTcp, "localhost", port, false);
            serverTls.setUseClientMode(false);

            clientTls = (SSLSocket) clientContext.getSocketFactory().createSocket(clientTcp, "localhost", port, false);
            clientTls.setUseClientMode(true);

            final SSLSocket finalServerTls = serverTls;
            Thread serverHandshake = new Thread(() -> {
                try {
                    finalServerTls.startHandshake();
                } catch (IOException ignored) {}
            });
            serverHandshake.setDaemon(true);
            serverHandshake.start();
            clientTls.startHandshake();
            serverHandshake.join(5000);
            Assert.assertFalse("Server handshake did not finish", serverHandshake.isAlive());
            Assert.assertEquals("TLSv1.2", clientTls.getSession().getProtocol());
            Assert.assertEquals("TLSv1.2", serverTls.getSession().getProtocol());

            final SSLSocket finalClientTls = clientTls;
            final Socket finalClientTcp = clientTcp;

            StartTlsResponse tlsResponse = new StartTlsResponse() {
                @Override public void setEnabledCipherSuites(String[] suites) {}
                @Override public void setHostnameVerifier(HostnameVerifier verifier) {}
                @Override public SSLSession negotiate() { return null; }
                @Override public SSLSession negotiate(SSLSocketFactory factory) { return null; }
                @Override public void close() throws IOException { finalClientTls.close(); }
            };

            CountDownLatch readerStarted = new CountDownLatch(1);
            Thread readerThread = new Thread(() -> {
                readerStarted.countDown();
                try {
                    finalClientTls.getInputStream().read();
                } catch (IOException ignored) {}
            });
            readerThread.setDaemon(true);
            readerThread.start();
            Assert.assertTrue(readerStarted.await(5, TimeUnit.SECONDS));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean blockedInSocketRead = false;
            while (System.nanoTime() < deadline) {
                StackTraceElement[] stack = readerThread.getStackTrace();
                blockedInSocketRead = readerThread.getState() == Thread.State.RUNNABLE
                        && isSocketReadBlocked(stack);
                if (blockedInSocketRead) {
                    break;
                }
                Thread.sleep(10);
            }
            Assert.assertTrue("Reader thread was not blocked in the underlying socket read: "
                    + java.util.Arrays.toString(readerThread.getStackTrace()), blockedInSocketRead);

            Thread shutdownThread = new Thread(() -> {
                LDAPStartTlsClose.close(tlsResponse, finalClientTls, finalClientTcp);
            });
            shutdownThread.setDaemon(true);
            shutdownThread.start();

            shutdownThread.join(5000);
            Assert.assertFalse("Shutdown thread hung", shutdownThread.isAlive());

            readerThread.join(5000);
            Assert.assertFalse("Reader thread hung", readerThread.isAlive());
        } finally {
            closeQuietly(clientTcp);
            closeQuietly(serverTcp);
            closeQuietly(serverSocket);
            closeQuietly(clientTls);
            closeQuietly(serverTls);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {}
        }
    }

    private static boolean isSocketReadBlocked(StackTraceElement[] stack) {
        for (StackTraceElement element : stack) {
            if (("sun.nio.ch.SocketDispatcher".equals(element.getClassName()) && "read0".equals(element.getMethodName()))
                    || ("java.net.SocketInputStream".equals(element.getClassName()) && "socketRead0".equals(element.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static final class TestTransportSocket extends Socket {
        private boolean closed;
        private int soTimeout;

        @Override
        public void setSoTimeout(int timeout) {
            soTimeout = timeout;
        }

        @Override
        public int getSoTimeout() {
            return soTimeout;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class TestStartTlsResponse extends StartTlsResponse {
        private final Runnable closeAction;
        private int closeCalls;

        private TestStartTlsResponse(Runnable closeAction) {
            this.closeAction = closeAction;
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
        }

        @Override
        public void setHostnameVerifier(HostnameVerifier verifier) {
        }

        @Override
        public SSLSession negotiate() {
            return null;
        }

        @Override
        public SSLSession negotiate(SSLSocketFactory factory) {
            return null;
        }

        @Override
        public void close() throws IOException {
            closeCalls++;
            closeAction.run();
        }
    }

    private static class TestSSLSocket extends SSLSocket {
        private int soTimeout;
        private boolean closed;

        @Override
        public void setSoTimeout(int timeout) {
            soTimeout = timeout;
        }

        @Override
        public int getSoTimeout() {
            return soTimeout;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override public String[] getSupportedCipherSuites() { return new String[0]; }
        @Override public String[] getEnabledCipherSuites() { return new String[0]; }
        @Override public void setEnabledCipherSuites(String[] suites) { }
        @Override public String[] getSupportedProtocols() { return new String[0]; }
        @Override public String[] getEnabledProtocols() { return new String[0]; }
        @Override public void setEnabledProtocols(String[] protocols) { }
        @Override public SSLSession getSession() { return null; }
        @Override public void addHandshakeCompletedListener(HandshakeCompletedListener listener) { }
        @Override public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) { }
        @Override public void startHandshake() { }
        @Override public void setUseClientMode(boolean mode) { }
        @Override public boolean getUseClientMode() { return false; }
        @Override public void setNeedClientAuth(boolean need) { }
        @Override public boolean getNeedClientAuth() { return false; }
        @Override public void setWantClientAuth(boolean want) { }
        @Override public boolean getWantClientAuth() { return false; }
        @Override public void setEnableSessionCreation(boolean flag) { }
        @Override public boolean getEnableSessionCreation() { return false; }
    }

    private static final class TestSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocket socket;

        private TestSSLSocketFactory(SSLSocket socket) {
            this.socket = socket;
        }

        @Override public String[] getDefaultCipherSuites() { return new String[0]; }
        @Override public String[] getSupportedCipherSuites() { return new String[0]; }
        @Override public Socket createSocket(Socket s, String host, int port, boolean autoClose) { return socket; }
        @Override public Socket createSocket(String host, int port) { return socket; }
        @Override public Socket createSocket(String host, int port, InetAddress localHost, int localPort) { return socket; }
        @Override public Socket createSocket(InetAddress host, int port) { return socket; }
        @Override public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) { return socket; }
    }

}
