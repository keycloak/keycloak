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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
    public void closeTemporarilyBoundsSocketReadTimeout() {
        TestSSLSocket socket = new TestSSLSocket();
        socket.soTimeout = 125;
        TestStartTlsResponse response = new TestStartTlsResponse(() -> Assert.assertEquals(3_000, socket.soTimeout));

        LDAPStartTlsClose.close(response, socket, null);

        Assert.assertEquals(1, response.closeCalls);
        Assert.assertEquals(125, socket.soTimeout);
        Assert.assertFalse(socket.closed);
    }

    @Test
    public void closeForciblyClosesSocketWhenTlsShutdownTimesOut() {
        TestSSLSocket socket = new TestSSLSocket();
        TestTransportSocket transport = new TestTransportSocket();
        socket.soTimeout = 125;
        TestStartTlsResponse response = new TestStartTlsResponse(() -> {
            Assert.assertEquals(3_000, socket.soTimeout);
            throw new CloseActionException(new SocketTimeoutException("peer did not send close_notify"));
        });

        LDAPStartTlsClose.close(response, socket, transport);

        Assert.assertEquals(1, response.closeCalls);
        Assert.assertFalse(socket.closed);
        Assert.assertTrue(transport.closed);
        Assert.assertEquals(3_000, socket.soTimeout);
    }

    @Test
    public void closeRestoresTimeoutOnTransportWhenSslSocketIsClosed() {
        TestSSLSocket socket = new TestSSLSocket();
        TestTransportSocket transport = new TestTransportSocket();
        socket.soTimeout = 125;
        transport.soTimeout = 125;
        TestStartTlsResponse response = new TestStartTlsResponse(() -> {
            Assert.assertEquals(3_000, socket.soTimeout);
            socket.close();
        });

        LDAPStartTlsClose.close(response, socket, transport);

        Assert.assertEquals(1, response.closeCalls);
        Assert.assertTrue(socket.closed);
        Assert.assertFalse(transport.closed);
        Assert.assertEquals(125, transport.soTimeout);
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
            try {
                closeAction.run();
            } catch (CloseActionException e) {
                throw e.ioException;
            }
        }
    }

    private static final class TestSSLSocket extends SSLSocket {
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

    private static final class CloseActionException extends RuntimeException {
        private final IOException ioException;

        private CloseActionException(IOException ioException) {
            this.ioException = ioException;
        }
    }
}
