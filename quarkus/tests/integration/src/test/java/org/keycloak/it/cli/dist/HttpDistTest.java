/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.cli.dist;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.TrustManager;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.resource.realm.TestRealmResourceTestProvider;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.restassured.RestAssured;
import io.restassured.config.RedirectConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@DistributionTest(stopServer = Mode.MANUAL, enableTls = true)
@RawDistOnly(reason = "Containers are immutable")
public class HttpDistTest {

    @BeforeEach
    public void setRestAssuredHttps() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config.redirect(RedirectConfig.redirectConfig().followRedirects(false));
    }
    
    @Test
    @TestProvider(TestRealmResourceTestProvider.class)
    public void maxQueuedRequestsTest(KeycloakRunner runner) {
        runner.run("start-dev", "--http-max-queued-requests=1", "--http-pool-max-threads=1");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<CompletableFuture<Integer>> statusCodesFuture = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                statusCodesFuture.add(CompletableFuture.supplyAsync(() ->
                        when().get("/realms/master/test-resources/slow").getStatusCode(), executor));
            }
            List<Integer> statusCodes = statusCodesFuture.stream().map(CompletableFuture::join).toList();

            assertThat("Some of the requests should be properly rejected", statusCodes, hasItem(503));
            assertThat("None of the requests should throw an unhandled exception", statusCodes, not(hasItem(500)));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @Launch({"start-dev", "--log-level=INFO,org.keycloak.quarkus.runtime.services.RejectNonNormalizedPathFilter:debug", "--http-access-log-enabled=true"})
    public void preventNonNormalizedURLs() {
        when().get("/realms/master").then().statusCode(200);
        when().get("/realms/xxx/../master").then().statusCode(400);
        given().urlEncodingEnabled(false)
                .when().get("/realms/master;xxx").then().statusCode(400);
    }
    
    @Test
    @Launch({"start-dev", "--hostname=https://example.com"})
    public void misdirectedRequestDetection() throws Exception {        
        // Build a trust-everything SSLContext so the self-signed dev cert is accepted
        TrustManager[] trustAll = new TrustManager[]{
            new javax.net.ssl.X509TrustManager() {
                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            }
        };
        javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, null);

        javax.net.ssl.SSLSocketFactory factory = sslContext.getSocketFactory();
        
        String status = http2Request(factory, "servicehost.com", "servicehost.com:8443");
        assertThat("Matching indicated to authority is allowed", status, Matchers.is("200"));
        
        status = http2Request(factory, null, "example.com:433");
        assertThat("No indicated name is allowed", status, Matchers.is("200"));
        
        // connection originated from another backend, but we're reusing it for a request to the keycloak server
        status = http2Request(factory, "other-example.com", "example.com:433");
        assertThat("Matching a known host is allowed", status, Matchers.is("200"));
        
        // connection originated from keycloak, but the browser is mistakenly reusing for another service
        status = http2Request(factory, "example.com", "misdirected.com:433");
        assertThat("Expected HTTP 421 Misdirected Request for SNI/authority mismatch",
                status, Matchers.is("421"));
    }
    
    private static String http2Request(javax.net.ssl.SSLSocketFactory factory, String indicatedHostname, String authority) throws IOException, SocketException {
        try (javax.net.ssl.SSLSocket socket =
                     (javax.net.ssl.SSLSocket) factory.createSocket()) {

            javax.net.ssl.SSLParameters params = socket.getSSLParameters();
            if (indicatedHostname != null) {
                params.setServerNames(List.of(new javax.net.ssl.SNIHostName(indicatedHostname)));
            }
            params.setApplicationProtocols(new String[]{"h2"});
            socket.setSSLParameters(params);

            socket.connect(new java.net.InetSocketAddress("localhost", 8443), 5_000);
            socket.startHandshake();

            java.io.DataOutputStream out = new java.io.DataOutputStream(socket.getOutputStream());
            java.io.DataInputStream  in  = new java.io.DataInputStream(socket.getInputStream());

            // ── HTTP/2 connection preface ──────────────────────────────────────
            out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            // SETTINGS frame: length=0, type=4, flags=0, stream=0
            out.write(new byte[]{0, 0, 0, 0x4, 0x0, 0, 0, 0, 0});
            out.flush();

            // ── HEADERS frame: GET /realms/master, :authority = authority ──────
            byte[] hpackAuthority = authority.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            byte[] hpackPath      = "/realms/master".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

            java.io.ByteArrayOutputStream hpack = new java.io.ByteArrayOutputStream();
            hpack.write(0x82);                   // :method GET  (static idx 2)
            hpack.write(0x87);                   // :scheme https (static idx 7)
            hpack.write(0x44);                   // literal+index, name = static idx 4 (:path)
            hpack.write(hpackPath.length);
            hpack.write(hpackPath);
            hpack.write(0x41);                   // literal+index, name = static idx 1 (:authority)
            hpack.write(hpackAuthority.length);
            hpack.write(hpackAuthority);

            byte[] headerBlock = hpack.toByteArray();

            // HEADERS frame header: 3-byte length, type=1, flags=END_HEADERS|END_STREAM=5, stream=1
            out.write((headerBlock.length >> 16) & 0xFF);
            out.write((headerBlock.length >> 8)  & 0xFF);
            out.write( headerBlock.length        & 0xFF);
            out.write(0x1);
            out.write(0x5);
            out.writeInt(1); // stream id 1
            out.write(headerBlock);
            out.flush();

            // ── Read HTTP/2 frames, skip SETTINGS/WINDOW_UPDATE, find HEADERS ──
            socket.setSoTimeout(5_000);
            String status = null;

            frameLoop:
            for (int i = 0; i < 20 && status == null; i++) {
                // Each HTTP/2 frame starts with a 9-byte header
                int lengthHigh = in.readUnsignedByte();
                int lengthMid  = in.readUnsignedByte();
                int lengthLow  = in.readUnsignedByte();
                int frameLength = (lengthHigh << 16) | (lengthMid << 8) | lengthLow;
                int frameType   = in.readUnsignedByte();
                int frameFlags  = in.readUnsignedByte();
                @SuppressWarnings("unused")
                int streamId    = in.readInt() & 0x7FFFFFFF; // mask reserved bit

                byte[] payload = new byte[frameLength];
                in.readFully(payload);

                switch (frameType) {
                    case 0x4: // SETTINGS
                        if ((frameFlags & 0x1) == 0) {
                            // Not an ACK — send our SETTINGS ACK back
                            out.write(new byte[]{0, 0, 0, 0x4, 0x1, 0, 0, 0, 0});
                            out.flush();
                        }
                        break;

                    case 0x8: // WINDOW_UPDATE — ignore
                        break;

                    case 0x1: // HEADERS — parse HPACK to find :status
                        int hpackOffset = 0;
                        // If PADDED flag (0x8) is set, skip 1-byte pad length
                        if ((frameFlags & 0x8) != 0) hpackOffset++;
                        // If PRIORITY flag (0x20) is set, skip 5-byte priority block
                        if ((frameFlags & 0x20) != 0) hpackOffset += 5;

                        status = extractStatus(payload, hpackOffset, payload.length - hpackOffset);
                        break frameLoop;

                    case 0x7: // GOAWAY — server rejected the connection
                        // payload[4..7] = last stream id, payload[8..11] = error code (big-endian)
                        int errorCode = ((payload[4] & 0xFF) << 24) | ((payload[5] & 0xFF) << 16)
                                | ((payload[6] & 0xFF) << 8) | (payload[7] & 0xFF);
                        System.err.println("GOAWAY received, error code: " + errorCode);
                        break frameLoop;

                    default:
                        // RST_STREAM (0x3), DATA (0x0), etc. — skip
                        break;
                }
            }
            
            return status;
        }
    }

    /**
     * Minimal HPACK decoder that only looks for the :status header.
     * Handles indexed representations (static table) and literal representations.
     * Does not update the dynamic table — sufficient for reading a typical response HEADERS frame.
     */
    private static String extractStatus(byte[] hpack, int offset, int length) {
        // HPACK static table entries for :status (indices 8-14):
        // 8=200, 9=204, 10=206, 11=304, 12=400, 13=404, 14=500
        String[] STATIC_STATUS = {"200","204","206","304","400","404","500"};

        int end = offset + length;
        while (offset < end) {
            int b = hpack[offset] & 0xFF;

            if ((b & 0x80) != 0) {
                // Indexed header field — RFC 7541 §6.1
                int index = b & 0x7F;
                if (index == 0x7F) { // multi-byte integer (unlikely for small indices)
                    index = 0x7F;
                    int shift = 0;
                    offset++;
                    while (offset < end) {
                        int next = hpack[offset++] & 0xFF;
                        index += (next & 0x7F) << shift;
                        shift += 7;
                        if ((next & 0x80) == 0) break;
                    }
                } else {
                    offset++;
                }
                // Static table :status entries are indices 8–14
                if (index >= 8 && index <= 14) {
                    return STATIC_STATUS[index - 8];
                }
                // :method=GET=2, :method=POST=3, :scheme=http=6, :scheme=https=7, :path=/=4, :path=/index.html=5
                // :authority=1 — none of these are :status; keep scanning
            } else if ((b & 0x40) != 0) {
                // Literal with incremental indexing — RFC 7541 §6.2.1
                int nameIndex = b & 0x3F;
                offset++;
                boolean isStatus = (nameIndex == 8); // static index 8 = :status
                if (nameIndex == 0) {
                    // Name is a literal string — skip it
                    int[] r = readString(hpack, offset, end);
                    isStatus = false; // we don't parse the name string here
                    offset = r[1];
                }
                int[] r = readString(hpack, offset, end);
                String value = new String(hpack, r[0], r[1] - r[0], java.nio.charset.StandardCharsets.US_ASCII);
                offset = r[1] + /* length-prefix bytes accounted in readString */ 0;
                offset = r[2]; // readString returns [valueStart, valueEnd, nextOffset]
                if (isStatus) return value;
            } else {
                // Literal without indexing (0x00) or never indexed (0x10) — RFC 7541 §6.2.2/6.2.3
                int nameIndex = b & (((b & 0x10) != 0) ? 0x0F : 0x0F);
                offset++;
                boolean isStatus = (nameIndex == 8);
                if (nameIndex == 0) {
                    int[] r = readString(hpack, offset, end);
                    offset = r[2];
                    isStatus = false;
                }
                int[] r = readString(hpack, offset, end);
                String value = new String(hpack, r[0], r[1] - r[0], java.nio.charset.StandardCharsets.US_ASCII);
                offset = r[2];
                if (isStatus) return value;
            }
        }
        return null;
    }

    /**
     * Reads one HPACK string (RFC 7541 §5.2).
     * Returns int[3]: {valueStart, valueEnd, nextOffset}.
     * Huffman-encoded strings are not decoded.
     */
    private static int[] readString(byte[] buf, int offset, int end) {
        int b = buf[offset] & 0xFF;
        boolean huffman = (b & 0x80) != 0;
        assertFalse(huffman);
        int len = b & 0x7F;
        offset++;
        if (len == 0x7F) { // multi-byte integer
            int shift = 0;
            len = 0x7F;
            while (offset < end) {
                int next = buf[offset++] & 0xFF;
                len += (next & 0x7F) << shift;
                shift += 7;
                if ((next & 0x80) == 0) break;
            }
        }
        int valueStart = offset;
        int valueEnd   = offset + len;
        return new int[]{valueStart, valueEnd, valueEnd};
    }


    @Test
    @Launch({"start-dev", "--http-access-log-enabled=true", "--http-accept-non-normalized-paths=true"})
    public void allowNonNormalizedURLs() {
        when().get("/realms/master").then().statusCode(200);
        when().get("/realms/xxx/../master").then().statusCode(200);
        given().urlEncodingEnabled(false)
                .when().get("/realms/master;xxx").then().statusCode(200);
    }

    @Test
    @Launch({"start-dev", "--https-certificates-reload-period=wrong"})
    public void testHttpCertificateReloadPeriod(CLIResult result) {
        result.assertError("Invalid duration");
    }

    @Test
    public void httpStoreTypeValidation(KeycloakRunner runner) {
        CLIResult result = runner.run("start", "--https-key-store-file=not-there.ks", "--hostname-strict=false");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: Unable to determine 'https-key-store-type' automatically. Adjust the file extension or specify the property");

        result = runner.run("start", "--https-trust-store-file=not-there.ks", "--hostname-strict=false");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: Unable to determine 'https-trust-store-type' automatically. Adjust the file extension or specify the property");

        result = runner.run("start", "--https-key-store-file=not-there.ks", "--hostname-strict=false", "--https-key-store-type=jdk");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: Failed to load 'https-*' material: NoSuchFileException not-there.ks");

        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        rawDist.copyOrReplaceFileFromClasspath("/server.keystore.pkcs12", Path.of("conf", "server.p12"));
        Path truststorePath = rawDist.getDistPath().resolve("conf").resolve("server.p12").toAbsolutePath();

        result = runner.run("start", "--https-trust-store-file=" + truststorePath, "--hostname-strict=false");
        result.assertExitCode(-1);
        result.assertMessage("ERROR: No trust store password provided");
    }
    
    @StopServer(Mode.MANUAL)
    @Test
    @Launch({"start", "--db=dev-file", "--hostname-strict=false", "--http-enabled=true"})
    void testStartNonLocalHttps(CLIResult cliResult) {
        cliResult.assertStarted();
        
        // should not be directed to create an admin user - we can't be sure if a local proxy is being used
        when().get("https://localhost:8443/").then().statusCode(200).body(containsString("You will need local access"));
    }
    
    @StopServer(Mode.MANUAL)
    @Test
    @Launch({"start", "--db=dev-file", "--proxy-headers=forwarded", "--hostname-strict=false", "--http-enabled=true"})
    void testStartLocalHttps(CLIResult cliResult) {
        cliResult.assertStarted();
        
        // should be directed to create an admin user, as the request is not setting the proxy header
        when().get("https://localhost:8443/").then().statusCode(200).body(Matchers.not(containsString("You will need local access")));
    }

    @Test
    @Launch({"start-dev", "--shutdown-delay=1s", "--shutdown-timeout=0s"})
    public void testShutdownParametersValidValues() {
        // Test that valid shutdown parameters are accepted (including 0s)
        when().get("/realms/master").then().statusCode(200);
    }

    @Test
    public void testShutdownParametersNegativeValue(KeycloakRunner runner) {
        // Test that negative values are rejected
        CLIResult result = runner.run("start-dev", "--shutdown-delay=-1s");
        result.assertError("Invalid duration '-1s'. Duration must be zero or positive");
    }
}
