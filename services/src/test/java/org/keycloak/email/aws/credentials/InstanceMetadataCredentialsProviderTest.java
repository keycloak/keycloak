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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.time.Instant;

import org.keycloak.email.aws.AwsHttpRequest;
import org.keycloak.email.aws.FakeTransport;
import org.keycloak.email.aws.TestEnvironment;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The wire contract of the instance metadata service, pinned end to end: the exact URLs, the IMDSv2
 * token dance, and — the part that is easy to get wrong — which failures mean "this is not an EC2
 * instance, try the next source" and which mean "this is an EC2 instance and something is broken".
 */
class InstanceMetadataCredentialsProviderTest {

    /**
     * AWS documents exactly one IPv6 address for the metadata service. The guard used to accept the
     * whole {@code fd00:ec2::/32} range around it, so a configured endpoint anywhere in that range
     * could have supplied credentials over plain HTTP.
     */
    @Test
    void refusesAnotherAddressInTheMetadataServicesRange() {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_EC2_METADATA_SERVICE_ENDPOINT", "http://[fd00:ec2::dead]");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(environment).resolve(transport));

        assertThat(failure.getMessage(), containsString("fd00:ec2::dead"));
        assertThat(transport.requestCount(), is(0));
    }

    /** The documented address itself is accepted, in either spelling. */
    @Test
    void acceptsTheDocumentedIpv6MetadataAddress() throws Exception {
        transport.respondWith(200, METADATA_TOKEN).respondWith(200, ROLE).respondWith(200, CREDENTIALS_JSON);
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_EC2_METADATA_SERVICE_ENDPOINT", "http://[fd00:0ec2:0:0:0:0:0:0254]");

        assertThat(new InstanceMetadataCredentialsProvider(environment).resolve(transport).accessKeyId(),
                is("ASIAIOSFODNN7EXAMPLE"));
    }

    /**
     * Instance-profile credentials always expire, so a document without an {@code Expiration} is
     * incomplete rather than perpetual. Accepting it would let the chain cache the credentials for
     * the life of the process and keep signing with them long after AWS stopped honouring them.
     */
    @Test
    void refusesACredentialDocumentWithoutAnExpiration() {
        transport.respondWith(200, METADATA_TOKEN).respondWith(200, ROLE).respondWith(200, """
                {
                  "Code": "Success",
                  "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
                  "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                  "Token": "IQoJb3JpZ2luX2VjEXAMPLESESSIONTOKEN"
                }
                """);

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        assertThat(failure.getMessage(), containsString("without an Expiration"));
    }

    private static final String TOKEN_URL = "http://169.254.169.254/latest/api/token";
    private static final String ROLES_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/";
    private static final String METADATA_TOKEN = "AQAEAP1Nz1AbCdEfGhIjKlMnOpQrStUvWxYz0123456789==";
    private static final String ROLE = "keycloak-ses-role";

    private static final String CREDENTIALS_JSON = """
            {
              "Code": "Success",
              "LastUpdated": "2026-09-03T11:00:00Z",
              "Type": "AWS-HMAC",
              "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
              "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
              "Token": "IQoJb3JpZ2luX2VjEXAMPLESESSIONTOKEN",
              "Expiration": "2026-09-03T17:00:00Z"
            }
            """;

    private final FakeTransport transport = new FakeTransport();

    @Test
    void makesNoHttpCallAtAllWhenDisabledByEnvironmentVariable() throws Exception {
        TestEnvironment environment = TestEnvironment.empty().with("AWS_EC2_METADATA_DISABLED", "TRUE");

        assertThat(new InstanceMetadataCredentialsProvider(environment).resolve(transport), is(nullValue()));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void makesNoHttpCallAtAllWhenDisabledBySystemProperty() throws Exception {
        TestEnvironment environment = TestEnvironment.empty().withProperty("aws.disableEc2Metadata", "true");

        assertThat(new InstanceMetadataCredentialsProvider(environment).resolve(transport), is(nullValue()));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void resolvesTheCredentialsOfTheAttachedRole() throws Exception {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, "\n" + ROLE + "\nsecond-role\n")
                .respondWith(200, CREDENTIALS_JSON);

        AwsCredentials credentials = new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport);

        assertThat(credentials.accessKeyId(), is("ASIAIOSFODNN7EXAMPLE"));
        assertThat(credentials.secretAccessKey(), is("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));
        assertThat(credentials.sessionToken(), is("IQoJb3JpZ2luX2VjEXAMPLESESSIONTOKEN"));
        assertThat(credentials.expiration(), is(Instant.parse("2026-09-03T17:00:00Z")));
        assertThat(credentials.isTemporary(), is(true));

        assertThat(transport.requestCount(), is(3));

        AwsHttpRequest tokenRequest = transport.request(0);
        assertThat(tokenRequest.method(), is("PUT"));
        assertThat(tokenRequest.uri().toString(), is(TOKEN_URL));
        assertThat(tokenRequest.headers(), hasEntry("X-aws-ec2-metadata-token-ttl-seconds", "21600"));
        assertThat(tokenRequest.headers(), not(hasKey("X-aws-ec2-metadata-token")));
        assertThat(tokenRequest.connectTimeoutMillis(), is(1000));
        assertThat(tokenRequest.readTimeoutMillis(), is(1000));

        AwsHttpRequest roleListRequest = transport.request(1);
        assertThat(roleListRequest.method(), is("GET"));
        assertThat(roleListRequest.uri().toString(), is(ROLES_URL));
        assertThat(roleListRequest.headers(), hasEntry("X-aws-ec2-metadata-token", METADATA_TOKEN));

        AwsHttpRequest credentialsRequest = transport.request(2);
        assertThat(credentialsRequest.method(), is("GET"));
        assertThat(credentialsRequest.uri().toString(), is(ROLES_URL + ROLE));
        assertThat(credentialsRequest.headers(), hasEntry("X-aws-ec2-metadata-token", METADATA_TOKEN));
        assertThat(credentialsRequest.readTimeoutMillis(), is(1000));
    }

    @Test
    void fallsThroughWhenThereIsNoMetadataServiceToAnswerTheToken() throws Exception {
        transport.failWith(new ConnectException("Connection refused"));

        assertThat(new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport), is(nullValue()));
        assertThat(transport.requestCount(), is(1));
    }

    @Test
    void fallsThroughWhenTheTokenIsRefused() throws Exception {
        transport.respondWith(403, "");

        assertThat(new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport), is(nullValue()));
        assertThat(transport.requestCount(), is(1));
    }

    @Test
    void fallsThroughWhenNoRoleIsAttachedToTheInstance() throws Exception {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(404, "");

        assertThat(new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport), is(nullValue()));
        assertThat(transport.requestCount(), is(2));
    }

    @Test
    void failsWhenTheCredentialDocumentIsNotSuccessful() {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, ROLE)
                .respondWith(200, "{\"Code\":\"AssumeRoleUnauthorizedAccess\",\"Message\":\"instance profile\"}");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        assertThat(failure.getMessage(), containsString("AssumeRoleUnauthorizedAccess"));
        assertThat(failure.getMessage(), containsString(ROLE));
    }

    @Test
    void failsWhenTheCredentialDocumentIsIncomplete() {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, ROLE)
                .respondWith(200, "{\"Code\":\"Success\",\"AccessKeyId\":\"ASIAIOSFODNN7EXAMPLE\"}");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        assertThat(failure.getMessage(), containsString("SecretAccessKey"));
    }

    @Test
    void failsWhenTheExpirationCannotBeParsed() {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, ROLE)
                .respondWith(200, CREDENTIALS_JSON.replace("2026-09-03T17:00:00Z", "in about an hour"));

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        assertThat(failure.getMessage(), containsString("Expiration"));
    }

    @Test
    void urlEncodesTheRoleNameIntoThePath() throws Exception {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, "my role/../evil")
                .respondWith(200, CREDENTIALS_JSON);

        new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport);

        assertThat(transport.lastRequest().uri().toString(), is(ROLES_URL + "my%20role%2F..%2Fevil"));
    }

    @Test
    void failsWhenTheCredentialsCallIsRejected() {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, ROLE)
                .respondWith(500, "");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        assertThat(failure.getMessage(), containsString("500"));
        assertThat(failure.getMessage(), containsString(ROLE));
    }

    /**
     * The whole rendered exception, not just its message: the tempting way to write the failure of
     * an HTTP call is to interpolate the request, and {@link AwsHttpRequest} is a record whose
     * {@code toString} would print the metadata token header into the server log.
     */
    @Test
    void neverPutsTheMetadataTokenInAFailure() {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, ROLE)
                .failWith(new IOException("connection reset by peer"));

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        StringWriter rendered = new StringWriter();
        failure.printStackTrace(new PrintWriter(rendered));
        assertThat(rendered.toString(), not(containsString(METADATA_TOKEN)));
    }

    @Test
    void neverEchoesTheCredentialDocumentThatCouldNotBeParsed() {
        transport.respondWith(200, METADATA_TOKEN)
                .respondWith(200, ROLE)
                .respondWith(200, "{\"SecretAccessKey\": \"wJalrXUtnFEMI-not-json");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(TestEnvironment.empty()).resolve(transport));

        assertThat(failure.getMessage(), not(containsString("wJalrXUtnFEMI")));
        assertThat(failure.getCause(), is(nullValue()));
    }

    @Test
    void refusesAnEndpointThatIsNotTheMetadataServiceOverPlainHttp() {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_EC2_METADATA_SERVICE_ENDPOINT", "http://metadata.attacker.example");

        AwsCredentialsException failure = assertThrows(AwsCredentialsException.class,
                () -> new InstanceMetadataCredentialsProvider(environment).resolve(transport));

        assertThat(failure.getMessage(), containsString("AWS_EC2_METADATA_SERVICE_ENDPOINT"));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void honoursALoopbackEndpointForAMetadataProxy() throws Exception {
        TestEnvironment environment = TestEnvironment.empty()
                .with("AWS_EC2_METADATA_SERVICE_ENDPOINT", "http://127.0.0.1:8169/");
        transport.failWith(new IOException("nothing listening"));

        assertThat(new InstanceMetadataCredentialsProvider(environment).resolve(transport), is(nullValue()));
        assertThat(transport.lastRequest().uri().toString(), is("http://127.0.0.1:8169/latest/api/token"));
    }
}
