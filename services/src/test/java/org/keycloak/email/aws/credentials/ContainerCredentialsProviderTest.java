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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.keycloak.email.aws.FakeTransport;
import org.keycloak.email.aws.TestEnvironment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerCredentialsProviderTest {

    private static final String RELATIVE_URI_VARIABLE = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";
    private static final String FULL_URI_VARIABLE = "AWS_CONTAINER_CREDENTIALS_FULL_URI";
    private static final String TOKEN_VARIABLE = "AWS_CONTAINER_AUTHORIZATION_TOKEN";
    private static final String TOKEN_FILE_VARIABLE = "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE";

    private static final String CREDENTIALS_PATH = "/v2/credentials/7f3d9c";

    private static final String CREDENTIALS_JSON = """
            {
              "AccessKeyId": "ASIACONTAINEREXAMPLE",
              "SecretAccessKey": "container-secret",
              "Token": "container-session-token",
              "Expiration": "2026-09-03T12:34:56Z"
            }""";

    /** Held apart from the header value below so that a leak of the secret half alone still fails. */
    private static final String TOKEN_SECRET = "container-authorization-token-value";
    private static final String AUTHORIZATION_TOKEN = "Bearer " + TOKEN_SECRET;

    private final FakeTransport transport = new FakeTransport();

    @Test
    void returnsNullWhenNoContainerVariableIsSet() throws Exception {
        AwsCredentials credentials = new ContainerCredentialsProvider(TestEnvironment.empty()).resolve(transport);

        assertThat(credentials, is(nullValue()));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void resolvesARelativeUriAgainstTheEcsEndpoint() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        AwsCredentials credentials = new ContainerCredentialsProvider(withRelativeUri()).resolve(transport);

        assertThat(credentials.accessKeyId(), is("ASIACONTAINEREXAMPLE"));
        assertThat(transport.lastRequest().method(), is("GET"));
        assertThat(transport.lastRequest().uri().toString(), is("http://169.254.170.2/v2/credentials/7f3d9c"));
        assertThat(transport.lastRequest().connectTimeoutMillis(), is(1000));
        assertThat(transport.lastRequest().readTimeoutMillis(), is(1000));
    }

    @Test
    void joinsARelativeUriThatHasNoLeadingSlash() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);
        TestEnvironment environment = TestEnvironment.empty().with(RELATIVE_URI_VARIABLE, "v2/credentials/7f3d9c");

        new ContainerCredentialsProvider(environment).resolve(transport);

        assertThat(transport.lastRequest().uri().toString(), is("http://169.254.170.2/v2/credentials/7f3d9c"));
    }

    @Test
    void acceptsAFullUriOverHttps() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        AwsCredentials credentials =
                new ContainerCredentialsProvider(withFullUri("https://pod-identity.example.com/creds")).resolve(transport);

        assertThat(credentials.accessKeyId(), is("ASIACONTAINEREXAMPLE"));
        assertThat(transport.lastRequest().uri().toString(), is("https://pod-identity.example.com/creds"));
    }

    @Test
    void acceptsAFullUriOnLoopbackOverHttp() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        new ContainerCredentialsProvider(withFullUri("http://localhost:8080/creds")).resolve(transport);

        assertThat(transport.lastRequest().uri().toString(), is("http://localhost:8080/creds"));
    }

    /**
     * The three addresses AWS's own agents listen on, all of them plain HTTP: the ECS task metadata
     * endpoint and the EKS Pod Identity agent in both of its forms. Missing one of them refuses
     * credentials the platform is offering, and the chain then falls through to "no credentials
     * found" on a pod that has a perfectly good role.
     * <p>
     * The IPv6 case is the one that breaks in production rather than in review: {@link java.net.URI}
     * keeps the brackets of a literal, so {@code getHost()} returns {@code [fd00:ec2::23]} while the
     * allow list is written bare — an implementation that compared the two directly would reject
     * every EKS Pod Identity pod configured over IPv6, and only those.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.170.2/v2/credentials/7f3d9c",
            "http://169.254.170.23/v1/credentials",
            "http://[fd00:ec2::23]/v1/credentials"})
    void acceptsAFullUriOnAnAwsCredentialEndpointOverHttp(String endpoint) throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        AwsCredentials credentials = new ContainerCredentialsProvider(withFullUri(endpoint)).resolve(transport);

        assertThat(credentials.accessKeyId(), is("ASIACONTAINEREXAMPLE"));
        assertThat(transport.lastRequest().uri().toString(), is(endpoint));
    }

    @Test
    void refusesAFullUriOnAnArbitraryHost() {
        assertRefuses("http://evil.example.com/creds", "evil.example.com");
    }

    @Test
    void refusesAFullUriOnTheInstanceMetadataService() {
        assertRefuses("http://169.254.169.254/latest/meta-data/iam/security-credentials/", "169.254.169.254");
    }

    /**
     * The first gate of the SSRF guard. A bare path in {@code AWS_CONTAINER_CREDENTIALS_FULL_URI} —
     * the shape an operator produces by copying the value of the <em>relative</em> variable into the
     * full one — has no scheme and no host to validate, so there is nothing to decide the request is
     * safe on, and it must be refused rather than resolved against something.
     */
    @Test
    void refusesAFullUriThatIsNotAbsolute() {
        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(withFullUri("/v2/credentials/7f3d9c")).resolve(transport));

        assertThat(failure.getMessage(), containsString(FULL_URI_VARIABLE));
        // The message has to name what is wrong with it: falling through to the "points at <host>"
        // rejection below would report the host as "null", which describes nothing an operator set.
        assertThat(failure.getMessage(), containsString("must be an absolute http or https URI"));
        assertThat(transport.requestCount(), is(0));
    }

    /**
     * A full URI is operator-supplied and may carry credentials of its own in a query string, so the
     * rejection names the variable and never quotes its value: this message goes to the server log,
     * where the whole point of the guard is that nothing secret arrives.
     */
    @Test
    void namesTheVariableWithoutQuotingItWhenTheFullUriCannotBeParsed() {
        String malformed = "http://pod identity.example.com/creds?token=" + TOKEN_SECRET;

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(withFullUri(malformed)).resolve(transport));

        assertThat(failure.getMessage(), containsString(FULL_URI_VARIABLE));
        assertThat(failure.getMessage(), not(containsString(TOKEN_SECRET)));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void sendsTheAuthorizationTokenFromTheEnvironment() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);
        TestEnvironment environment = withRelativeUri().with(TOKEN_VARIABLE, AUTHORIZATION_TOKEN);

        new ContainerCredentialsProvider(environment).resolve(transport);

        assertThat(transport.lastRequest().headers().get("Authorization"), is(AUTHORIZATION_TOKEN));
    }

    @Test
    void sendsTheAuthorizationTokenFromTheTokenFile(@TempDir Path directory) throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);
        Path tokenFile = Files.writeString(directory.resolve("token"), AUTHORIZATION_TOKEN + "\n", StandardCharsets.UTF_8);
        TestEnvironment environment = withRelativeUri().with(TOKEN_FILE_VARIABLE, tokenFile.toString());

        new ContainerCredentialsProvider(environment).resolve(transport);

        assertThat(transport.lastRequest().headers().get("Authorization"), is(AUTHORIZATION_TOKEN));
    }

    /**
     * The file wins over the variable, which is the difference between a pod that keeps working and
     * one that stops. EKS Pod Identity rotates the token on disk while the environment keeps the copy
     * the process was started with: preferring the variable would sign credential fetches with a
     * value that was valid at boot and is stale hours later, and the failure — an HTTP 401 from the
     * agent — says nothing about which of the two was used.
     */
    @Test
    void prefersTheRotatingTokenFileOverTheTokenCapturedInTheEnvironment(@TempDir Path directory) throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);
        String rotated = "Bearer container-authorization-token-after-rotation";
        Path tokenFile = Files.writeString(directory.resolve("token"), rotated + "\n", StandardCharsets.UTF_8);
        TestEnvironment environment = withRelativeUri()
                .with(TOKEN_VARIABLE, AUTHORIZATION_TOKEN)
                .with(TOKEN_FILE_VARIABLE, tokenFile.toString());

        new ContainerCredentialsProvider(environment).resolve(transport);

        assertThat(transport.lastRequest().headers().get("Authorization"), is(rotated));
    }

    /**
     * An empty token file is what a read caught mid-rotation looks like. Sending the empty
     * {@code Authorization} header it would produce turns a retryable local race into an
     * indistinguishable 401 from the agent; failing here names the file instead.
     */
    @Test
    void failsWhenTheTokenFileIsEmpty(@TempDir Path directory) throws Exception {
        Path tokenFile = Files.writeString(directory.resolve("token"), "\n", StandardCharsets.UTF_8);
        TestEnvironment environment = withRelativeUri().with(TOKEN_FILE_VARIABLE, tokenFile.toString());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(environment).resolve(transport));

        assertThat(failure.getMessage(), containsString(TOKEN_FILE_VARIABLE));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void sendsNoAuthorizationHeaderWhenNeitherTokenVariableIsSet() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        new ContainerCredentialsProvider(withRelativeUri()).resolve(transport);

        assertThat(transport.lastRequest().headers().containsKey("Authorization"), is(false));
    }

    @Test
    void failsWhenTheTokenFileCannotBeRead(@TempDir Path directory) {
        TestEnvironment environment = withRelativeUri().with(TOKEN_FILE_VARIABLE, directory.resolve("absent").toString());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(environment).resolve(transport));

        assertThat(failure.getMessage(), containsString(TOKEN_FILE_VARIABLE));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void mapsTheTokenFieldToTheSessionTokenAndParsesTheExpiration() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        AwsCredentials credentials = new ContainerCredentialsProvider(withRelativeUri()).resolve(transport);

        assertThat(credentials.secretAccessKey(), is("container-secret"));
        assertThat(credentials.sessionToken(), is("container-session-token"));
        assertThat(credentials.isTemporary(), is(true));
        assertThat(credentials.expiration(), is(Instant.parse("2026-09-03T12:34:56Z")));
    }

    @Test
    void failsOnAnErrorStatusWithoutRevealingTheAuthorizationToken() {
        transport.respondWith(500, "{\"message\":\"" + AUTHORIZATION_TOKEN + "\"}");
        TestEnvironment environment = withRelativeUri().with(TOKEN_VARIABLE, AUTHORIZATION_TOKEN);

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(environment).resolve(transport));

        assertThat(failure.getMessage(), containsString("500"));
        assertThat(failure.getMessage(), not(containsString(TOKEN_SECRET)));
    }

    @Test
    void failsOnAMalformedJsonResponse() {
        transport.respondWith(200, "{\"AccessKeyId\": ");

        assertThrows(AwsCredentialsException.class, this::resolveWithRelativeUri);
    }

    @Test
    void failsWhenTheResponseHasNoSecretAccessKey() {
        transport.respondWith(200, "{\"AccessKeyId\":\"ASIACONTAINEREXAMPLE\",\"Token\":\"container-session-token\"}");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, this::resolveWithRelativeUri);

        assertThat(failure.getMessage(), containsString("SecretAccessKey"));
    }

    @Test
    void failsOnAnExpirationThatIsNotAnInstant() {
        transport.respondWith(200, """
                {
                  "AccessKeyId": "ASIACONTAINEREXAMPLE",
                  "SecretAccessKey": "container-secret",
                  "Token": "container-session-token",
                  "Expiration": "2026-09-03 12:34:56"
                }""");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, this::resolveWithRelativeUri);

        assertThat(failure.getMessage(), containsString("Expiration"));
    }

    /**
     * Container role credentials are always temporary. A document without a {@code Token} would be
     * accepted as a long-lived key pair and every signed request would then be rejected by AWS, far
     * away from the endpoint that produced it.
     */
    @Test
    void refusesACredentialDocumentWithoutAToken() {
        transport.respondWith(200, """
                {
                  "AccessKeyId": "ASIACONTAINEREXAMPLE",
                  "SecretAccessKey": "container-secret",
                  "Expiration": "2026-09-03T12:34:56Z"
                }""");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, this::resolveWithRelativeUri);

        assertThat(failure.getMessage(), containsString("without a Token"));
    }

    /** Same reasoning for the expiry: absent is incomplete, not perpetual. */
    @Test
    void refusesACredentialDocumentWithoutAnExpiration() {
        transport.respondWith(200, """
                {
                  "AccessKeyId": "ASIACONTAINEREXAMPLE",
                  "SecretAccessKey": "container-secret",
                  "Token": "container-session-token"
                }""");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, this::resolveWithRelativeUri);

        assertThat(failure.getMessage(), containsString("without an Expiration"));
    }

    /**
     * {@code 127.999.999.999} is not an address, and the loopback check used to accept anything
     * shaped like a dotted quad. It never actually got that far — {@link java.net.URI} refuses an
     * authority that looks like an IPv4 address and is not one, so the endpoint is rejected before
     * the guard sees it — but "rejected by accident, one layer earlier" is not a property to rely on,
     * so both layers are pinned here.
     */
    @Test
    void refusesAnAddressShapedNameThatIsNotAnAddress() {
        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(withFullUri("http://127.999.999.999/v1/credentials"))
                        .resolve(transport));

        assertThat(failure.getMessage(), containsString(FULL_URI_VARIABLE));
        assertThat(transport.requestCount(), is(0));
    }

    /** The same shape written so that URI does parse it: the guard itself has to refuse it. */
    @Test
    void refusesALoopbackLookalikeThatUriAccepts() {
        assertRefuses("http://127.0.0.1.example.com/v1/credentials", "127.0.0.1.example.com");
    }

    /**
     * The allowed endpoints are compared as addresses, not as strings: an IPv6 address has many legal
     * spellings, and accepting only the one written in the allow list would refuse a perfectly
     * ordinary way of writing the EKS Pod Identity endpoint.
     */
    @Test
    void acceptsAnAllowedEndpointHoweverItsAddressIsSpelled() throws Exception {
        transport.respondWith(200, CREDENTIALS_JSON);

        AwsCredentials credentials = new ContainerCredentialsProvider(
                withFullUri("http://[fd00:0ec2:0000:0000:0000:0000:0000:0023]/v1/credentials")).resolve(transport);

        assertThat(credentials.accessKeyId(), is("ASIACONTAINEREXAMPLE"));
    }

    private static TestEnvironment withRelativeUri() {
        return TestEnvironment.empty().with(RELATIVE_URI_VARIABLE, CREDENTIALS_PATH);
    }

    private static TestEnvironment withFullUri(String uri) {
        return TestEnvironment.empty().with(FULL_URI_VARIABLE, uri);
    }

    private AwsCredentials resolveWithRelativeUri() throws AwsCredentialsException {
        return new ContainerCredentialsProvider(withRelativeUri()).resolve(transport);
    }

    /** A refused endpoint must fail before the exchange: the authorization token may not leave the process. */
    private void assertRefuses(String fullUri, String host) {
        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new ContainerCredentialsProvider(withFullUri(fullUri)).resolve(transport));

        assertThat(failure.getMessage(), containsString(host));
        assertThat(transport.requestCount(), is(0));
    }
}
