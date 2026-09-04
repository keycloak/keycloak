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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.email.aws.AwsHttpRequest;
import org.keycloak.email.aws.AwsHttpResponse;
import org.keycloak.email.aws.AwsHttpTransport;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * Credentials handed out by a container credential endpoint — an ECS task role, or the EKS Pod
 * Identity agent.
 * <p>
 * The runtime that starts the container exports one of two variables:
 * {@code AWS_CONTAINER_CREDENTIALS_RELATIVE_URI}, a path resolved against the fixed ECS endpoint
 * {@code http://169.254.170.2}, or {@code AWS_CONTAINER_CREDENTIALS_FULL_URI}, an absolute URI.
 * Neither one set means the server is not running under such a runtime, and the chain moves on.
 * <p>
 * Two details of this endpoint differ from the STS ones and are both asserted in the tests: the
 * session token arrives in a field named {@code Token} rather than {@code SessionToken}, and the
 * request may carry a bearer token of its own, which is what makes the full-URI validation below a
 * security control rather than an input check.
 */
public final class ContainerCredentialsProvider implements AwsCredentialsProvider {

    private static final Logger logger = Logger.getLogger(ContainerCredentialsProvider.class);

    private static final String RELATIVE_URI_VARIABLE = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
    private static final String FULL_URI_VARIABLE = "AWS_CONTAINER_CREDENTIALS_FULL_URI";
    private static final String AUTHORIZATION_TOKEN_VARIABLE = "AWS_CONTAINER_AUTHORIZATION_TOKEN";
    private static final String AUTHORIZATION_TOKEN_FILE_VARIABLE = "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE";

    /** The ECS task metadata endpoint. Fixed by the agent, never configurable. */
    private static final String ECS_ENDPOINT = "http://169.254.170.2";

    /**
     * The link-local addresses AWS's own credential agents listen on: the ECS task metadata endpoint
     * and the EKS Pod Identity agent, in its IPv4 and IPv6 form. A list, not a set, so the order in
     * the rejection message is the same on every JVM.
     */
    private static final List<String> ALLOWED_LINK_LOCAL_HOSTS =
            List.of("169.254.170.2", "169.254.170.23", "fd00:ec2::23");

    /**
     * One second either way. The endpoint answers from the local link or from a loopback socket, so
     * anything slower than this is down rather than busy — and this lookup runs inside the
     * transaction that sends the mail, where a hang is worse than a failure.
     */
    private static final int TIMEOUT_MILLIS = 1000;

    private final AwsEnvironment environment;

    public ContainerCredentialsProvider(AwsEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public boolean requiresDirectConnection() {
        return true;
    }

    @Override
    public String name() {
        return "container credentials (ECS task role / EKS Pod Identity)";
    }

    @Override
    public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
        URI endpoint = endpoint();
        if (endpoint == null) {
            return null;
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        String token = authorizationToken();
        if (token != null) {
            // Verbatim, without a scheme prefix: both the ECS and the Pod Identity agent compare the
            // header against the value they generated. Never logged, at any level.
            headers.put("Authorization", token);
        }

        logger.debugf("Fetching AWS credentials from the container credential endpoint at %s", endpoint.getHost());

        AwsHttpResponse response;
        try {
            response = transport.exchange(AwsHttpRequest.get(endpoint, headers, TIMEOUT_MILLIS, TIMEOUT_MILLIS));
        } catch (IOException e) {
            throw new AwsCredentialsException("Container credential endpoint " + endpoint.getHost()
                    + " is not reachable", e);
        }
        if (!response.isSuccessful()) {
            // The status and nothing else: the body of a failed credential response is undocumented,
            // and an endpoint that is not the one we think it is could echo the authorization token
            // straight back into the server log.
            throw new AwsCredentialsException("Container credential endpoint " + endpoint.getHost()
                    + " returned HTTP " + response.status());
        }
        return parse(response, endpoint.getHost());
    }

    /**
     * The URI to call, or {@code null} when neither variable is set. The relative form wins when both
     * are, as it does in the AWS SDKs.
     */
    private URI endpoint() throws AwsCredentialsException {
        String relative = environment.value(RELATIVE_URI_VARIABLE);
        if (relative != null) {
            String path = relative.startsWith("/") ? relative : "/" + relative;
            return uri(ECS_ENDPOINT + path, RELATIVE_URI_VARIABLE);
        }
        String full = environment.value(FULL_URI_VARIABLE);
        return full == null ? null : validated(uri(full, FULL_URI_VARIABLE));
    }

    private static URI uri(String value, String variable) throws AwsCredentialsException {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            // The variable name, not its value: a full URI is operator-supplied and may carry
            // anything in its query string.
            throw new AwsCredentialsException(variable + " is not a valid URI", e);
        }
    }

    /**
     * Rejects a full URI that is neither HTTPS nor local.
     * <p>
     * This is a deliberate SSRF guard rather than a tidy input check: the request about to be made
     * carries the container authorization token, and {@code AWS_CONTAINER_CREDENTIALS_FULL_URI} is a
     * plain environment variable. Without this, whoever can set that variable — an edited deployment
     * manifest, an injected env entry — turns Keycloak into a credential-forwarding proxy that hands
     * the token to a host of their choosing, in cleartext.
     * <p>
     * The check is lexical on purpose. Resolving the host to decide whether it is really loopback
     * would put an untimed DNS lookup inside the mail transaction and would leave a rebinding window
     * between the decision and the connection.
     */
    private static URI validated(URI uri) throws AwsCredentialsException {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new AwsCredentialsException(FULL_URI_VARIABLE + " must be an absolute http or https URI");
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return uri;
        }
        if ("http".equalsIgnoreCase(scheme) && isLocal(host)) {
            return uri;
        }
        throw new AwsCredentialsException(FULL_URI_VARIABLE + " points at " + host + ", which is refused: without"
                + " https the container authorization token may only be sent to a loopback address or to one of the"
                + " AWS credential endpoints " + String.join(", ", ALLOWED_LINK_LOCAL_HOSTS) + ".");
    }

    private static boolean isLocal(String host) {
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }
        if (LocalAddress.isLoopback(host)) {
            return true;
        }
        return ALLOWED_LINK_LOCAL_HOSTS.stream().anyMatch(allowed -> LocalAddress.is(host, allowed));
    }

    /**
     * The value for the {@code Authorization} header, or {@code null} when the runtime uses none — a
     * bare ECS task role does not.
     * <p>
     * The file form takes precedence, as in the AWS SDKs: EKS Pod Identity rotates the token on disk,
     * so a copy captured in the environment at startup must never win over the file the agent keeps
     * current.
     */
    private String authorizationToken() throws AwsCredentialsException {
        String file = environment.value(AUTHORIZATION_TOKEN_FILE_VARIABLE);
        if (file == null) {
            return environment.value(AUTHORIZATION_TOKEN_VARIABLE);
        }
        String token;
        try {
            // Trimmed: the file ends with a newline the agent never meant as part of the value, and a
            // header carrying CR or LF is at best rejected by the client, at worst a splitting vector.
            token = Files.readString(Paths.get(file), StandardCharsets.UTF_8).trim();
        } catch (IOException | InvalidPathException e) {
            throw new AwsCredentialsException(AUTHORIZATION_TOKEN_FILE_VARIABLE + " (" + file + ") cannot be read", e);
        }
        if (token.isEmpty()) {
            throw new AwsCredentialsException(AUTHORIZATION_TOKEN_FILE_VARIABLE + " (" + file + ") is empty");
        }
        return token;
    }

    private static AwsCredentials parse(AwsHttpResponse response, String host) throws AwsCredentialsException {
        JsonNode body;
        try {
            body = JsonSerialization.mapper.readTree(response.body());
        } catch (IOException e) {
            // The cause is dropped on purpose: Jackson quotes the offending source in its message,
            // and that source is the credential document.
            throw new AwsCredentialsException("Container credential endpoint " + host
                    + " returned a body that is not JSON");
        }

        if (body == null) {
            // Defensive rather than reachable: Jackson answers an empty body with MissingNode. Kept
            // so a change of parser cannot turn an empty 200 into a NullPointerException here.
            throw new AwsCredentialsException("Container credential endpoint " + host + " returned an empty body");
        }

        String accessKeyId = text(body, "AccessKeyId");
        String secretAccessKey = text(body, "SecretAccessKey");
        if (accessKeyId == null || secretAccessKey == null) {
            throw new AwsCredentialsException("Container credential endpoint " + host
                    + " returned a response without AccessKeyId or SecretAccessKey");
        }
        // "Token", not "SessionToken": this endpoint and STS name the same value differently, and
        // reading the STS name here yields credentials that sign happily and are rejected by AWS.
        String sessionToken = text(body, "Token");
        if (sessionToken == null) {
            throw new AwsCredentialsException("Container credential endpoint " + host
                    + " returned a response without a Token; container role credentials are always temporary");
        }
        return new AwsCredentials(accessKeyId, secretAccessKey, sessionToken, expiration(body, host));
    }

    private static Instant expiration(JsonNode body, String host) throws AwsCredentialsException {
        String expiration = text(body, "Expiration");
        if (expiration == null) {
            // These credentials always expire, so a document without an Expiration is incomplete
            // rather than perpetual — treating it as perpetual would cache it past its life.
            throw new AwsCredentialsException("Container credential endpoint " + host
                    + " returned a response without an Expiration");
        }
        try {
            return Instant.parse(expiration);
        } catch (DateTimeParseException e) {
            // Not "treat it as non-expiring": that would turn one unreadable timestamp into
            // credentials the chain caches forever and keeps signing with long after AWS stopped
            // honouring them.
            throw new AwsCredentialsException("Container credential endpoint " + host
                    + " returned an Expiration that is not an ISO-8601 instant: " + expiration, e);
        }
    }

    /** {@code null} for a field that is absent, null, non-textual or blank — all of them "not supplied". */
    private static String text(JsonNode body, String field) {
        JsonNode value = body.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
}
