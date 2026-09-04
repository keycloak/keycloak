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
package org.keycloak.http.simple;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import static org.keycloak.http.simple.SimpleHttp.OnCompletion.CLOSE_CLIENT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Proves the lifecycle contract of {@link SimpleHttp#create(HttpClient, SimpleHttp.OnCompletion)}: an owned per-call client is closed
 * exactly once after the response body has been consumed, on every terminal read path and on the exception path;
 * a shared (non-owned) client is never closed.
 */
public final class SimpleHttpOwnedClientTest {

    private static final String BODY = "{\"value\":\"ok\"}";

    @Test
    public void ownedClientClosedAfterAsResponseAndConsume() throws IOException {
        CountingHttpClient client = new CountingHttpClient();
        try (SimpleHttpResponse response = SimpleHttp.create(client, CLOSE_CLIENT).doGet("http://localhost/userinfo").asResponse()) {
            // Body not yet consumed via terminal read: reading it triggers the close.
            assertEquals(BODY, response.asString());
        }
        assertEquals("owned client must be closed exactly once", 1, client.closeCount.get());
    }

    @Test
    public void ownedClientClosedAfterAsString() throws IOException {
        CountingHttpClient client = new CountingHttpClient();
        String body = SimpleHttp.create(client, CLOSE_CLIENT).doGet("http://localhost/userinfo").asString();
        assertEquals(BODY, body);
        assertEquals(1, client.closeCount.get());
    }

    @Test
    public void ownedClientClosedAfterAsStatus() throws IOException {
        CountingHttpClient client = new CountingHttpClient();
        int status = SimpleHttp.create(client, CLOSE_CLIENT).doGet("http://localhost/logout").asStatus();
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals(1, client.closeCount.get());
    }

    @Test
    public void ownedClientClosedWhenResponseNeverExplicitlyClosed() throws IOException {
        // Mirrors call sites that hold the SimpleHttpResponse open and only read from it (no try-with-resources).
        CountingHttpClient client = new CountingHttpClient();
        SimpleHttpResponse response = SimpleHttp.create(client, CLOSE_CLIENT).doGet("http://localhost/userinfo").asResponse();
        assertEquals(HttpStatus.SC_OK, response.getStatus()); // getStatus() triggers readResponse() -> close
        assertEquals(1, client.closeCount.get());
    }

    @Test
    public void ownedClientClosedExactlyOnceOnRepeatedReads() throws IOException {
        CountingHttpClient client = new CountingHttpClient();
        SimpleHttpResponse response = SimpleHttp.create(client, CLOSE_CLIENT).doGet("http://localhost/userinfo").asResponse();
        response.getStatus();
        response.asString();
        response.close();
        assertEquals("readResponse() is idempotent; the owned client must not be double-closed", 1, client.closeCount.get());
    }

    @Test
    public void ownedClientClosedWhenExecuteThrows() {
        CountingHttpClient client = new CountingHttpClient();
        client.failOnExecute = true;
        try {
            SimpleHttp.create(client, CLOSE_CLIENT).doGet("http://localhost/userinfo").asResponse();
            fail("expected IOException");
        } catch (IOException expected) {
            // expected
        }
        assertEquals("owned client must be closed even when the request never yields a response", 1, client.closeCount.get());
    }

    @Test
    public void ownedClientClosedWhenRequestBuildThrowsBeforeExecute() {
        // A POST with neither params nor entity fails with "No content set" while the request is being built,
        // before client.execute() is ever reached. The owned per-call client must still be closed.
        CountingHttpClient client = new CountingHttpClient();
        try {
            SimpleHttp.create(client, CLOSE_CLIENT).doPost("http://localhost/token").asResponse();
            fail("expected IllegalStateException for a POST with no content");
        } catch (IllegalStateException expected) {
            // expected: "No content set"
        } catch (IOException e) {
            fail("expected IllegalStateException, not IOException");
        }
        assertEquals("owned client must be closed even when request construction fails before execute",
                1, client.closeCount.get());
    }

    @Test
    public void sharedClientNeverClosed() throws IOException {
        // closeClientOnCompletion == false: shared/pooled client must survive the request.
        CountingHttpClient client = new CountingHttpClient();
        String body = SimpleHttp.create(client).doGet("http://localhost/userinfo").asString();
        assertEquals(BODY, body);
        assertEquals("a shared client must never be closed by SimpleHttp", 0, client.closeCount.get());
    }

    /**
     * Hand-written stub (no mocking framework, matching the module convention) that also implements
     * {@link Closeable} and counts close() calls.
     */
    private static final class CountingHttpClient implements HttpClient, Closeable {

        final AtomicInteger closeCount = new AtomicInteger();
        boolean failOnExecute = false;

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }

        @Override
        public HttpResponse execute(HttpUriRequest request) throws IOException {
            if (failOnExecute) {
                throw new IOException("boom");
            }
            BasicHttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, "OK");
            httpResponse.setEntity(new StringEntity(BODY, StandardCharsets.UTF_8));
            return httpResponse;
        }

        @Override
        public HttpParams getParams() {
            fail();
            return null;
        }

        @Override
        public ClientConnectionManager getConnectionManager() {
            fail();
            return null;
        }

        @Override
        public HttpResponse execute(HttpUriRequest request, HttpContext context) {
            fail();
            return null;
        }

        @Override
        public HttpResponse execute(HttpHost host, HttpRequest request) {
            fail();
            return null;
        }

        @Override
        public HttpResponse execute(HttpHost host, HttpRequest request, HttpContext context) {
            fail();
            return null;
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> handler) {
            fail();
            return null;
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> handler, HttpContext context) {
            fail();
            return null;
        }

        @Override
        public <T> T execute(HttpHost host, HttpRequest request, ResponseHandler<? extends T> handler) {
            fail();
            return null;
        }

        @Override
        public <T> T execute(HttpHost host, HttpRequest request, ResponseHandler<? extends T> handler, HttpContext context) {
            fail();
            return null;
        }
    }
}
