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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuiteTunnelTest {

    private static final URI INTERNAL_URI = URI.create("https://nginx:8443");
    private static final int TIMEOUT_MILLIS = 5000;

    private ServerSocket target;
    private SuiteTunnel tunnel;

    @BeforeEach
    void startTarget() throws IOException {
        target = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
    }

    @AfterEach
    void stop() throws IOException {
        if (tunnel != null) {
            tunnel.close();
        }
        target.close();
    }

    @Test
    void forwardsResponseSentAfterClientClosedItsOutput() throws Exception {
        // the target answers only once it has read the whole request, i.e. once the client closed its output
        CompletableFuture<String> received = CompletableFuture.supplyAsync(() -> {
            try (Socket socket = target.accept()) {
                String request = readAll(socket.getInputStream());
                socket.getOutputStream().write(("pong:" + request).getBytes(StandardCharsets.US_ASCII));
                return request;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        tunnel = tunnelToTarget();

        try (Socket client = connect()) {
            assertEquals("HTTP/1.1 200 Connection Established", sendConnect(client, "nginx:8443"));
            client.getOutputStream().write("ping".getBytes(StandardCharsets.US_ASCII));
            client.shutdownOutput();
            assertEquals("pong:ping", readAll(client.getInputStream()));
        }
        assertEquals("ping", received.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    void rejectsOtherTargets() throws Exception {
        tunnel = tunnelToTarget();

        try (Socket client = connect()) {
            assertEquals("HTTP/1.1 403 Forbidden", sendConnect(client, "example.test:443"));
        }
    }

    @Test
    void closesBothSocketsWhenForwardingFails() {
        // reading from the source fails, while the other side, e.g. the target, would otherwise stay open and wait
        FailingSocket from = new FailingSocket();
        FailingSocket to = new FailingSocket();

        SuiteTunnel.pipe(from, to);

        assertTrue(from.isClosed());
        assertTrue(to.isClosed());
    }

    @Test
    void closesClientWhenTargetResetsTheConnection() throws Exception {
        CompletableFuture.runAsync(() -> {
            try (Socket socket = target.accept()) {
                socket.setSoLinger(true, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        tunnel = tunnelToTarget();

        try (Socket client = connect()) {
            assertEquals("HTTP/1.1 200 Connection Established", sendConnect(client, "nginx:8443"));
            // the client neither closes its output nor sends anything, so only closing it ends its read
            assertClosedByTunnel(client);
        }
    }

    @Test
    void closeClosesConnectionsWaitingForTheirRequest() throws Exception {
        tunnel = tunnelToTarget();

        try (Socket client = connect()) {
            client.getOutputStream().write("CONNECT nginx:8443 HTTP/1.1\r\nHost: ngi".getBytes(StandardCharsets.US_ASCII));
            client.getOutputStream().flush();
            Thread.sleep(200);
            tunnel.close();
            assertClosedByTunnel(client);
        }
    }

    @Test
    void closeClosesTunnelledConnections() throws Exception {
        CompletableFuture<Integer> targetRead = CompletableFuture.supplyAsync(() -> {
            try (Socket socket = target.accept()) {
                socket.setSoTimeout(TIMEOUT_MILLIS);
                return socket.getInputStream().read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        tunnel = tunnelToTarget();

        try (Socket client = connect()) {
            assertEquals("HTTP/1.1 200 Connection Established", sendConnect(client, "nginx:8443"));
            Thread.sleep(200);
            tunnel.close();
            assertClosedByTunnel(client);
        }
        // the target sees the end of the tunnelled connection too
        assertEquals(-1, targetRead.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    void proxyMappingBracketsIpv6Addresses() throws Exception {
        assertEquals("nginx;http://127.0.0.1:1234",
                SuiteTunnel.proxyMapping("nginx", InetAddress.getByName("127.0.0.1"), 1234));
        assertEquals("nginx;http://[0:0:0:0:0:0:0:1]:1234",
                SuiteTunnel.proxyMapping("nginx", InetAddress.getByName("::1"), 1234));
        assertEquals("suite\\.example\\.test;http://127.0.0.1:1234",
                SuiteTunnel.proxyMapping("suite.example.test", InetAddress.getByName("127.0.0.1"), 1234));
    }

    private SuiteTunnel tunnelToTarget() {
        return new SuiteTunnel(INTERNAL_URI, target.getInetAddress().getHostAddress(), target.getLocalPort());
    }

    // The tunnel listens on the loopback address its proxy mapping names
    private Socket connect() throws IOException {
        String mapping = tunnel.keycloakProxyMapping();
        URI proxy = URI.create(mapping.substring(mapping.indexOf(';') + 1));
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), proxy.getPort()), TIMEOUT_MILLIS);
        socket.setSoTimeout(TIMEOUT_MILLIS);
        return socket;
    }

    // Sends a CONNECT request and returns the status line of the answer
    private static String sendConnect(Socket client, String authority) throws IOException {
        client.getOutputStream().write(("CONNECT " + authority + " HTTP/1.1\r\nHost: " + authority + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
        client.getOutputStream().flush();
        InputStream input = client.getInputStream();
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        while (!header.toString(StandardCharsets.US_ASCII).endsWith("\r\n\r\n")) {
            int b = input.read();
            if (b < 0) {
                break;
            }
            header.write(b);
        }
        String text = header.toString(StandardCharsets.US_ASCII);
        return text.substring(0, Math.max(0, text.indexOf("\r\n")));
    }

    private static String readAll(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.US_ASCII);
    }

    // The read ends, with the end of the stream or an error, well before the socket timeout
    private static void assertClosedByTunnel(Socket client) throws IOException {
        long start = System.nanoTime();
        int read;
        try {
            read = client.getInputStream().read();
        } catch (java.net.SocketTimeoutException e) {
            throw new AssertionError("The tunnel did not close the connection", e);
        } catch (IOException e) {
            read = -1;
        }
        assertEquals(-1, read);
        assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS));
    }

    // An unconnected socket whose input fails like a connection reset
    private static final class FailingSocket extends Socket {

        @Override
        public InputStream getInputStream() {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Connection reset");
                }
            };
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }
    }
}
