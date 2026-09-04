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

package org.keycloak.email.aws.credentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.keycloak.email.aws.AwsHttpRequest;
import org.keycloak.email.aws.AwsHttpResponse;
import org.keycloak.email.aws.AwsHttpTransport;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * The credentials of the IAM role attached to an EC2 instance, read from the instance metadata
 * service (IMDS) on the link-local address.
 * <p>
 * <strong>IMDSv2 only.</strong> Every read here is authenticated with a session token obtained by a
 * {@code PUT}, which a browser-driven request cannot issue cross-origin and whose response AWS
 * returns with an IP hop limit of 1 so it cannot be relayed off the instance. IMDSv1 — the
 * unauthenticated {@code GET http://169.254.169.254/latest/meta-data/…} that AWS itself now
 * discourages and that new instances disable by default — is deliberately not implemented: it is
 * the SSRF-amplifier variant, and a Keycloak that never speaks it cannot be tricked, by a
 * server-side request forgery anywhere else in the server, into handing out the instance's role
 * credentials through a single forged GET. The price is that this source stays silent on an
 * instance that only offers IMDSv1, which is the right way round.
 * <p>
 * Finding nothing is the normal case, not an error: this provider sits last in the chain, so on
 * every machine that is not an EC2 instance — a laptop, a container on someone else's cloud, a
 * plain VPS — the token call fails and {@link #resolve} returns {@code null} quietly. Logging that
 * at anything above {@code DEBUG} would put a stack trace in the server log on every email sent.
 */
public final class InstanceMetadataCredentialsProvider implements AwsCredentialsProvider {

    private static final Logger logger = Logger.getLogger(InstanceMetadataCredentialsProvider.class);

    private static final String METADATA_DISABLED_ENV = "AWS_EC2_METADATA_DISABLED";
    /** The spelling the AWS SDK for Java v2 uses; it is not the camel case of the variable above. */
    private static final String METADATA_DISABLED_PROPERTY = "aws.disableEc2Metadata";
    private static final String ENDPOINT_ENV = "AWS_EC2_METADATA_SERVICE_ENDPOINT";

    private static final String METADATA_IPV4 = "169.254.169.254";
    /** The one IPv6 address AWS documents for the instance metadata service, not the range around it. */
    private static final String METADATA_IPV6 = "fd00:ec2::254";
    private static final String DEFAULT_ENDPOINT = "http://" + METADATA_IPV4;

    private static final String TOKEN_PATH = "/latest/api/token";
    private static final String SECURITY_CREDENTIALS_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String TOKEN_HEADER = "X-aws-ec2-metadata-token";
    private static final String TOKEN_TTL_HEADER = "X-aws-ec2-metadata-token-ttl-seconds";
    private static final String TOKEN_TTL_SECONDS = "21600";

    /**
     * A credential lookup runs inside the transaction that is sending a mail, on the link-local
     * address of the host itself. A second is already generous; anything longer would turn a
     * black-holed 169.254.0.0/16 route into a hung Keycloak thread.
     */
    private static final int TIMEOUT_MILLIS = 1000;

    private final AwsEnvironment environment;

    public InstanceMetadataCredentialsProvider(AwsEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
        if (isDisabled()) {
            return null;
        }
        URI endpoint = endpoint();
        String token = fetchToken(transport, endpoint);
        if (token == null) {
            return null;
        }
        String role = fetchRoleName(transport, endpoint, token);
        if (role == null) {
            return null;
        }
        return fetchCredentials(transport, endpoint, token, role);
    }

    @Override
    public boolean requiresDirectConnection() {
        return true;
    }

    @Override
    public String name() {
        return "EC2 instance metadata (IMDSv2)";
    }

    private boolean isDisabled() {
        return "true".equalsIgnoreCase(environment.value(METADATA_DISABLED_ENV))
                || "true".equalsIgnoreCase(environment.property(METADATA_DISABLED_PROPERTY));
    }

    private URI endpoint() throws AwsCredentialsException {
        String configured = environment.value(ENDPOINT_ENV);
        String raw = configured == null ? DEFAULT_ENDPOINT : configured;
        URI endpoint;
        try {
            endpoint = new URI(raw.replaceAll("/+$", ""));
        } catch (URISyntaxException e) {
            throw new AwsCredentialsException(ENDPOINT_ENV + " is not a valid URI: " + raw, e);
        }
        if (!isAllowedEndpoint(endpoint)) {
            throw new AwsCredentialsException(ENDPOINT_ENV + " is set to " + raw + ", which is neither the EC2"
                    + " instance metadata service nor a loopback address. Over plain HTTP only " + METADATA_IPV4
                    + ", " + METADATA_IPV6 + " and loopback are accepted; use https for anything else.");
        }
        return endpoint;
    }

    /**
     * Confines the un-encrypted, un-authenticated leg of this exchange to the addresses that can
     * only be the host itself. Without it, an operator-supplied (or, worse, injected) endpoint would
     * make Keycloak fetch an attacker's URL and hand whatever came back to the signer as
     * credentials. Over TLS the peer is authenticated by its certificate, so any host is fine there.
     */
    private static boolean isAllowedEndpoint(URI endpoint) {
        if ("https".equalsIgnoreCase(endpoint.getScheme())) {
            return true;
        }
        String host = endpoint.getHost();
        if (host == null) {
            return false;
        }
        if (host.equalsIgnoreCase("localhost")) {
            return true;
        }
        // A name is refused rather than resolved: resolving it is the request forgery being guarded
        // against, and a DNS answer of 127.0.0.1 today says nothing about the next lookup.
        return LocalAddress.is(host, METADATA_IPV4)
                || LocalAddress.is(host, METADATA_IPV6)
                || LocalAddress.isLoopback(host);
    }

    /**
     * @return the IMDSv2 session token, or {@code null} when there is no metadata service to talk
     *         to — which is what every non-EC2 host answers, and is not a failure
     */
    private String fetchToken(AwsHttpTransport transport, URI endpoint) {
        AwsHttpRequest request = new AwsHttpRequest("PUT", URI.create(endpoint + TOKEN_PATH),
                Map.of(TOKEN_TTL_HEADER, TOKEN_TTL_SECONDS), AwsHttpRequest.NO_BODY,
                TIMEOUT_MILLIS, TIMEOUT_MILLIS);
        AwsHttpResponse response;
        try {
            response = transport.exchange(request);
        } catch (IOException e) {
            logger.debugf(e, "No EC2 instance metadata service at %s, this server is not an EC2 instance", endpoint);
            return null;
        }
        if (!response.isSuccessful()) {
            logger.debugf("EC2 instance metadata service at %s refused an IMDSv2 token with HTTP %d", endpoint,
                    response.status());
            return null;
        }
        String token = response.bodyAsString().trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * @return the first role in the instance profile, or {@code null} when no role is attached; from
     *         here on a failure is loud, because the successful token call proved this is an EC2
     *         instance and therefore that this source <em>is</em> configured
     */
    private String fetchRoleName(AwsHttpTransport transport, URI endpoint, String token) throws AwsCredentialsException {
        URI uri = URI.create(endpoint + SECURITY_CREDENTIALS_PATH);
        AwsHttpResponse response = get(transport, uri, token);
        if (response.status() == 404) {
            return null;
        }
        if (!response.isSuccessful()) {
            throw new AwsCredentialsException("EC2 instance metadata service answered HTTP " + response.status()
                    + " for " + uri);
        }
        for (String line : response.bodyAsString().split("\n")) {
            String role = line.trim();
            if (!role.isEmpty()) {
                return role;
            }
        }
        // An empty list says the same thing as the 404 above: no role is attached to this instance.
        return null;
    }

    private AwsCredentials fetchCredentials(AwsHttpTransport transport, URI endpoint, String token, String role)
            throws AwsCredentialsException {
        URI uri = URI.create(endpoint + SECURITY_CREDENTIALS_PATH + encodePathSegment(role));
        AwsHttpResponse response = get(transport, uri, token);
        if (!response.isSuccessful()) {
            throw new AwsCredentialsException("EC2 instance metadata service answered HTTP " + response.status()
                    + " for the credentials of role " + role);
        }

        JsonNode document;
        try {
            document = JsonSerialization.mapper.readTree(response.body());
        } catch (IOException e) {
            // The cause is dropped on purpose: Jackson quotes the offending source in its message,
            // and that source is the credential document itself.
            throw new AwsCredentialsException("EC2 instance metadata service returned an unreadable credential"
                    + " document for role " + role);
        }

        if (document == null) {
            // Defensive rather than reachable: Jackson answers an empty body with MissingNode. Kept
            // so a change of parser cannot turn an empty 200 into a NullPointerException here.
            throw new AwsCredentialsException("EC2 instance metadata service returned an empty credential"
                    + " document for role " + role);
        }

        String code = text(document, "Code");
        if (!"Success".equals(code)) {
            throw new AwsCredentialsException("EC2 instance metadata service reported Code=" + code
                    + " for role " + role);
        }
        String accessKeyId = text(document, "AccessKeyId");
        String secretAccessKey = text(document, "SecretAccessKey");
        // Required rather than optional: an instance role is always temporary, and credentials used
        // without their session token come back from SES as a bare SignatureDoesNotMatch.
        String sessionToken = text(document, "Token");
        if (accessKeyId == null || secretAccessKey == null || sessionToken == null) {
            throw new AwsCredentialsException("EC2 instance metadata service returned an incomplete credential"
                    + " document for role " + role + ": AccessKeyId, SecretAccessKey or Token is missing");
        }
        return new AwsCredentials(accessKeyId, secretAccessKey, sessionToken,
                expiration(text(document, "Expiration"), role, accessKeyId));
    }

    private AwsHttpResponse get(AwsHttpTransport transport, URI uri, String token) throws AwsCredentialsException {
        try {
            return transport.exchange(AwsHttpRequest.get(uri, Map.of(TOKEN_HEADER, token),
                    TIMEOUT_MILLIS, TIMEOUT_MILLIS));
        } catch (IOException e) {
            // Only the URI goes into the message; rendering the request would print the token header.
            throw new AwsCredentialsException("EC2 instance metadata service call to " + uri + " failed", e);
        }
    }

    /**
     * Role credentials always expire, so an {@code Expiration} that cannot be read is treated as an
     * error rather than as "never expires": the latter would cache a credential past its life and
     * turn the whole realm's email into 403s an hour later, with nothing in the log to explain it.
     */
    private static Instant expiration(String value, String role, String accessKeyId) throws AwsCredentialsException {
        if (value == null) {
            // Absent counts the same as unparseable. Instance-profile credentials always expire, so
            // "no expiry" is not a document this endpoint can legitimately produce, and accepting it
            // would cache a credential for the life of the process.
            throw new AwsCredentialsException("EC2 instance metadata service returned credentials without an"
                    + " Expiration for role " + role + " (access key id " + accessKeyId + ")");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new AwsCredentialsException("EC2 instance metadata service returned an unparseable Expiration \""
                    + value + "\" for role " + role + " (access key id " + accessKeyId + ")", e);
        }
    }

    /**
     * Reads a string field, collapsing absent, null and blank alike into {@code null}. Blank has to
     * count as missing: an empty {@code SecretAccessKey} would sign every message with the empty
     * string and surface, much later and somewhere else, as an opaque SES SignatureDoesNotMatch.
     */
    private static String text(JsonNode document, String field) {
        String value = document.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * The role name is read off the wire and then spliced into a URL path, so it is encoded, not
     * trusted. {@link URLEncoder} is form encoding rather than path encoding — the one difference
     * that matters is the space, which it renders as {@code +}; a literal {@code +} has already
     * become {@code %2B} by then, so rewriting what is left is safe.
     */
    private static String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
