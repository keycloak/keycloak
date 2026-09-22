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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.logging.Logger;

/**
 * A {@link StartTlsResponse} that puts an upper bound on how long {@link #close()} may block.
 * <p>
 * Closing a TLS socket makes the JDK send {@code close_notify} and then, for anything below TLS 1.3, block on a read
 * until the peer answers with its own {@code close_notify}. That read is covered by neither
 * {@code com.sun.jndi.ldap.connect.timeout} nor {@code com.sun.jndi.ldap.read.timeout}, only by the socket read
 * timeout, which JNDI leaves at infinity. A server that never answers therefore pins the calling thread forever. As
 * StartTLS also disables connection pooling, every single LDAP operation closes a connection and can get stuck that
 * way, so a handful of such closes is enough to exhaust the worker pool.
 * <p>
 * The socket created during the negotiation is captured and given a read timeout just before the close, so the wait
 * for the peer's {@code close_notify} ends in a {@link SocketTimeoutException} rather than hanging.
 * <p>
 * The timeout is deliberately not set right after the negotiation and left in place: JNDI reads from the connection
 * on a dedicated thread and treats every {@link java.io.IOException} as a broken connection, so a socket read timeout
 * would drop any connection idle for longer than it, for example between the pages of a paginated search. Setting it
 * only at close time bounds the shutdown without ever applying to a connection still in use.
 */
class StartTlsResponseWithCloseTimeout extends StartTlsResponse {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(StartTlsResponseWithCloseTimeout.class);

    private final StartTlsResponse delegate;
    private final int closeTimeoutMillis;
    private transient SocketCapturingSSLSocketFactory socketFactory;

    StartTlsResponseWithCloseTimeout(StartTlsResponse delegate, int closeTimeoutMillis) {
        this.delegate = delegate;
        this.closeTimeoutMillis = closeTimeoutMillis;
    }

    @Override
    public SSLSession negotiate() throws IOException {
        return negotiate(null);
    }

    @Override
    public SSLSession negotiate(SSLSocketFactory factory) throws IOException {
        socketFactory = new SocketCapturingSSLSocketFactory(factory);
        return delegate.negotiate(socketFactory);
    }

    @Override
    public void close() throws IOException {
        SSLSocket socket = socketFactory != null ? socketFactory.socket : null;

        if (socket != null && closeTimeoutMillis > 0) {
            try {
                socket.setSoTimeout(closeTimeoutMillis);
            } catch (SocketException e) {
                logger.debug("Could not set a close timeout on the StartTLS socket.", e);
            }
        }

        try {
            delegate.close();
        } catch (SocketTimeoutException e) {
            logger.debugf("The LDAP server did not acknowledge the TLS shutdown within %d ms, dropping the connection "
                    + "without a graceful TLS close.", closeTimeoutMillis);
        }
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        delegate.setEnabledCipherSuites(suites);
    }

    @Override
    public void setHostnameVerifier(HostnameVerifier verifier) {
        delegate.setHostnameVerifier(verifier);
    }

    @Override
    public String getID() {
        return delegate.getID();
    }

    @Override
    public byte[] getEncodedValue() {
        return delegate.getEncodedValue();
    }

    /**
     * Hands out the sockets of the delegate factory while keeping a reference to the last one created, so that the
     * TLS socket set up by the negotiation can be configured afterwards.
     */
    private static final class SocketCapturingSSLSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;
        private volatile SSLSocket socket;

        private SocketCapturingSSLSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate != null ? delegate : (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return capture(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return capture(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return capture(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return capture(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return capture(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket capture(Socket created) {
            if (created instanceof SSLSocket) {
                socket = (SSLSocket) created;
            }
            return created;
        }
    }
}