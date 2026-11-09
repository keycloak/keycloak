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

package org.keycloak.testframework.conformance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.logging.Logger;

/**
 * An HTTP proxy in the test JVM through which Keycloak reaches the suite's container internal host, e.g. to fetch the
 * jwks_uri, request_uri or sector_identifier_uri a dynamically registered client points at. The suite publishes these
 * URLs on {@link OpenIdConformanceServer#INTERNAL_BASE_URI}, which only resolves on the containers' network, while
 * Keycloak runs on the Docker host. Keycloak's outgoing HTTP client sends the requests for that host through this proxy
 * ({@link #keycloakProxyMapping}), which tunnels them (HTTP CONNECT) to the suite's port published on the Docker host.
 * TLS stays end to end between Keycloak and the suite.
 */
final class SuiteTunnel implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(SuiteTunnel.class);

    private static final int MAX_REQUEST_HEADER_BYTES = 16 * 1024;

    private final String internalAuthority;
    private final String targetHost;
    private final int targetPort;
    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "conformance-suite-tunnel");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Tunnels CONNECT requests for the authority of {@code internalUri} to {@code targetHost}:{@code targetPort}.
     */
    SuiteTunnel(URI internalUri, String targetHost, int targetPort) {
        this.internalAuthority = internalUri.getHost() + ":" + internalUri.getPort();
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        } catch (IOException e) {
            throw new RuntimeException("Failed to start the conformance suite tunnel", e);
        }
        executor.execute(this::accept);
    }

    /**
     * The value of Keycloak's {@code proxy-mappings} option of its outgoing HTTP client sending the requests for the
     * suite's internal host through this proxy.
     */
    String keycloakProxyMapping() {
        return internalAuthority.substring(0, internalAuthority.indexOf(':')).replace(".", "\\.") + ";http://"
                + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
    }

    private void accept() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                executor.execute(() -> tunnel(client));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOGGER.warn("Conformance suite tunnel failed to accept a connection: " + e.getMessage());
                }
            }
        }
    }

    private void tunnel(Socket client) {
        try (client) {
            String requestLine = readRequestHeader(client.getInputStream());
            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !"CONNECT".equals(parts[0]) || !internalAuthority.equalsIgnoreCase(parts[1])) {
                LOGGER.warn("Conformance suite tunnel rejected request: " + requestLine);
                client.getOutputStream().write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                return;
            }
            try (Socket target = new Socket(targetHost, targetPort)) {
                client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().flush();
                Future<?> response = executor.submit(() -> pipe(target, client));
                pipe(client, target);
                // Both sockets are closed only once the response is forwarded as well, as closing them while the
                // suite is still answering would cut off its response
                response.get();
            }
        } catch (IOException | ExecutionException e) {
            LOGGER.debug("Conformance suite tunnel connection closed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Reads the request line and headers of the CONNECT request, which ends with an empty line
    private static String readRequestHeader(InputStream input) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < 4) {
            int b = input.read();
            if (b < 0 || header.size() >= MAX_REQUEST_HEADER_BYTES) {
                throw new IOException("Incomplete proxy request");
            }
            header.write(b);
            matched = (b == (matched % 2 == 0 ? '\r' : '\n')) ? matched + 1 : (b == '\r' ? 1 : 0);
        }
        String text = header.toString(StandardCharsets.US_ASCII);
        return text.substring(0, text.indexOf("\r\n"));
    }

    // Copies one direction of the tunnel until its source closes, then closes the output of the other side, which the
    // other direction still uses for the answer. On an error both sockets are closed, as the other direction would
    // otherwise wait for data that never comes.
    private static void pipe(Socket from, Socket to) {
        try {
            InputStream input = from.getInputStream();
            OutputStream output = to.getOutputStream();
            byte[] buffer = new byte[8192];
            for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
                output.write(buffer, 0, read);
                output.flush();
            }
            to.shutdownOutput();
        } catch (IOException e) {
            LOGGER.debug("Conformance suite tunnel forwarding failed: " + e.getMessage());
            closeQuietly(from);
            closeQuietly(to);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // closing anyway
        }
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            // closing anyway
        }
        executor.shutdownNow();
    }
}
