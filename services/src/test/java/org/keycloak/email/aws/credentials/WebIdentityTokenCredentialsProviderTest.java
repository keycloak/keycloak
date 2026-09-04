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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.keycloak.email.aws.AwsHttpRequest;
import org.keycloak.email.aws.FakeTransport;
import org.keycloak.email.aws.TestEnvironment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Behaviour of the EKS IRSA credential source. The STS exchange is asserted at the byte level — the
 * request that would reach the wire — because the failures this flow produces in production
 * ({@code InvalidIdentityToken}, {@code SignatureDoesNotMatch}) name none of their real causes.
 */
class WebIdentityTokenCredentialsProviderTest {

    private static final String TOKEN_FILE_VARIABLE = "AWS_WEB_IDENTITY_TOKEN_FILE";
    private static final String ROLE_ARN_VARIABLE = "AWS_ROLE_ARN";
    private static final String SESSION_NAME_VARIABLE = "AWS_ROLE_SESSION_NAME";

    private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/keycloak-ses";
    /**
     * Base64url characters only — the alphabet a real projected service-account token uses — so form
     * encoding leaves it untouched and the assertion on the request body pins the exact bytes STS
     * would receive instead of restating what the encoder does.
     * <p>
     * Deliberately not shaped as three dot-separated JWT segments: the provider treats this value as
     * opaque, so the realism buys no coverage, while a JWT-shaped literal is flagged by secret
     * scanners and would fail the repository's own gitleaks gate.
     */
    private static final String TOKEN = "projected-service-account-token-for-tests_0123456789-abcdefg";
    private static final String EXPIRATION = "2026-09-03T12:34:56Z";

    @TempDir
    Path tempDir;

    @Test
    void returnsNullWhenTheTokenFileVariableIsNotSet() throws Exception {
        FakeTransport transport = new FakeTransport();

        AwsCredentials credentials = new WebIdentityTokenCredentialsProvider(
                TestEnvironment.empty().with(ROLE_ARN_VARIABLE, ROLE_ARN)).resolve(transport);

        assertThat(credentials, is(nullValue()));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void returnsNullWhenTheRoleArnIsNotSet() throws Exception {
        FakeTransport transport = new FakeTransport();

        AwsCredentials credentials = new WebIdentityTokenCredentialsProvider(
                TestEnvironment.empty().with(TOKEN_FILE_VARIABLE, writeTokenFile(TOKEN + "\n"))).resolve(transport);

        assertThat(credentials, is(nullValue()));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void exchangesTheProjectedTokenForTemporaryCredentials() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(EXPIRATION));

        AwsCredentials credentials = new WebIdentityTokenCredentialsProvider(
                configured().with("AWS_REGION", "eu-south-1")).resolve(transport);

        AwsHttpRequest request = transport.lastRequest();
        assertThat(request.method(), is("POST"));
        assertThat(request.uri().toString(), is("https://sts.eu-south-1.amazonaws.com/"));
        assertThat(request.headers().get("Content-Type"), containsString("application/x-www-form-urlencoded"));
        assertThat("the STS exchange must go out unsigned, the token is the credential",
                request.headers().keySet().stream().anyMatch(name -> name.equalsIgnoreCase("Authorization")), is(false));

        String body = request.bodyAsString();
        assertThat(body, containsString("Action=AssumeRoleWithWebIdentity&Version=2011-06-15"));
        assertThat(body, containsString("RoleArn=arn%3Aaws%3Aiam%3A%3A123456789012%3Arole%2Fkeycloak-ses"));
        assertThat(body, containsString("RoleSessionName=keycloak-email-aws-ses"));
        assertThat(body, containsString("WebIdentityToken=" + TOKEN));
        assertThat("the newline the kubelet leaves in the file must not reach STS", body, not(containsString("%0A")));

        assertThat(credentials.accessKeyId(), is("ASIAEXAMPLEKEYID"));
        assertThat(credentials.secretAccessKey(), is("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));
        assertThat(credentials.sessionToken(), is("FwoGZXIvYXdzEExampleSessionToken"));
        assertThat(credentials.isTemporary(), is(true));
        assertThat(credentials.expiration(), is(Instant.parse(EXPIRATION)));
    }

    @Test
    void formEncodesACustomSessionName() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(EXPIRATION));

        new WebIdentityTokenCredentialsProvider(
                configured().with(SESSION_NAME_VARIABLE, "keycloak@node-1")).resolve(transport);

        assertThat(transport.lastRequest().bodyAsString(), containsString("RoleSessionName=keycloak%40node-1"));
    }

    @Test
    void fallsBackToAwsDefaultRegionForTheEndpoint() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(EXPIRATION));

        new WebIdentityTokenCredentialsProvider(
                configured().with("AWS_DEFAULT_REGION", "us-west-2")).resolve(transport);

        assertThat(transport.lastRequest().uri().toString(), is("https://sts.us-west-2.amazonaws.com/"));
    }

    @Test
    void usesTheGlobalEndpointWhenNoRegionIsSet() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(EXPIRATION));

        new WebIdentityTokenCredentialsProvider(configured()).resolve(transport);

        assertThat(transport.lastRequest().uri().toString(), is("https://sts.amazonaws.com/"));
    }

    @Test
    void reportsTheStatusAndErrorCodeWithoutEchoingTheToken() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(403, """
                <ErrorResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
                  <Error>
                    <Type>Sender</Type>
                    <Code>AccessDenied</Code>
                    <Message>Not authorized to perform sts:AssumeRoleWithWebIdentity: %s</Message>
                  </Error>
                </ErrorResponse>
                """.formatted(TOKEN));
        AwsCredentialsProvider provider = new WebIdentityTokenCredentialsProvider(configured());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), containsString("403"));
        assertThat(failure.getMessage(), containsString("AccessDenied"));
        assertThat(failure.getMessage(), not(containsString(TOKEN)));
    }

    /**
     * The reason the parser is hardened. With a {@code DocumentBuilderFactory} at its defaults this
     * response resolves the entity and hands back credentials whose access key id is the content of a
     * local file, so removing any of the hardening lines fails this test as "expected exception not
     * thrown" rather than degrading quietly.
     */
    @Test
    void refusesAnXxePayloadInsteadOfResolvingTheEntity() throws Exception {
        Path secret = tempDir.resolve("xxe-target.txt");
        Files.writeString(secret, "TOP-SECRET-FILE-CONTENT");
        FakeTransport transport = new FakeTransport().respondWith(200, """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE AssumeRoleWithWebIdentityResponse [<!ENTITY xxe SYSTEM "%s">]>
                <AssumeRoleWithWebIdentityResponse>
                  <AssumeRoleWithWebIdentityResult>
                    <Credentials>
                      <AccessKeyId>&xxe;</AccessKeyId>
                      <SecretAccessKey>&xxe;</SecretAccessKey>
                      <SessionToken>&xxe;</SessionToken>
                      <Expiration>%s</Expiration>
                    </Credentials>
                  </AssumeRoleWithWebIdentityResult>
                </AssumeRoleWithWebIdentityResponse>
                """.formatted(secret.toUri(), EXPIRATION));
        AwsCredentialsProvider provider = new WebIdentityTokenCredentialsProvider(configured());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), not(containsString("TOP-SECRET-FILE-CONTENT")));
    }

    @Test
    void rejectsAnExpirationThatIsNotAnInstant() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse("very soon"));
        AwsCredentialsProvider provider = new WebIdentityTokenCredentialsProvider(configured());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), containsString("Expiration"));
    }

    @Test
    void rejectsAResponseWithoutAnExpiration() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(null));
        AwsCredentialsProvider provider = new WebIdentityTokenCredentialsProvider(configured());

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), containsString("Expiration"));
    }

    @Test
    void failsWhenTheTokenFileIsNotOnDisk() {
        FakeTransport transport = new FakeTransport();
        AwsCredentialsProvider provider = new WebIdentityTokenCredentialsProvider(TestEnvironment.empty()
                .with(TOKEN_FILE_VARIABLE, tempDir.resolve("never-projected").toString())
                .with(ROLE_ARN_VARIABLE, ROLE_ARN));

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), containsString(TOKEN_FILE_VARIABLE));
        assertThat("a broken source must not be papered over with an STS call", transport.requestCount(), is(0));
    }

    @Test
    void failsWhenTheTokenFileIsEmpty() throws Exception {
        FakeTransport transport = new FakeTransport();
        AwsCredentialsProvider provider = new WebIdentityTokenCredentialsProvider(TestEnvironment.empty()
                .with(TOKEN_FILE_VARIABLE, writeTokenFile("\n"))
                .with(ROLE_ARN_VARIABLE, ROLE_ARN));

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class, () -> provider.resolve(transport));

        assertThat(failure.getMessage(), containsString("empty"));
        assertThat(transport.requestCount(), is(0));
    }

    /**
     * The region is interpolated into the hostname of a request whose body carries the projected
     * service-account token. Without validation, a region of {@code attacker.example/collect} builds
     * {@code https://sts.attacker.example/collect.amazonaws.com/} and posts the token there.
     */
    @Test
    void refusesARegionThatWouldRedirectTheTokenToAnotherHost() throws Exception {
        FakeTransport transport = new FakeTransport();
        TestEnvironment environment = configured().with("AWS_REGION", "attacker.example/collect");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new WebIdentityTokenCredentialsProvider(environment).resolve(transport));

        assertThat(failure.getMessage(), containsString("not a valid AWS region name"));
        assertThat(transport.requestCount(), is(0));
    }

    /** STS answers on a different suffix in the China partition, so the region decides the host. */
    @Test
    void usesThePartitionSuffixForTheRegionalStsEndpoint() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(EXPIRATION));

        new WebIdentityTokenCredentialsProvider(configured().with("AWS_REGION", "cn-north-1")).resolve(transport);

        assertThat(transport.lastRequest().uri().toString(), is("https://sts.cn-north-1.amazonaws.com.cn/"));
    }

    /**
     * The AWS SDKs accept these three as JVM system properties before their environment equivalents.
     * A deployment that sets them the documented way would otherwise skip web identity entirely and
     * fall through to a later, wrong source.
     */
    @Test
    void readsTheWebIdentitySettingsFromSystemPropertiesToo() throws Exception {
        FakeTransport transport = new FakeTransport().respondWith(200, credentialsResponse(EXPIRATION));
        TestEnvironment environment = TestEnvironment.empty()
                .withProperty("aws.webIdentityTokenFile", writeTokenFile(TOKEN + "\n"))
                .withProperty("aws.roleArn", ROLE_ARN)
                .withProperty("aws.roleSessionName", "from-a-system-property");

        AwsCredentials credentials = new WebIdentityTokenCredentialsProvider(environment).resolve(transport);

        assertThat(credentials.isTemporary(), is(true));
        assertThat(transport.lastRequest().bodyAsString(), containsString("RoleSessionName=from-a-system-property"));
    }

    private TestEnvironment configured() throws IOException {
        return TestEnvironment.empty()
                .with(TOKEN_FILE_VARIABLE, writeTokenFile(TOKEN + "\n"))
                .with(ROLE_ARN_VARIABLE, ROLE_ARN);
    }

    private String writeTokenFile(String content) throws IOException {
        Path file = tempDir.resolve("web-identity-token");
        Files.writeString(file, content);
        return file.toString();
    }

    /** @param expiration the {@code Expiration} value, or {@code null} to leave the element out */
    private static String credentialsResponse(String expiration) {
        return """
                <AssumeRoleWithWebIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
                  <AssumeRoleWithWebIdentityResult>
                    <Credentials>
                      <AccessKeyId>ASIAEXAMPLEKEYID</AccessKeyId>
                      <SecretAccessKey>wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY</SecretAccessKey>
                      <SessionToken>FwoGZXIvYXdzEExampleSessionToken</SessionToken>
                      %s
                    </Credentials>
                  </AssumeRoleWithWebIdentityResult>
                </AssumeRoleWithWebIdentityResponse>
                """.formatted(expiration == null ? "" : "<Expiration>" + expiration + "</Expiration>");
    }
}
