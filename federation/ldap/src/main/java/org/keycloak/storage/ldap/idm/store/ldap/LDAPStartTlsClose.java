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
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jboss.logging.Logger;

/**
 * Tracks the TLS socket negotiated by JNDI and bounds the TLS close-notify read.
 */
final class LDAPStartTlsClose {

    private static final Logger logger = Logger.getLogger(LDAPStartTlsClose.class);
    private static final int CLOSE_TIMEOUT_MILLIS = 3_000;

    private LDAPStartTlsClose() {
    }

    static SSLSocketFactory trackingFactory(SSLSocketFactory delegate, AtomicReference<SSLSocket> socket, AtomicReference<Socket> transport) {
        return new TrackingSSLSocketFactory(delegate, socket, transport);
    }

    static void close(StartTlsResponse tlsResponse, SSLSocket socket, Socket transport) {
        if (tlsResponse == null) {
            return;
        }

        // Without the socket there is no way to bound StartTlsResponse.close().
        // The owning LdapContext is closed immediately after this method.
        if (socket == null) {
            logger.warn("Could not close Ldap tlsResponse gracefully because the TLS socket was not captured; closing the LDAP context instead.");
            if (transport != null) {
                closeSocket(null, transport);
            }
            return;
        }

        int originalTimeout;
        try {
            originalTimeout = socket.getSoTimeout();
            socket.setSoTimeout(CLOSE_TIMEOUT_MILLIS);
        } catch (IOException e) {
            logger.warn("Could not set a bounded timeout for closing the LDAP TLS response; closing the TLS socket.", e);
            closeSocket(socket, transport);
            return;
        }

        boolean closed = false;
        try {
            tlsResponse.close();
            closed = true;
        } catch (IOException e) {
            logger.warn("Could not close Ldap tlsResponse within the configured socket timeout; closing the TLS socket.", e);
            closeSocket(socket, transport);
        } finally {
            if (closed && !socket.isClosed()) {
                try {
                    socket.setSoTimeout(originalTimeout);
                } catch (IOException e) {
                    logger.debug("Could not restore the LDAP TLS socket timeout after closing the TLS response.", e);
                }
            }
        }
    }

    private static void closeSocket(SSLSocket socket, Socket transport) {
        try {
            if (transport != null) {
                transport.close();
            } else if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.debug("Could not close LDAP TLS socket.", e);
        }
    }

    private static final class TrackingSSLSocketFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;
        private final AtomicReference<SSLSocket> socket;
        private final AtomicReference<Socket> transport;

        private TrackingSSLSocketFactory(SSLSocketFactory delegate, AtomicReference<SSLSocket> socket, AtomicReference<Socket> transport) {
            this.delegate = delegate;
            this.socket = socket;
            this.transport = transport;
        }

        private Socket track(Socket created, Socket underlyingTransport) throws IOException {
            if (!(created instanceof SSLSocket)) {
                created.close();
                throw new IOException("LDAP StartTLS socket factory did not create an SSLSocket");
            }
            socket.set((SSLSocket) created);
            transport.set(underlyingTransport == null ? created : underlyingTransport);
            return created;
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
            return track(delegate.createSocket(s, host, port, autoClose), s);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return track(delegate.createSocket(host, port), null);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return track(delegate.createSocket(host, port, localHost, localPort), null);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return track(delegate.createSocket(host, port), null);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return track(delegate.createSocket(address, port, localAddress, localPort), null);
        }
    }
}
