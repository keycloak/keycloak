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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.keycloak.email.aws.AwsHttpTransport;
import org.keycloak.email.aws.FakeTransport;
import org.keycloak.email.aws.TestEnvironment;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The chain's own behaviour — precedence, failure propagation and caching — pinned down against
 * hand-written sources, so none of it depends on which real source happens to be reachable from the
 * machine running the build.
 */
class AwsCredentialsProviderChainTest {

    private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");
    private static final AwsCredentials LONG_LIVED =
            AwsCredentials.of("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

    private final FakeTransport noNetwork = new FakeTransport();

    @Test
    void takesTheFirstConfiguredSourceAndStopsThere() throws Exception {
        StubSource unconfigured = StubSource.unconfigured("first source");
        StubSource configured = new StubSource("second source", LONG_LIVED);
        StubSource neverReached = new StubSource("third source", AwsCredentials.of("AKIAOTHER", "other"));
        AwsCredentialsProviderChain chain =
                new AwsCredentialsProviderChain(List.of(unconfigured, configured, neverReached));

        AwsCredentials resolved = chain.resolve(noNetwork, noNetwork, NOW);

        assertThat(resolved, is(sameInstance(LONG_LIVED)));
        assertThat(unconfigured.resolveCount(), is(1));
        assertThat(neverReached.resolveCount(), is(0));
    }

    /**
     * A source that is configured but broken is an operator error that must surface as itself. If the
     * chain swallowed it and carried on, a typo in the profile file would be reported as "no AWS
     * credentials found" — or, worse, silently replaced by the instance role's identity.
     */
    @Test
    void stopsAtTheFirstBrokenSource() {
        BrokenSource broken = new BrokenSource("broken source");
        StubSource neverReached = new StubSource("later source", LONG_LIVED);
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(List.of(broken, neverReached));

        AwsCredentialsException failure =
                assertThrows(AwsCredentialsException.class, () -> chain.resolve(noNetwork, noNetwork, NOW));

        assertThat(failure.getMessage(), containsString("broken source"));
        assertThat(neverReached.resolveCount(), is(0));
    }

    @Test
    void listsEverySourceItTriedWhenNoneIsConfigured() {
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(
                List.of(StubSource.unconfigured("first source"), StubSource.unconfigured("second source")));

        AwsCredentialsException failure =
                assertThrows(AwsCredentialsException.class, () -> chain.resolve(noNetwork, noNetwork, NOW));

        assertThat(failure.getMessage(), containsString("No AWS credentials found"));
        assertThat(failure.getMessage(), containsString("first source"));
        assertThat(failure.getMessage(), containsString("second source"));
    }

    @Test
    void resolvesCredentialsWithoutAnExpiryOnlyOnce() throws Exception {
        StubSource source = new StubSource("static source", LONG_LIVED);
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(List.of(source));

        chain.resolve(noNetwork, noNetwork, NOW);
        AwsCredentials second = chain.resolve(noNetwork, noNetwork, NOW.plus(Duration.ofDays(30)));

        assertThat(second, is(sameInstance(LONG_LIVED)));
        assertThat(source.resolveCount(), is(1));
    }

    @Test
    void invalidateDropsTheCache() throws Exception {
        StubSource source = new StubSource("static source", LONG_LIVED);
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(List.of(source));

        chain.resolve(noNetwork, noNetwork, NOW);
        chain.invalidate();
        chain.resolve(noNetwork, noNetwork, NOW);

        assertThat(source.resolveCount(), is(2));
    }

    /**
     * Two minutes of validity left is inside the {@value AwsCredentialsProviderChain#REFRESH_WINDOW_MINUTES}
     * minute window: the credentials are still valid now, but a message signed with them may not be by
     * the time SES checks the signature, so the chain must fetch a fresh pair.
     */
    @Test
    void refreshesCredentialsThatExpireInsideTheRefreshWindow() throws Exception {
        StubSource source = new StubSource("temporary source", temporary(NOW.plus(Duration.ofMinutes(2))));
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(List.of(source));

        chain.resolve(noNetwork, noNetwork, NOW);
        chain.resolve(noNetwork, noNetwork, NOW);

        assertThat(source.resolveCount(), is(2));
    }

    @Test
    void keepsCredentialsThatExpireAfterTheRefreshWindow() throws Exception {
        StubSource source = new StubSource("temporary source", temporary(NOW.plus(Duration.ofMinutes(30))));
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(List.of(source));

        chain.resolve(noNetwork, noNetwork, NOW);
        chain.resolve(noNetwork, noNetwork, NOW);

        assertThat(source.resolveCount(), is(1));
    }

    /**
     * Temporary credentials with no stated expiry — a session token pasted into a profile file or
     * exported into the environment — must not be cached. Nothing here can notice when they stop
     * working, so caching them would keep every email failing even after an operator refreshed the
     * file, until someone restarted the server.
     */
    @Test
    void doesNotCacheTemporaryCredentialsWithNoKnownExpiry() throws Exception {
        StubSource source = new StubSource("profile file",
                new AwsCredentials("ASIAEXAMPLE", "secret", "session-token", null));
        AwsCredentialsProviderChain chain = new AwsCredentialsProviderChain(List.of(source));

        chain.resolve(noNetwork, noNetwork, NOW);
        chain.resolve(noNetwork, noNetwork, NOW);

        assertThat(source.resolveCount(), is(2));
    }

    /**
     * The sources that answer on this machine — the container endpoint and the metadata service —
     * are reached through the proxy-free transport, and the ones that talk to AWS through the
     * server's own client. Sending a bearer token to the first group through a configured proxy
     * would hand it to the proxy, and would make the address checks in front of those calls
     * meaningless, since the host being validated would no longer be the host being spoken to.
     */
    @Test
    void routesLocalCredentialSourcesThroughTheDirectTransport() throws Exception {
        FakeTransport shared = new FakeTransport();
        FakeTransport direct = new FakeTransport();
        RecordingSource local = new RecordingSource("container", true);
        RecordingSource remote = new RecordingSource("web identity", false);

        new AwsCredentialsProviderChain(List.of(local)).resolve(shared, direct, NOW);
        new AwsCredentialsProviderChain(List.of(remote)).resolve(shared, direct, NOW);

        assertThat(local.transportSeen, is(sameInstance(direct)));
        assertThat(remote.transportSeen, is(sameInstance(shared)));
    }

    /** Records which transport the chain handed it, and answers with fixed credentials. */
    private static final class RecordingSource implements AwsCredentialsProvider {

        private final String name;
        private final boolean direct;

        private AwsHttpTransport transportSeen;

        private RecordingSource(String name, boolean direct) {
            this.name = name;
            this.direct = direct;
        }

        @Override
        public AwsCredentials resolve(AwsHttpTransport transport) {
            transportSeen = transport;
            return AwsCredentials.of("AKIAEXAMPLE", "secret");
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean requiresDirectConnection() {
            return direct;
        }
    }

    /**
     * On a machine with no AWS configuration at all the default chain ends where every AWS SDK ends:
     * probing the instance metadata service, because "am I running on EC2?" cannot be answered any
     * other way. That probe is expected to fail here, and failing is the point — the chain must then
     * report that it found nothing, rather than surfacing the metadata service's own error.
     */
    @Test
    void defaultChainOnAnEmptyEnvironmentProbesInstanceMetadataAndThenGivesUp() {
        FakeTransport transport = new FakeTransport().failWith(new IOException("no route to 169.254.169.254"));
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.defaultChain(TestEnvironment.empty());

        AwsCredentialsException failure =
                assertThrows(AwsCredentialsException.class, () -> chain.resolve(transport, transport, NOW));

        assertThat(failure.getMessage(), containsString("No AWS credentials found"));
        assertThat(failure.getMessage(), containsString("environment variables"));
        assertThat(transport.requestCount(), is(1));
    }

    /**
     * The posture for a server that is not on AWS at all — this product ships on plain docker compose
     * — where the metadata probe is a guaranteed-useless round trip on the path of every email.
     */
    @Test
    void skipsTheNetworkEntirelyWhenInstanceMetadataIsDisabled() {
        AwsCredentialsProviderChain chain = AwsCredentialsProviderChain.defaultChain(
                TestEnvironment.empty().with("AWS_EC2_METADATA_DISABLED", "true"));

        AwsCredentialsException failure =
                assertThrows(AwsCredentialsException.class, () -> chain.resolve(noNetwork, noNetwork, NOW));

        assertThat(failure.getMessage(), containsString("No AWS credentials found"));
        assertThat(noNetwork.requestCount(), is(0));
    }

    /**
     * The chain logs the source and the access key id of whatever it resolved, and credentials travel
     * on from here into exception messages. A record's generated {@code toString()} would print the
     * secret access key in all of them.
     */
    @Test
    void credentialsDoNotPrintTheSecret() {
        AwsCredentials credentials = temporary(NOW.plus(Duration.ofMinutes(30)));

        assertThat(credentials.toString(), not(containsString(credentials.secretAccessKey())));
        assertThat(credentials.toString(), not(containsString(credentials.sessionToken())));
        assertThat(credentials.toString(), containsString(credentials.accessKeyId()));
    }

    private static AwsCredentials temporary(Instant expiration) {
        return new AwsCredentials("ASIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "FwoGZXIvYXdzEExampleToken", expiration);
    }

    /** A source that always answers the same thing and counts how often it was asked. */
    private static final class StubSource implements AwsCredentialsProvider {

        private final String name;
        private final AwsCredentials credentials;

        private int resolveCount;

        StubSource(String name, AwsCredentials credentials) {
            this.name = name;
            this.credentials = credentials;
        }

        static StubSource unconfigured(String name) {
            return new StubSource(name, null);
        }

        @Override
        public AwsCredentials resolve(AwsHttpTransport transport) {
            resolveCount++;
            return credentials;
        }

        @Override
        public String name() {
            return name;
        }

        int resolveCount() {
            return resolveCount;
        }
    }

    /** A source that is configured but cannot produce credentials. */
    private static final class BrokenSource implements AwsCredentialsProvider {

        private final String name;

        BrokenSource(String name) {
            this.name = name;
        }

        @Override
        public AwsCredentials resolve(AwsHttpTransport transport) throws AwsCredentialsException {
            throw new AwsCredentialsException(name + " is configured but unreadable");
        }

        @Override
        public String name() {
            return name;
        }
    }
}
