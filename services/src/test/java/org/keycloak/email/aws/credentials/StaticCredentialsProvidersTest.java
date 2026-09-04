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

import org.keycloak.email.aws.FakeTransport;
import org.keycloak.email.aws.TestEnvironment;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The two sources that read the process itself: JVM system properties and environment variables.
 * <p>
 * The transport handed to them has no queued response, so it fails the test loudly if either source
 * ever tries to reach the network — they must resolve without leaving the process.
 */
class StaticCredentialsProvidersTest {

    private final FakeTransport noNetwork = new FakeTransport();

    @Test
    void resolvesALongLivedPairFromSystemProperties() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .withProperty("aws.accessKeyId", "AKIAIOSFODNN7EXAMPLE")
                .withProperty("aws.secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        AwsCredentials credentials = new SystemPropertiesCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials, is(notNullValue()));
        assertThat(credentials.accessKeyId(), is("AKIAIOSFODNN7EXAMPLE"));
        assertThat(credentials.secretAccessKey(), is("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));
        assertThat(credentials.sessionToken(), is(nullValue()));
        assertThat(credentials.isTemporary(), is(false));
        assertThat(credentials.expiration(), is(nullValue()));
        assertThat(noNetwork.requestCount(), is(0));
    }

    @Test
    void carriesTheSessionTokenFromSystemProperties() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .withProperty("aws.accessKeyId", "ASIAIOSFODNN7EXAMPLE")
                .withProperty("aws.secretAccessKey", "secret")
                .withProperty("aws.sessionToken", "FwoGZXIvYXdzEExampleToken");

        AwsCredentials credentials = new SystemPropertiesCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials.sessionToken(), is("FwoGZXIvYXdzEExampleToken"));
        assertThat(credentials.isTemporary(), is(true));
        assertThat(credentials.expiration(), is(nullValue()));
    }

    @Test
    void returnsNullWhenNoSystemPropertyIsSet() throws Exception {
        AwsCredentials credentials =
                new SystemPropertiesCredentialsProvider(TestEnvironment.empty()).resolve(noNetwork);

        assertThat(credentials, is(nullValue()));
    }

    @Test
    void returnsNullWhenTheSystemPropertiesAreBlank() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .withProperty("aws.accessKeyId", "")
                .withProperty("aws.secretAccessKey", "   ");

        AwsCredentials credentials = new SystemPropertiesCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials, is(nullValue()));
    }

    @Test
    void failsWhenOnlyTheSystemPropertyAccessKeyIdIsSet() {
        TestEnvironment environment = TestEnvironment.empty()
                .withProperty("aws.accessKeyId", "AKIAIOSFODNN7EXAMPLE");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new SystemPropertiesCredentialsProvider(environment).resolve(noNetwork));

        assertThat(failure.getMessage(), containsString("aws.secretAccessKey"));
    }

    @Test
    void failsWhenOnlyTheSystemPropertySecretAccessKeyIsSet() {
        TestEnvironment environment = TestEnvironment.empty()
                .withProperty("aws.secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new SystemPropertiesCredentialsProvider(environment).resolve(noNetwork));

        assertThat(failure.getMessage(), containsString("aws.accessKeyId"));
        assertThat(failure.getMessage(), not(containsString("wJalrXUtnFEMI")));
    }

    @Test
    void resolvesALongLivedPairFromEnvironmentVariables() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE")
                .with("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        AwsCredentials credentials = new EnvironmentVariableCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials, is(notNullValue()));
        assertThat(credentials.accessKeyId(), is("AKIAIOSFODNN7EXAMPLE"));
        assertThat(credentials.secretAccessKey(), is("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));
        assertThat(credentials.sessionToken(), is(nullValue()));
        assertThat(credentials.isTemporary(), is(false));
        assertThat(credentials.expiration(), is(nullValue()));
        assertThat(noNetwork.requestCount(), is(0));
    }

    @Test
    void carriesTheSessionTokenFromEnvironmentVariables() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_ACCESS_KEY_ID", "ASIAIOSFODNN7EXAMPLE")
                .with("AWS_SECRET_ACCESS_KEY", "secret")
                .with("AWS_SESSION_TOKEN", "FwoGZXIvYXdzEExampleToken");

        AwsCredentials credentials = new EnvironmentVariableCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials.sessionToken(), is("FwoGZXIvYXdzEExampleToken"));
        assertThat(credentials.isTemporary(), is(true));
        assertThat(credentials.expiration(), is(nullValue()));
    }

    @Test
    void returnsNullWhenNoEnvironmentVariableIsSet() throws Exception {
        AwsCredentials credentials =
                new EnvironmentVariableCredentialsProvider(TestEnvironment.empty()).resolve(noNetwork);

        assertThat(credentials, is(nullValue()));
    }

    /**
     * The case that matters in practice: docker compose's {@code AWS_ACCESS_KEY_ID: ${VAR:-}} exports
     * the variable with an empty value instead of omitting it. Treating that as "configured" signs
     * every message with an empty key and turns a plain misconfiguration into an opaque SES
     * {@code SignatureDoesNotMatch}, three layers away from the cause.
     */
    @Test
    void returnsNullWhenTheEnvironmentVariablesAreBlank() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_ACCESS_KEY_ID", "")
                .with("AWS_SECRET_ACCESS_KEY", "   ");

        AwsCredentials credentials = new EnvironmentVariableCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials, is(nullValue()));
    }

    /** A blank session token is not a session token: it must not turn the pair temporary. */
    @Test
    void ignoresABlankSessionToken() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE")
                .with("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .with("AWS_SESSION_TOKEN", "");

        AwsCredentials credentials = new EnvironmentVariableCredentialsProvider(environment).resolve(noNetwork);

        assertThat(credentials.sessionToken(), is(nullValue()));
        assertThat(credentials.isTemporary(), is(false));
    }

    @Test
    void failsWhenOnlyTheEnvironmentVariableAccessKeyIdIsSet() {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_ACCESS_KEY_ID", "AKIAIOSFODNN7EXAMPLE");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new EnvironmentVariableCredentialsProvider(environment).resolve(noNetwork));

        assertThat(failure.getMessage(), containsString("AWS_SECRET_ACCESS_KEY"));
    }

    @Test
    void failsWhenOnlyTheEnvironmentVariableSecretAccessKeyIsSet() {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_SECRET_ACCESS_KEY", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new EnvironmentVariableCredentialsProvider(environment).resolve(noNetwork));

        assertThat(failure.getMessage(), containsString("AWS_ACCESS_KEY_ID"));
        assertThat(failure.getMessage(), not(containsString("wJalrXUtnFEMI")));
    }

    /** The names end up in the "no AWS credentials found" message an administrator has to act on. */
    @Test
    void namesTheSourcesTheWayAnOperatorConfiguresThem() {
        TestEnvironment environment = TestEnvironment.empty();

        assertThat(new SystemPropertiesCredentialsProvider(environment).name(), is("JVM system properties"));
        assertThat(new EnvironmentVariableCredentialsProvider(environment).name(), is("environment variables"));
    }
}
