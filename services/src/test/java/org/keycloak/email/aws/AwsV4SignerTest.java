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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.email.aws.credentials.AwsCredentials;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Pins the signer against AWS's own published signing example.
 * <p>
 * A signature is all-or-nothing: any deviation anywhere in the canonical request — a missing
 * newline, a header left unsorted, a payload hashed as Latin-1 — produces a different hex string.
 * Asserting the final {@code Authorization} value against AWS's expected output therefore verifies
 * every step at once, which is why these constants are worth more than a suite of assertions on the
 * intermediate strings.
 */
class AwsV4SignerTest {

    private static final String ACCESS_KEY_ID = "AKIDEXAMPLE";
    private static final String SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final Instant SIGNING_TIME = Instant.parse("2015-08-30T12:36:00Z");

    @Test
    void signsAwsPublishedGetVanillaExample() {
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/"),
                Map.of("Host", "example.amazonaws.com"), new byte[0]);

        Map<String, String> signed = new AwsV4Signer("us-east-1", "service")
                .sign(request, AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY), SIGNING_TIME);

        assertThat(signed.get("X-Amz-Date"), is("20150830T123600Z"));
        assertThat(signed.get("X-Amz-Security-Token"), is(nullValue()));
        assertThat(signed.get("Authorization"), is("AWS4-HMAC-SHA256 "
                + "Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                + "SignedHeaders=host;x-amz-date, "
                + "Signature=5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31"));
    }

    /**
     * AWS's {@code get-header-value-trim} case. The header arrives with leading, trailing and
     * repeated internal whitespace and must canonicalise to {@code value1 value2 value3};
     * {@code String.trim()} alone would leave the internal runs and sign something else.
     */
    @Test
    void collapsesWhitespaceInsideHeaderValues() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");
        headers.put("My-Header1", " value1  value2     value3");
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/"), headers, new byte[0]);

        Map<String, String> signed = new AwsV4Signer("us-east-1", "service")
                .sign(request, AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY), SIGNING_TIME);

        assertThat(signed.get("Authorization"), is("AWS4-HMAC-SHA256 "
                + "Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                + "SignedHeaders=host;my-header1;x-amz-date, "
                + "Signature=cfd34249e4b1c8d6b91ef74165d41a32e5fab3306300901bb65a51a73575eefd"));
    }

    /**
     * The shape this provider actually sends: a JSON POST to the SES endpoint with temporary
     * credentials. The session token has to be part of the signature and of {@code SignedHeaders},
     * not merely sent alongside — AWS rejects the request otherwise.
     */
    @Test
    void signsSesSendEmailRequestWithTemporaryCredentials() {
        byte[] body = ("{\"FromEmailAddress\":\"AldoTime <noreply@aldotime.it>\","
                + "\"Destination\":{\"ToAddresses\":[\"user@example.com\"]},"
                + "\"Content\":{\"Raw\":{\"Data\":\"UkFX\"}}}").getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "email.eu-central-1.amazonaws.com");
        headers.put("Content-Type", "application/json");
        AwsHttpRequest request = request("POST",
                URI.create("https://email.eu-central-1.amazonaws.com/v2/email/outbound-emails"), headers, body);
        AwsCredentials credentials = new AwsCredentials("ASIAEXAMPLE123456789", SECRET_ACCESS_KEY,
                "FwoGZXIvYXdzEExampleSessionToken==", Instant.parse("2026-09-03T11:00:00Z"));

        Map<String, String> signed = new AwsV4Signer("eu-central-1", "ses")
                .sign(request, credentials, Instant.parse("2026-09-03T10:15:30Z"));

        assertThat(signed.get("X-Amz-Security-Token"), is("FwoGZXIvYXdzEExampleSessionToken=="));
        assertThat(signed.get("Authorization"), is("AWS4-HMAC-SHA256 "
                + "Credential=ASIAEXAMPLE123456789/20260903/eu-central-1/ses/aws4_request, "
                + "SignedHeaders=content-type;host;x-amz-date;x-amz-security-token, "
                + "Signature=c747cbbba89b046b0741cea3b4886af64a0ad60e548e920ea0c9e5d730d547a9"));
    }

    /**
     * Headers an HTTP client is free to add or rewrite between signing and transmission must not
     * enter the signature — if they did, the request would be rejected whenever Apache HttpClient
     * decided to spell one differently. Asserting the signature is byte-identical to the vanilla case
     * proves they were excluded rather than merely tolerated.
     */
    @Test
    void ignoresHeadersTheHttpClientIsAllowedToRewrite() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");
        headers.put("User-Agent", "Apache-HttpClient/4.5.14");
        headers.put("Content-Length", "0");
        headers.put("Connection", "keep-alive");
        headers.put("Accept-Encoding", "gzip,deflate");
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/"), headers, new byte[0]);

        Map<String, String> signed = new AwsV4Signer("us-east-1", "service")
                .sign(request, AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY), SIGNING_TIME);

        assertThat(signed.get("Authorization"), is("AWS4-HMAC-SHA256 "
                + "Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                + "SignedHeaders=host;x-amz-date, "
                + "Signature=5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31"));
    }

    @Test
    void bindsTheSignatureToRegionAndService() {
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/"),
                Map.of("Host", "example.amazonaws.com"), new byte[0]);
        AwsCredentials credentials = AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        String signature = signatureOf(new AwsV4Signer("us-east-1", "service").sign(request, credentials, SIGNING_TIME));

        assertThat(signatureOf(new AwsV4Signer("eu-central-1", "service").sign(request, credentials, SIGNING_TIME)),
                is(not(signature)));
        // The one that bites in practice: SES answers on email.<region>.amazonaws.com but signs as
        // "ses". Deriving the signing name from the hostname yields "email" and a 403 on every send.
        assertThat(signatureOf(new AwsV4Signer("us-east-1", "email").sign(request, credentials, SIGNING_TIME)),
                is(not(signature)));
    }

    /**
     * The credential scope date and the {@code X-Amz-Date} header must come from the same instant.
     * Reading the clock twice makes them disagree for requests that straddle midnight UTC, which
     * fails roughly one send a day and is close to impossible to reproduce.
     */
    @Test
    void takesTheScopeDateFromTheSameInstantAsTheDateHeader() {
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/"),
                Map.of("Host", "example.amazonaws.com"), new byte[0]);

        Map<String, String> signed = new AwsV4Signer("us-east-1", "service").sign(request,
                AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY), Instant.parse("2026-12-31T23:59:59.999Z"));

        assertThat(signed.get("X-Amz-Date"), is("20261231T235959Z"));
        assertThat(signed.get("Authorization"), startsWith(
                "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20261231/us-east-1/service/aws4_request,"));
    }

    /**
     * Query parameters are ordered by name and then by value, which is not the same as ordering the
     * joined {@code name=value} strings: {@code =} (0x3D) sorts above {@code -} and {@code .}, so
     * {@code a-c=3} would come first under a plain lexicographic sort and the signature would differ.
     * <p>
     * The SES call carries no query string, so nothing in this provider exercises the path today —
     * which is exactly why it is pinned here rather than left to be discovered by the first caller
     * that does.
     */
    @Test
    void ordersQueryParametersByNameThenValue() {
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/?a=1&a-c=3&a.b=2"),
                Map.of("Host", "example.amazonaws.com"), new byte[0]);

        Map<String, String> signed = new AwsV4Signer("us-east-1", "service")
                .sign(request, AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY), SIGNING_TIME);

        assertThat(signed.get("Authorization"), is("AWS4-HMAC-SHA256 "
                + "Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                + "SignedHeaders=host;x-amz-date, "
                + "Signature=f139a4ac0cd3b1b7d6490a97b1ce73519a87bf38f10bad6298ccadc4b9dfcc2c"));
    }

    /**
     * A {@code +} in a query is a literal plus, not a space. Decoding it as a space — which is what
     * {@code URLDecoder} does, because it decodes form bodies — and re-encoding would emit
     * {@code %20} where the request carried {@code %2B}, signing something the server never received.
     */
    @Test
    void treatsAPlusInTheQueryAsALiteralPlus() {
        AwsHttpRequest request = request("GET", URI.create("https://example.amazonaws.com/?a=1%2B2&b=x%2By"),
                Map.of("Host", "example.amazonaws.com"), new byte[0]);

        Map<String, String> signed = new AwsV4Signer("us-east-1", "service")
                .sign(request, AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY), SIGNING_TIME);

        assertThat(signed.get("Authorization"), is("AWS4-HMAC-SHA256 "
                + "Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                + "SignedHeaders=host;x-amz-date, "
                + "Signature=f586138b3e5d10112110b90d6de1e2bed89e271ddcd6115cbd69a376dd399eae"));
    }

    /**
     * An encoded slash is part of a path segment, not a separator. Canonicalising from the decoded
     * path would collapse {@code /v2/a%2Fb} onto {@code /v2/a/b} and sign a path the caller never
     * wrote — so the two must produce different signatures.
     */
    @Test
    void keepsAnEncodedSlashInsideItsPathSegment() {
        AwsCredentials credentials = AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        AwsV4Signer signer = new AwsV4Signer("us-east-1", "service");
        Map<String, String> headers = Map.of("Host", "example.amazonaws.com");

        String encoded = signer.sign(request("GET", URI.create("https://example.amazonaws.com/v2/a%2Fb"),
                headers, new byte[0]), credentials, SIGNING_TIME).get("Authorization");
        String separated = signer.sign(request("GET", URI.create("https://example.amazonaws.com/v2/a/b"),
                headers, new byte[0]), credentials, SIGNING_TIME).get("Authorization");

        assertThat(encoded, is(not(separated)));
    }

    @Test
    void percentDecodesWithoutTheFormBodyRules() {
        assertThat(AwsV4Signer.percentDecode("a+b"), is("a+b"));
        assertThat(AwsV4Signer.percentDecode("a%2Bb"), is("a+b"));
        assertThat(AwsV4Signer.percentDecode("caf%C3%A9"), is("café"));
        assertThat(AwsV4Signer.percentDecode("nothing to decode"), is("nothing to decode"));
        assertThat(AwsV4Signer.percentDecode("100%"), is("100%"));
    }

    @Test
    void percentEncodesPerRfc3986RatherThanAsAFormBody() {
        // java.net.URLEncoder would give "a+b" for the space and escape the tilde: both produce a
        // wrong signature, and both are what a reviewer's first instinct would reach for.
        assertThat(AwsV4Signer.uriEncode("a b"), is("a%20b"));
        assertThat(AwsV4Signer.uriEncode("~-_."), is("~-_."));
        assertThat(AwsV4Signer.uriEncode("*"), is("%2A"));
        assertThat(AwsV4Signer.uriEncode("/"), is("%2F"));
        assertThat(AwsV4Signer.uriEncode("ሴ"), is("%E1%88%B4"));
        assertThat(AwsV4Signer.uriEncode("café"), is("caf%C3%A9"));
    }

    @Test
    void normalisesHeaderValuesForCanonicalisation() {
        assertThat(AwsV4Signer.normalizeHeaderValue("  a   b  "), is("a b"));
        assertThat(AwsV4Signer.normalizeHeaderValue("\ta\tb\t"), is("a b"));
        assertThat(AwsV4Signer.normalizeHeaderValue(""), is(""));
        assertThat(AwsV4Signer.normalizeHeaderValue(null), is(""));
    }

    @Test
    void hashesTheEmptyPayloadToTheKnownSha256() {
        assertThat(AwsV4Signer.hexSha256(new byte[0]),
                equalTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    private static String signatureOf(Map<String, String> signedHeaders) {
        String authorization = signedHeaders.get("Authorization");
        return authorization.substring(authorization.indexOf("Signature=") + "Signature=".length());
    }

    private static AwsHttpRequest request(String method, URI uri, Map<String, String> headers, byte[] body) {
        return new AwsHttpRequest(method, uri, headers, body, 1000, 1000);
    }
}
