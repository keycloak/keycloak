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
package org.keycloak.ssf.transmitter.delivery.push;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.ssf.transmitter.delivery.push.PrivateAddressDenyingDnsResolver.PrivateAddressException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PrivateAddressDenyingDnsResolver}, the delivery-time
 * half of the push SSRF gate that defeats DNS rebinding.
 */
class PrivateAddressDenyingDnsResolverTest {

    @Test
    void publicAnswerIsReturned() throws Exception {
        InetAddress[] answer = {address("93.184.216.34"), address("2606:2800:220:1:248:1893:25c8:1946")};
        assertArrayEquals(answer, resolverAnswering(answer).resolve("receiver.example.com"));
    }

    @Test
    void privateAnswerIsRejected() throws Exception {
        for (String ip : new String[]{"127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254",
                "0.0.0.0", "0.1.2.3", "100.64.0.1", "224.0.0.1", "::1", "fe80::1", "fd00::1"}) {
            PrivateAddressDenyingDnsResolver resolver = resolverAnswering(address(ip));
            assertThrows(PrivateAddressException.class, () -> resolver.resolve("receiver.example.com"), ip);
        }
    }

    @Test
    void mixedAnswerIsRejected() throws Exception {
        PrivateAddressDenyingDnsResolver resolver = resolverAnswering(address("93.184.216.34"), address("10.0.0.1"));
        assertThrows(PrivateAddressException.class, () -> resolver.resolve("receiver.example.com"));
    }

    @Test
    void emptyAnswerIsRejected() {
        PrivateAddressDenyingDnsResolver resolver = resolverAnswering();
        UnknownHostException ex = assertThrows(UnknownHostException.class, () -> resolver.resolve("receiver.example.com"));
        assertEquals(UnknownHostException.class, ex.getClass());
    }

    @Test
    void rebindingAfterConfigurationTimeIsRejectedOnEveryResolution() throws Exception {
        // First lookup (e.g. at stream configuration) answers public, later
        // lookups (at delivery) answer loopback.
        AtomicInteger lookups = new AtomicInteger();
        PrivateAddressDenyingDnsResolver resolver = new PrivateAddressDenyingDnsResolver() {
            @Override
            protected InetAddress[] doResolve(String host) throws UnknownHostException {
                return new InetAddress[]{lookups.getAndIncrement() == 0 ? address("93.184.216.34") : address("127.0.0.1")};
            }
        };
        resolver.resolve("receiver.example.com");
        assertThrows(PrivateAddressException.class, () -> resolver.resolve("receiver.example.com"));
    }

    @Test
    void httpClientDoesNotConnectToPrivateTarget() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             CloseableHttpClient client = HttpClients.custom()
                     .setDnsResolver(new PrivateAddressDenyingDnsResolver())
                     .build()) {
            server.setSoTimeout(200);
            HttpPost post = new HttpPost("http://127.0.0.1:" + server.getLocalPort() + "/push");
            assertThrows(PrivateAddressException.class, () -> client.execute(post));
            // nothing reached the listener
            assertThrows(SocketTimeoutException.class, server::accept);
        }
    }

    private static PrivateAddressDenyingDnsResolver resolverAnswering(InetAddress... answer) {
        return new PrivateAddressDenyingDnsResolver() {
            @Override
            protected InetAddress[] doResolve(String host) {
                return answer;
            }
        };
    }

    private static InetAddress address(String literal) throws UnknownHostException {
        return InetAddress.getByName(literal);
    }
}
