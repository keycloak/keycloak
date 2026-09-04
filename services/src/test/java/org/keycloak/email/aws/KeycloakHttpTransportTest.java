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

package org.keycloak.email.aws;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Drives the real Apache HttpClient Keycloak hands out against a real local HTTP server, because
 * this class exists for one reason only: to guarantee what leaves the socket. A fake client would
 * assert the code we wrote, not the bytes Apache actually puts on the wire, and every failure this
 * adapter can cause — a re-encoded body, a rewritten header, a followed redirect — is invisible
 * until it reaches AWS, where it comes back as an unexplainable {@code SignatureDoesNotMatch}.
 */
class KeycloakHttpTransportTest {

    private static final String SES_PATH = "/v2/email/outbound-emails";
    private static final String METADATA_TOKEN_PATH = "/latest/api/token";
    private static final String SIGNED_HOST = "email.eu-central-1.amazonaws.com";

    private static final int CONNECT_TIMEOUT_MILLIS = 2000;
    private static final int READ_TIMEOUT_MILLIS = 2000;

    /** Mirrors the transport's own {@code MAX_RESPONSE_BYTES}, which is private and has to stay so. */
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    private HttpServer server;
    private ExecutorService serverExecutor;
    private CloseableHttpClient httpClient;
    private KeycloakHttpTransport transport;

    private final List<RecordedRequest> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger redirectTargetHits = new AtomicInteger();

    /** Released in teardown so a deliberately slow handler can never outlive its test. */
    private final CountDownLatch slowHandlerRelease = new CountDownLatch(1);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.start();
        httpClient = HttpClients.createDefault();
        transport = new KeycloakHttpTransport(httpClient);
    }

    @AfterEach
    void stopServer() throws IOException {
        slowHandlerRelease.countDown();
        httpClient.close();
        server.stop(0);
        serverExecutor.shutdownNow();
    }

    /**
     * A UTF-8 body must arrive as UTF-8. The obvious {@code new StringEntity(json, ...)}
     * implementation re-encodes to ISO-8859-1 by default, which changes the bytes AWS hashes: every
     * accented subject line would then fail signature verification while an ASCII one worked.
     */
    @Test
    void putsTheExactBodyBytesOnTheWireIncludingNonAsciiCharacters() throws Exception {
        String json = "{\"Subject\":\"Attivazione dell'account \u00e8 gi\u00e0 pronta \u2014 conferma\"}";
        byte[] utf8 = json.getBytes(StandardCharsets.UTF_8);
        stub(SES_PATH, 200, Map.of(), "{\"MessageId\":\"0100018f\"}");

        transport.exchange(post(uri(SES_PATH), Map.of("Content-Type", "application/json"), utf8));

        RecordedRequest request = onlyRequest();
        assertThat(request.method(), is("POST"));
        assertThat(request.path(), is(SES_PATH));
        assertThat(request.body(), is(equalTo(utf8)));
        assertThat(request.body(), is(not(equalTo(json.getBytes(StandardCharsets.ISO_8859_1)))));
    }

    /**
     * SigV4 signs the header values verbatim, so anything the transport rewrites, drops or adds a
     * second copy of invalidates the signature. {@code Host} is the sharp case: it is signed with
     * the SES endpoint's name while the socket goes somewhere else entirely (here 127.0.0.1, in
     * production a proxy), and a client that overwrote it with the connection's authority would
     * make every send fail with a 403 no log could explain.
     */
    @Test
    void sendsEveryHeaderTheCallerSetWithItsExactValue() throws Exception {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", SIGNED_HOST);
        headers.put("Content-Type", "application/json");
        headers.put("X-Amz-Date", "20260903T101530Z");
        headers.put("X-Amz-Security-Token", "FwoGZXIvYXdzEExampleSessionToken==");
        headers.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20260903/eu-central-1/ses/aws4_request, "
                + "SignedHeaders=content-type;host;x-amz-date;x-amz-security-token, "
                + "Signature=c747cbbba89b046b0741cea3b4886af64a0ad60e548e920ea0c9e5d730d547a9");
        stub(SES_PATH, 200, Map.of(), "{}");

        transport.exchange(post(uri(SES_PATH), headers, "{}".getBytes(StandardCharsets.UTF_8)));

        RecordedRequest request = onlyRequest();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            assertThat("header " + header.getKey(), request.headerValues(header.getKey()), contains(header.getValue()));
        }
    }

    /**
     * The caller signs one {@code content-type}; a second one added by the entity would leave AWS
     * canonicalising a value the signature never covered.
     */
    @Test
    void leavesTheCallersContentTypeAsTheOnlyOne() throws Exception {
        stub(SES_PATH, 200, Map.of(), "{}");

        transport.exchange(post(uri(SES_PATH), Map.of("Content-Type", "application/json"),
                "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(onlyRequest().headerValues("Content-Type"), contains("application/json"));
    }

    /** An unsigned header the transport invents is a header AWS sees and the signature does not. */
    @Test
    void sendsNoContentTypeWhenTheCallerSetNone() throws Exception {
        stub(SES_PATH, 200, Map.of(), "{}");

        transport.exchange(post(uri(SES_PATH), Map.of("Host", SIGNED_HOST),
                "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(onlyRequest().headerValues("Content-Type"), is(empty()));
    }

    /** The shape of every instance-metadata read: a GET carrying the IMDSv2 token and no body. */
    @Test
    void sendsAGetWithNoBody() throws Exception {
        stub("/latest/meta-data/iam/security-credentials/", 200, Map.of(), "keycloak-ses-role");

        AwsHttpResponse response = transport.exchange(AwsHttpRequest.get(
                uri("/latest/meta-data/iam/security-credentials/"),
                Map.of("X-aws-ec2-metadata-token", "imds-session-token"),
                CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS));

        RecordedRequest request = onlyRequest();
        assertThat(request.method(), is("GET"));
        assertThat(request.body(), is(equalTo(new byte[0])));
        assertThat(request.headerValues("X-aws-ec2-metadata-token"), contains("imds-session-token"));
        assertThat(response.bodyAsString(), is("keycloak-ses-role"));
    }

    /**
     * The IMDSv2 token call is a PUT with an empty body — the one place the provider sends a body
     * method without a body. An entity-less PUT that Apache refused to send would silently
     * downgrade every EC2 deployment to "no credentials found".
     */
    @Test
    void sendsAPutWithNoBody() throws Exception {
        stub(METADATA_TOKEN_PATH, 200, Map.of(), "AQAEAExampleTokenValue==");

        AwsHttpResponse response = transport.exchange(new AwsHttpRequest("PUT", uri(METADATA_TOKEN_PATH),
                Map.of("X-aws-ec2-metadata-token-ttl-seconds", "21600"), AwsHttpRequest.NO_BODY,
                CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS));

        RecordedRequest request = onlyRequest();
        assertThat(request.method(), is("PUT"));
        assertThat(request.body(), is(equalTo(new byte[0])));
        assertThat(request.headerValues("X-aws-ec2-metadata-token-ttl-seconds"), contains("21600"));
        assertThat(response.status(), is(200));
        assertThat(response.bodyAsString(), is("AQAEAExampleTokenValue=="));
    }

    @Test
    void returnsTheStatusHeadersAndBodyOfTheResponse() throws Exception {
        String body = "{\"MessageId\":\"0100018f-\u00e8\"}";
        stub(SES_PATH, 200, Map.of("Content-Type", "application/json"), body);

        AwsHttpResponse response = transport.exchange(post(uri(SES_PATH), Map.of(), "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(response.status(), is(200));
        assertThat(response.isSuccessful(), is(true));
        assertThat(response.body(), is(equalTo(body.getBytes(StandardCharsets.UTF_8))));
        assertThat(response.header("Content-Type"), is("application/json"));
    }

    /**
     * The only handle on a failed send is the request id AWS returns, and it arrives in whatever
     * casing the responding server chose. Looking the header up by the documented spelling must
     * still find it, or every SES failure is reported without the id support asks for.
     */
    @Test
    void findsAResponseHeaderWhateverCasingTheServerUsed() throws Exception {
        stub(SES_PATH, 400, Map.of("x-amzn-requestid", "4c9f5d1e-0d2a-4d3f-9d2a-4d3f9d2a4d3f"), "{}");

        AwsHttpResponse response = transport.exchange(post(uri(SES_PATH), Map.of(), "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(response.header("X-Amzn-RequestId"), is("4c9f5d1e-0d2a-4d3f-9d2a-4d3f9d2a4d3f"));
        // No HTTP stack produces this spelling on its own, so the lookup above cannot have matched
        // the map key literally: it went through the case-insensitive path.
        assertThat(response.headers(), not(hasKey("X-Amzn-RequestId")));
    }

    /**
     * A followed redirect replays a signed request against a host the signature does not cover: at
     * best a 403, at worst the credential-endpoint token handed to whatever host answered. The
     * verb here is a GET on purpose — Apache's default strategy declines to redirect a POST, so
     * only a GET proves redirects are off rather than merely unreachable for the SES call.
     */
    @Test
    void doesNotFollowARedirect() throws Exception {
        stub(METADATA_TOKEN_PATH, 302, Map.of("Location", uri("/elsewhere").toString()), "");
        server.createContext("/elsewhere", exchange -> {
            redirectTargetHits.incrementAndGet();
            respond(exchange, 200, Map.of(), "should never be reached".getBytes(StandardCharsets.UTF_8));
        });

        AwsHttpResponse response = transport.exchange(AwsHttpRequest.get(uri(METADATA_TOKEN_PATH), Map.of(),
                CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS));

        assertThat(response.status(), is(302));
        assertThat(response.header("Location"), is(uri("/elsewhere").toString()));
        assertThat(redirectTargetHits.get(), is(0));
    }

    /**
     * SES puts the reason for a rejection in the body of a 4xx. A transport that threw on a non-2xx
     * would leave the caller with "HTTP 400" and no way to tell a throttle from an unverified
     * sender address.
     */
    @Test
    void returnsTheBodyOfAnErrorResponseInsteadOfThrowing() throws Exception {
        String error = "{\"__type\":\"MessageRejected\",\"message\":\"Email address is not verified.\"}";
        stub(SES_PATH, 400, Map.of("x-amzn-ErrorType", "MessageRejected:1a2b3c"), error);

        AwsHttpResponse response = transport.exchange(post(uri(SES_PATH), Map.of(), "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(response.status(), is(400));
        assertThat(response.isSuccessful(), is(false));
        assertThat(response.bodyAsString(), is(error));
        assertThat(response.header("x-amzn-errortype"), is("MessageRejected:1a2b3c"));
    }

    /**
     * A silent SES endpoint must not pin the Keycloak thread that is sending the email: without the
     * per-request socket timeout the shared client's default of "wait forever" applies, and a
     * blackholed route holds the transaction open until the OS gives up.
     */
    @Test
    void failsWhenTheServerStaysSilentPastTheReadTimeout() {
        server.createContext("/slow", exchange -> {
            awaitTeardown();
            respondQuietly(exchange);
        });
        AwsHttpRequest request = new AwsHttpRequest("POST", uri("/slow"), Map.of(),
                "{}".getBytes(StandardCharsets.UTF_8), 300, 300);

        assertThrows(SocketTimeoutException.class, () -> transport.exchange(request));
    }

    /**
     * The signature is computed over the method, so sending a request as anything other than what
     * the caller named would fail at AWS with no local trace. Failing here names the mistake.
     */
    @Test
    void rejectsAMethodItCannotSendRatherThanSendingSomethingElse() {
        stub(SES_PATH, 200, Map.of(), "{}");
        AwsHttpRequest request = new AwsHttpRequest("DELETE", uri(SES_PATH), Map.of(),
                AwsHttpRequest.NO_BODY, CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> transport.exchange(request));

        assertThat(failure.getMessage(), containsString("DELETE"));
        assertThat(received, is(empty()));
    }

    /**
     * Apache's per-request {@link RequestConfig} <em>replaces</em> the client's rather than merging
     * with it, so the three timeouts have to be set in the same object: a change that set only the
     * socket timeout would silently return connect and connection-request to "infinite". Neither of
     * those two can be observed from a passing exchange — a connect timeout needs a blackholed
     * route, a connection-request timeout an exhausted pool — so the configuration Apache resolved
     * for the request is read back instead, out of the client's own execution context.
     */
    @Test
    void boundsTheConnectAndConnectionRequestTimeoutsAlongsideTheReadTimeout() throws Exception {
        stub(SES_PATH, 200, Map.of(), "{}");
        AtomicReference<RequestConfig> applied = new AtomicReference<>();
        HttpRequestInterceptor captureAppliedConfig =
                (request, context) -> applied.set(HttpClientContext.adapt(context).getRequestConfig());

        try (CloseableHttpClient client = HttpClients.custom().addInterceptorFirst(captureAppliedConfig).build()) {
            new KeycloakHttpTransport(client).exchange(new AwsHttpRequest("POST", uri(SES_PATH), Map.of(),
                    "{}".getBytes(StandardCharsets.UTF_8), 1500, 2500));
        }

        RequestConfig config = applied.get();
        assertThat(config.getConnectTimeout(), is(1500));
        assertThat(config.getConnectionRequestTimeout(), is(1500));
        assertThat(config.getSocketTimeout(), is(2500));
    }

    /**
     * The response is read on a request thread inside a Keycloak transaction, and the same transport
     * talks to the container credential endpoint and the instance metadata service — neither of them
     * an endpoint whose size is guaranteed by AWS. A peer that streams without end must therefore be
     * cut off: buffering it would pin the thread and exhaust the heap, and the heap is shared with
     * every other session on the server.
     */
    @Test
    void refusesAResponseBodyLargerThanTheBound() {
        stubOversizedBody(SES_PATH, 4 * MAX_RESPONSE_BYTES);

        IOException failure = assertThrows(IOException.class,
                () -> transport.exchange(post(uri(SES_PATH), Map.of(), "{}".getBytes(StandardCharsets.UTF_8))));

        assertThat(failure.getMessage(), containsString(String.valueOf(MAX_RESPONSE_BYTES)));
    }

    /**
     * The other side of the bound. Written as an exact fit because that is where an off-by-one lives:
     * a {@code >=} in place of the {@code >} would reject a legitimate answer, and the failure would
     * look like an SES outage rather than like a bug here.
     */
    @Test
    void returnsABodyThatExactlyFillsTheBound() throws Exception {
        stub(SES_PATH, 200, Map.of(), "x".repeat(MAX_RESPONSE_BYTES));

        AwsHttpResponse response = transport.exchange(post(uri(SES_PATH), Map.of(),
                "{}".getBytes(StandardCharsets.UTF_8)));

        assertThat(response.body().length, is(MAX_RESPONSE_BYTES));
    }

    private AwsHttpRequest post(URI uri, Map<String, String> headers, byte[] body) {
        return new AwsHttpRequest("POST", uri, headers, body, CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    /** Answers {@code path} with a canned response, recording the request exactly as it arrived. */
    private void stub(String path, int status, Map<String, String> responseHeaders, String responseBody) {
        server.createContext(path, exchange -> {
            received.add(RecordedRequest.of(exchange));
            respond(exchange, status, responseHeaders, responseBody.getBytes(StandardCharsets.UTF_8));
        });
    }

    /**
     * Answers {@code path} with a chunked body of {@code totalBytes}, which is more than the caller
     * is willing to read. The write fails as soon as the transport gives up and closes the socket,
     * which is the expected outcome rather than a problem.
     */
    private void stubOversizedBody(String path, int totalBytes) {
        server.createContext(path, exchange -> {
            byte[] chunk = new byte[8192];
            try (exchange) {
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream out = exchange.getResponseBody()) {
                    for (int written = 0; written < totalBytes; written += chunk.length) {
                        out.write(chunk);
                    }
                }
            } catch (IOException expected) {
                // The transport hung up mid-body. That is the assertion under test, client-side.
            }
        });
    }

    private RecordedRequest onlyRequest() {
        assertThat("requests the server received", received.size(), is(1));
        return received.get(0);
    }

    private static void respond(HttpExchange exchange, int status, Map<String, String> headers, byte[] body)
            throws IOException {
        headers.forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        try (exchange) {
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        }
    }

    private void awaitTeardown() {
        try {
            slowHandlerRelease.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** The client has long since timed out and closed the socket; writing back is best-effort. */
    private static void respondQuietly(HttpExchange exchange) {
        try {
            respond(exchange, 200, Map.of(), "too late".getBytes(StandardCharsets.UTF_8));
        } catch (IOException expected) {
            // The connection is gone. Nothing to report: the assertion under test is client-side.
        }
    }

    private record RecordedRequest(String method, String path, Map<String, List<String>> headers, byte[] body) {

        static RecordedRequest of(HttpExchange exchange) throws IOException {
            byte[] body = exchange.getRequestBody().readAllBytes();
            return new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    new LinkedHashMap<>(exchange.getRequestHeaders()), body);
        }

        /** Every value sent under {@code name}, so a duplicated header is visible, not merged away. */
        List<String> headerValues(String name) {
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                if (header.getKey().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
            return List.of();
        }
    }
}
