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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.email.aws.credentials.AwsCredentials;

/**
 * AWS Signature Version 4 (the {@code AWS4-HMAC-SHA256} scheme) for JSON/REST services.
 * <p>
 * Implemented directly rather than pulled in with an SDK so that the provider adds no runtime
 * dependency to the server. The algorithm is fully specified by AWS and is verified here against
 * AWS's own published test vectors (see {@code AwsV4SignerTest}), which is what makes hand-rolling
 * it defensible: the vectors are a byte-level oracle for every step — canonical request, string to
 * sign and signature — so an error cannot hide behind a passing "it returns something" assertion.
 * <p>
 * Two details are worth knowing before touching this class:
 * <ul>
 * <li>The signing <em>service</em> name is not the hostname prefix. SES answers on
 * {@code email.<region>.amazonaws.com} but signs as {@code ses}; deriving one from the other
 * produces a 403 on every request.</li>
 * <li>Only the headers passed in are signed. Whatever the HTTP client adds afterwards (Host when
 * unset, Content-Length, User-Agent, Accept-Encoding, Connection) must stay out of the signature,
 * so the caller sets every signed header explicitly and this class leaves the hop-by-hop ones out of
 * the signature.</li>
 * </ul>
 */
public final class AwsV4Signer {

    public static final String X_AMZ_DATE = "X-Amz-Date";
    public static final String X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";
    public static final String AUTHORIZATION = "Authorization";

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String TERMINATOR = "aws4_request";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final DateTimeFormatter AMZ_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /**
     * Headers a client is entitled to add, rewrite or drop between signing and transmission. Signing
     * any of them makes the signature depend on state this class does not control; AWS lists them as
     * headers that must not be signed.
     */
    private static final Set<String> UNSIGNABLE_HEADERS = Set.of(
            "authorization", "connection", "content-length", "expect", "keep-alive", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "user-agent", "accept-encoding", "x-amzn-trace-id");

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final String region;
    private final String service;

    public AwsV4Signer(String region, String service) {
        this.region = region;
        this.service = service;
    }

    /**
     * Signs {@code request} and returns the headers that must be added to it: {@code X-Amz-Date},
     * {@code X-Amz-Security-Token} when the credentials are temporary, and {@code Authorization}.
     * <p>
     * {@code signingTime} is a parameter rather than a {@code now()} call inside the method for two
     * reasons: the tests need a fixed clock to compare against AWS's vectors, and the date must be
     * read exactly once — computing the {@code X-Amz-Date} header and the credential-scope date from
     * two separate reads mismatches them for the requests that straddle midnight UTC.
     */
    public Map<String, String> sign(AwsHttpRequest request, AwsCredentials credentials, Instant signingTime) {
        String amzDateTime = AMZ_DATE_TIME.format(signingTime);
        String date = amzDateTime.substring(0, 8);

        Map<String, String> headersToSign = new TreeMap<>();
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            String name = header.getKey().toLowerCase(Locale.ROOT);
            if (!UNSIGNABLE_HEADERS.contains(name)) {
                headersToSign.put(name, normalizeHeaderValue(header.getValue()));
            }
        }
        headersToSign.put("x-amz-date", amzDateTime);
        if (credentials.isTemporary()) {
            // Must be signed, not merely sent: AWS rejects a request whose session token is outside
            // SignedHeaders, and the signature differs from the one computed without it.
            headersToSign.put("x-amz-security-token", normalizeHeaderValue(credentials.sessionToken()));
        }

        String signedHeaders = String.join(";", headersToSign.keySet());
        String canonicalRequest = canonicalRequest(request, headersToSign, signedHeaders);
        String credentialScope = date + "/" + region + "/" + service + "/" + TERMINATOR;
        String stringToSign = ALGORITHM + "\n" + amzDateTime + "\n" + credentialScope + "\n" + hexSha256(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = signingKey(credentials.secretAccessKey(), date);
        String signature = hex(hmacSha256(signingKey, stringToSign.getBytes(StandardCharsets.UTF_8)));

        Map<String, String> signedRequestHeaders = new LinkedHashMap<>();
        signedRequestHeaders.put(X_AMZ_DATE, amzDateTime);
        if (credentials.isTemporary()) {
            signedRequestHeaders.put(X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
        }
        signedRequestHeaders.put(AUTHORIZATION, ALGORITHM
                + " Credential=" + credentials.accessKeyId() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature);
        return signedRequestHeaders;
    }

    String canonicalRequest(AwsHttpRequest request, Map<String, String> sortedHeadersToSign, String signedHeaders) {
        StringBuilder canonicalHeaders = new StringBuilder();
        for (Map.Entry<String, String> header : sortedHeadersToSign.entrySet()) {
            canonicalHeaders.append(header.getKey()).append(':').append(header.getValue()).append('\n');
        }
        // The blank line between the headers block and SignedHeaders is required: every header line
        // ends with \n and the block itself is then followed by another \n.
        return request.method().toUpperCase(Locale.ROOT) + "\n"
                + canonicalUri(request) + "\n"
                + canonicalQueryString(request) + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hexSha256(request.body());
    }

    private static String canonicalUri(AwsHttpRequest request) {
        // The RAW path, not the decoded one: URI#getPath turns a %2F inside a segment back into a
        // slash, which then reads as a segment separator and changes the canonical request — and so
        // the signature — for a path the caller never wrote.
        String path = request.uri().getRawPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        StringBuilder canonical = new StringBuilder();
        // Split on '/' keeping empty segments: an empty segment is a genuine "//" in the path and
        // must survive into the canonical form, which is why the -1 limit is not optional.
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                canonical.append('/');
            }
            canonical.append(uriEncode(percentDecode(segments[i])));
        }
        return canonical.toString();
    }

    private static String canonicalQueryString(AwsHttpRequest request) {
        String rawQuery = request.uri().getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<String[]> parameters = new ArrayList<>();
        for (String parameter : rawQuery.split("&", -1)) {
            int separator = parameter.indexOf('=');
            String name = separator < 0 ? parameter : parameter.substring(0, separator);
            String value = separator < 0 ? "" : parameter.substring(separator + 1);
            parameters.add(new String[]{uriEncode(percentDecode(name)), uriEncode(percentDecode(value))});
        }
        // Sorted by name and then by value, which is not the same as sorting the joined
        // "name=value" strings: '=' (0x3D) sorts above '-', '.' and the digits, so "a-c=3" would
        // come before "a=1" under a plain lexicographic sort and produce a different signature.
        parameters.sort(Comparator.<String[], String>comparing(parameter -> parameter[0])
                .thenComparing(parameter -> parameter[1]));

        StringJoiner canonical = new StringJoiner("&");
        for (String[] parameter : parameters) {
            canonical.add(parameter[0] + "=" + parameter[1]);
        }
        return canonical.toString();
    }

    /**
     * Percent-encodes per RFC 3986 as AWS requires: unreserved characters pass through, everything
     * else becomes uppercase-hex triplets of its UTF-8 bytes.
     * <p>
     * {@link java.net.URLEncoder} cannot be used here — it is {@code application/x-www-form-urlencoded},
     * so it emits {@code +} for a space and escapes {@code ~}, both of which produce a wrong
     * signature. AWS's signing documentation says so explicitly.
     */
    static String uriEncode(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = b & 0xFF;
            if ((unsigned >= 'A' && unsigned <= 'Z') || (unsigned >= 'a' && unsigned <= 'z')
                    || (unsigned >= '0' && unsigned <= '9')
                    || unsigned == '-' || unsigned == '_' || unsigned == '.' || unsigned == '~') {
                encoded.append((char) unsigned);
            } else {
                encoded.append('%')
                        .append(Character.toUpperCase(HEX[unsigned >> 4]))
                        .append(Character.toUpperCase(HEX[unsigned & 0x0F]));
            }
        }
        return encoded.toString();
    }

    /**
     * Percent-decodes a URI component.
     * <p>
     * {@link java.net.URLDecoder} cannot be used: it decodes {@code application/x-www-form-urlencoded},
     * where {@code +} means a space. In a URI a {@code +} is a literal plus, so decoding it as a space
     * and re-encoding would emit {@code %20} where the request carried {@code %2B} — a valid-looking
     * canonical string that signs something the server never received.
     * <p>
     * Works on the UTF-8 bytes rather than the characters so that a multi-byte sequence split across
     * percent triplets reassembles correctly.
     */
    static String percentDecode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        byte[] source = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(source.length);
        for (int i = 0; i < source.length; i++) {
            if (source[i] == '%' && i + 2 < source.length) {
                int high = Character.digit((char) source[i + 1], 16);
                int low = Character.digit((char) source[i + 2], 16);
                if (high >= 0 && low >= 0) {
                    decoded.write((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            decoded.write(source[i]);
        }
        return decoded.toString(StandardCharsets.UTF_8);
    }

    /**
     * Trims the value and collapses every run of whitespace into a single space, as the canonical
     * form requires. {@code String.trim()} alone is not enough: {@code "a   b"} and {@code "a b"}
     * must canonicalise to the same bytes.
     */
    static String normalizeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(value.length());
        boolean pendingSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ' ' || c == '\t') {
                pendingSpace = normalized.length() > 0;
            } else {
                if (pendingSpace) {
                    normalized.append(' ');
                    pendingSpace = false;
                }
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    /**
     * Derives the request-scoped signing key: HMAC chained over date, region, service and the
     * {@code aws4_request} terminator, so the key that signs a request is useless for any other
     * date, region or service.
     */
    private byte[] signingKey(String secretAccessKey, String date) {
        byte[] initial = ("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8);
        byte[] dateKey = hmacSha256(initial, date.getBytes(StandardCharsets.UTF_8));
        byte[] regionKey = hmacSha256(dateKey, region.getBytes(StandardCharsets.UTF_8));
        byte[] serviceKey = hmacSha256(regionKey, service.getBytes(StandardCharsets.UTF_8));
        return hmacSha256(serviceKey, TERMINATOR.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available in this JVM", e);
        }
    }

    static String hexSha256(byte[] payload) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
        }
        return hex.toString();
    }
}
