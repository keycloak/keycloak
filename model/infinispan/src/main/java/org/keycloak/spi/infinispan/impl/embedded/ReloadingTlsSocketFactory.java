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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import org.jgroups.util.DefaultSocketFactory;

/** Selects an immutable identity/TLS snapshot for each new connection, including accepted sockets. */
final class ReloadingTlsSocketFactory extends DefaultSocketFactory {
    record State(SSLContext context, SSLParameters parameters) {
        Socket configure(Socket socket, boolean client) {
            var tls = (SSLSocket) socket;
            tls.setUseClientMode(client);
            tls.setSSLParameters(parameters);
            return tls;
        }
    }

    ReloadingTlsSocketFactory(Supplier<State> states) {
        super(new Clients(states), new Servers(states));
        states.get(); // Validate policy and refresh the identity before opening any listener.
    }

    private static final class Clients extends javax.net.SocketFactory {
        private final Supplier<State> states;
        Clients(Supplier<State> states) { this.states = states; }
        public Socket createSocket() throws IOException {
            var state = states.get();
            return state.configure(state.context().getSocketFactory().createSocket(), true);
        }
        public Socket createSocket(String host, int port) throws IOException {
            var state = states.get();
            return state.configure(state.context().getSocketFactory().createSocket(host, port), true);
        }
        public Socket createSocket(InetAddress host, int port) throws IOException {
            var state = states.get();
            return state.configure(state.context().getSocketFactory().createSocket(host, port), true);
        }
        public Socket createSocket(String host, int port, InetAddress local, int localPort) throws IOException {
            var state = states.get();
            return state.configure(state.context().getSocketFactory().createSocket(host, port, local, localPort), true);
        }
        public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) throws IOException {
            var state = states.get();
            return state.configure(state.context().getSocketFactory().createSocket(host, port, local, localPort), true);
        }
    }

    private static final class Servers extends javax.net.ServerSocketFactory {
        private final Supplier<State> states;
        Servers(Supplier<State> states) { this.states = states; }
        public ServerSocket createServerSocket() throws IOException { return new Listener(states); }
        public ServerSocket createServerSocket(int port) throws IOException { return createServerSocket(port, 50, null); }
        public ServerSocket createServerSocket(int port, int backlog) throws IOException { return createServerSocket(port, backlog, null); }
        public ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException {
            var socket = new Listener(states);
            try {
                socket.bind(new java.net.InetSocketAddress(address, port), backlog);
                return socket;
            } catch (IOException | RuntimeException e) {
                socket.close();
                throw e;
            }
        }
    }

    private static final class Listener extends ServerSocket {
        private final Supplier<State> states;
        Listener(Supplier<State> states) throws IOException { this.states = states; }
        @Override
        public Socket accept() throws IOException {
            var plain = super.accept();
            try {
                var state = states.get();
                var socket = state.context().getSocketFactory().createSocket(plain,
                        plain.getInetAddress().getHostAddress(), plain.getPort(), true);
                return state.configure(socket, false);
            } catch (IOException | RuntimeException e) {
                plain.close();
                throw e;
            }
        }
    }
}
