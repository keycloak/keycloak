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

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.Config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The configuration read once at boot, and the only place where an operator's typo can still be
 * turned into a message instead of into a 403.
 * <p>
 * Two of these values are not merely settings: the region is interpolated into a hostname and into
 * the SigV4 credential scope, and the host header is signed. A region that is not a region, or a
 * host header that disagrees by a port with what the signature covered, produces an AWS rejection
 * that says nothing about its cause — so both are pinned here rather than discovered in production.
 * <p>
 * The {@link Config.Scope} is a real one: {@link Config.SystemPropertiesConfigProvider} backed by
 * JVM system properties, the same implementation Keycloak itself falls back to. A hand-written test
 * double would compile against today's {@code Config.Scope} and break upstream the next time the
 * interface gains a method.
 */
class AwsSesConfigTest {

    /** {@code Config.scope("emailSender", "aws-ses").get("x")} reads {@code <this>x}. */
    private static final String OPTION_PREFIX = "keycloak.emailSender.aws-ses.";

    private final Set<String> setProperties = new HashSet<>();

    @BeforeEach
    void useSystemPropertiesAsTheServerConfiguration() {
        Config.init(new Config.SystemPropertiesConfigProvider());
    }

    @AfterEach
    void clearOptions() {
        setProperties.forEach(System::clearProperty);
        setProperties.clear();
    }

    @Test
    void readsTheRegionFromTheSpiOption() {
        option("region", "eu-central-1");

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.region(), is("eu-central-1"));
    }

    @Test
    void fallsBackToAwsRegionWhenTheSpiOptionIsUnset() {
        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty().with("AWS_REGION", "us-east-2"));

        assertThat(config.region(), is("us-east-2"));
    }

    @Test
    void fallsBackToAwsDefaultRegionWhenAwsRegionIsUnset() {
        AwsSesConfig config =
                AwsSesConfig.from(options(), TestEnvironment.empty().with("AWS_DEFAULT_REGION", "ap-southeast-2"));

        assertThat(config.region(), is("ap-southeast-2"));
    }

    /**
     * The SPI option is the explicit instruction and outranks the ambient environment; between the
     * two variables, {@code AWS_REGION} wins, which is the order every other AWS tool on the box
     * uses. Getting this backwards sends mail from the wrong region, where the verified identity
     * does not exist, and the failure names neither variable.
     */
    @Test
    void prefersTheSpiOptionThenAwsRegionThenAwsDefaultRegion() {
        TestEnvironment bothVariables = TestEnvironment.empty()
                .with("AWS_REGION", "us-east-1")
                .with("AWS_DEFAULT_REGION", "ap-south-1");

        assertThat(AwsSesConfig.from(options(), bothVariables).region(), is("us-east-1"));

        option("region", "eu-central-1");
        assertThat(AwsSesConfig.from(options(), bothVariables).region(), is("eu-central-1"));
    }

    @Test
    void explainsWhatToSetWhenNoRegionIsConfiguredAnywhere() {
        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), containsString("--spi-email-sender-aws-ses-region"));
        assertThat(thrown.getMessage(), containsString("AWS_REGION"));
    }

    /**
     * The region ends up in {@code email.<region>.amazonaws.com} and in the credential scope, so a
     * value that is not a region is refused rather than concatenated: {@code eu-central-1.example.com}
     * would otherwise send a signed request, credentials and all, to a host of someone else's
     * choosing.
     */
    @ParameterizedTest
    @ValueSource(strings = {"eu_central_1", "EU-CENTRAL-1", "a/b", "evil.example.com/",
            "eu-central-1.attacker.example.com", "eu-central-1 "})
    void rejectsARegionThatIsNotARegionName(String region) {
        option("region", region);

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), is("'" + region + "' is not a valid AWS region name"));
    }

    /**
     * Blank has to collapse into unset. Docker compose's {@code AWS_REGION: ${AWS_REGION:-}} form
     * injects an empty string rather than omitting the variable; accepting it would build the host
     * {@code email..amazonaws.com} and report a DNS failure instead of a missing setting.
     */
    @Test
    void treatsABlankRegionAsNoRegionAtAll() {
        option("region", "   ");

        AwsSesConfigurationException fromOption = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));
        AwsSesConfigurationException fromEnvironment = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty().with("AWS_REGION", "")));

        assertThat(fromOption.getMessage(), containsString("No AWS region configured"));
        assertThat(fromEnvironment.getMessage(), containsString("No AWS region configured"));
    }

    /**
     * The default endpoint is asserted whole, including the path: SES v2 answers on
     * {@code /v2/email/outbound-emails} and on the older {@code /v1/email/outbound-emails} with a
     * different payload shape, and the region-less {@code email.amazonaws.com} is a different
     * (classic SES) API altogether.
     */
    @ParameterizedTest
    @ValueSource(strings = {"eu-central-1", "us-gov-west-1"})
    void buildsTheDefaultSesEndpointFromTheRegion(String region) {
        option("region", region);

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.sendEmailUri().toString(),
                is("https://email." + region + ".amazonaws.com/v2/email/outbound-emails"));
    }

    @Test
    void usesTheEndpointOverrideFromTheSpiOption() {
        option("region", "eu-central-1");
        option("endpoint", "https://vpce-0123.email.eu-central-1.vpce.amazonaws.com");

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.sendEmailUri().toString(),
                is("https://vpce-0123.email.eu-central-1.vpce.amazonaws.com/v2/email/outbound-emails"));
    }

    /**
     * {@code AWS_ENDPOINT_URL_SESV2} is the service-specific variable and outranks the blanket
     * {@code AWS_ENDPOINT_URL}; the SPI option outranks both. A box running LocalStack exports the
     * blanket one for every service, and a deliberate per-service override that lost to it would
     * silently keep sending mail to the stub.
     */
    @Test
    void prefersTheSpiOptionThenTheSesv2EndpointVariableThenTheGenericOne() {
        option("region", "eu-central-1");
        TestEnvironment bothVariables = TestEnvironment.empty()
                .with("AWS_ENDPOINT_URL_SESV2", "http://ses.localhost:4566")
                .with("AWS_ENDPOINT_URL", "http://localstack:4566");

        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()
                        .with("AWS_ENDPOINT_URL", "http://localstack:4566")).sendEmailUri().toString(),
                is("http://localstack:4566/v2/email/outbound-emails"));
        assertThat(AwsSesConfig.from(options(), bothVariables).sendEmailUri().toString(),
                is("http://ses.localhost:4566/v2/email/outbound-emails"));

        option("endpoint", "http://stub:9999");
        assertThat(AwsSesConfig.from(options(), bothVariables).sendEmailUri().toString(),
                is("http://stub:9999/v2/email/outbound-emails"));
    }

    /**
     * An operator copying an endpoint out of the AWS console brings the trailing slash with it. The
     * resulting {@code //v2/email/outbound-emails} is a different path, and it is the canonical
     * request's path that gets signed, so SES would answer 404 or 403 rather than normalise it.
     */
    @Test
    void stripsATrailingSlashFromAnEndpointOverride() {
        option("region", "eu-central-1");
        option("endpoint", "http://localhost:4566/");

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.sendEmailUri().toString(), is("http://localhost:4566/v2/email/outbound-emails"));
    }

    /**
     * The {@code Host} header is signed and sent verbatim, so it must be spelled the way the URL
     * spells it: an implicit port stays out, an explicit one stays in. Add a {@code :443} the
     * signature did not cover and AWS answers 403 with no indication of which header disagreed.
     */
    @Test
    void keepsTheHostHeaderSpelledTheWayTheEndpointUrlSpellsIt() {
        option("region", "eu-central-1");
        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).hostHeader(),
                is("email.eu-central-1.amazonaws.com"));

        option("endpoint", "http://localhost:8123");
        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).hostHeader(), is("localhost:8123"));

        option("endpoint", "https://email.eu-central-1.amazonaws.com:443");
        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).hostHeader(),
                is("email.eu-central-1.amazonaws.com:443"));
    }

    @Test
    void rejectsAnEndpointWithNoScheme() {
        option("region", "eu-central-1");
        option("endpoint", "localhost:4566");

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), containsString("'localhost:4566' is not a usable Amazon SES endpoint URL"));
    }

    @Test
    void rejectsAnEndpointThatIsNotAUrlAtAll() {
        option("region", "eu-central-1");
        option("endpoint", "https://exa mple.com");

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), is("'https://exa mple.com' is not a valid Amazon SES endpoint URL"));
        assertThat(thrown.getCause(), is(instanceOf(URISyntaxException.class)));
    }

    @Test
    void defaultsBothTimeoutsToTenSeconds() {
        option("region", "eu-central-1");

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.connectTimeoutMillis(), is(10_000));
        assertThat(config.readTimeoutMillis(), is(10_000));
    }

    @Test
    void readsBothTimeoutOverrides() {
        option("region", "eu-central-1");
        option("connect-timeout", "2500");
        option("read-timeout", "7000");

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.connectTimeoutMillis(), is(2500));
        assertThat(config.readTimeoutMillis(), is(7000));
    }

    /**
     * Apache HttpClient reads 0 and any negative value as "wait forever". The send happens inside a
     * Keycloak transaction, so an unbounded timeout on a blackholed route holds that transaction —
     * and its database connection — open until the operating system gives up on the handshake.
     */
    @ParameterizedTest
    @CsvSource({"connect-timeout,0", "connect-timeout,-1", "read-timeout,0", "read-timeout,-1"})
    void rejectsATimeoutThatMeansWaitForever(String timeoutOption, String value) {
        option("region", "eu-central-1");
        option(timeoutOption, value);

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), containsString("must be positive"));
        // The option is named in the message: an operator reading a boot failure should not have to
        // guess which of the two timeouts they mistyped.
        assertThat(thrown.getMessage(), containsString("spi-email-sender-aws-ses-" + timeoutOption));
    }

    /**
     * The operation path is appended to the configured endpoint, so every trailing slash has to go,
     * not just the last one: {@code https://host//} would otherwise post to {@code //v2/...}.
     */
    @ParameterizedTest
    @CsvSource({"https://stub.example.com", "https://stub.example.com/", "https://stub.example.com///"})
    void appendsTheOperationPathToAnEndpointHoweverItIsWritten(String endpoint) {
        option("region", "eu-central-1");
        option("endpoint", endpoint);

        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).sendEmailUri().toString(),
                is("https://stub.example.com/v2/email/outbound-emails"));
    }

    /**
     * An IPv6 endpoint keeps its brackets in the Host header. {@code URI#getHost} returns the
     * bracketed form, so concatenating the port is already correct — pinned because the header is
     * signed, and a Host that renders differently from what was signed is a 403 with no diagnosis.
     */
    @Test
    void bracketsAnIpv6EndpointInTheHostHeader() {
        option("region", "eu-central-1");
        option("endpoint", "http://[::1]:8500");

        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).hostHeader(), is("[::1]:8500"));
    }

    /**
     * The endpoint host is built from the region's partition. Hard-coding {@code amazonaws.com} makes
     * the default endpoint fail to resolve in the China partition, where the suffix differs.
     */
    @Test
    void usesThePartitionSuffixWhenBuildingTheDefaultEndpoint() {
        option("region", "cn-north-1");

        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).sendEmailUri().toString(),
                is("https://email.cn-north-1.amazonaws.com.cn/v2/email/outbound-emails"));
    }

    /**
     * The transport speaks HTTP only, so any other scheme parses happily at boot and then fails on
     * every single send.
     */
    @ParameterizedTest
    @CsvSource({"ftp://stub.example.com", "file://stub.example.com"})
    void rejectsAnEndpointThatIsNotHttp(String endpoint) {
        option("region", "eu-central-1");
        option("endpoint", endpoint);

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), containsString("must be http or https"));
    }

    /**
     * A malformed timeout must be a configuration error, not a raw {@link NumberFormatException}.
     * <p>
     * The factory catches {@link AwsSesConfigurationException} to stay silent when another email
     * sender is in use; anything else escapes that branch, so a typo in an option belonging to a
     * provider the server does not even use would stop it from booting. And the typo is easy: most
     * Keycloak duration options are written {@code 10s}.
     */
    @ParameterizedTest
    @CsvSource({"connect-timeout,10s", "read-timeout,thirty", "connect-timeout,1_000"})
    void rejectsATimeoutThatIsNotANumberOfMilliseconds(String timeoutOption, String value) {
        option("region", "eu-central-1");
        option(timeoutOption, value);

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), containsString("spi-email-sender-aws-ses-" + timeoutOption));
        assertThat(thrown.getMessage(), containsString(value));
    }

    /**
     * The operation path is appended to the configured endpoint. If the endpoint already carries a
     * query string the path lands inside it, and the request goes to {@code /} — an SES 404 a long
     * way from the typo that caused it.
     */
    @ParameterizedTest
    @CsvSource({"https://stub.example.com/?x=1", "https://stub.example.com/#frag"})
    void rejectsAnEndpointCarryingAQueryStringOrFragment(String endpoint) {
        option("region", "eu-central-1");
        option("endpoint", endpoint);

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> AwsSesConfig.from(options(), TestEnvironment.empty()));

        assertThat(thrown.getMessage(), containsString("no query string or fragment"));
    }

    /**
     * The configuration set is optional and reaches the SES request only when non-null, so a blank
     * one has to arrive as null: SES rejects {@code ConfigurationSetName: ""} with a 400 on every
     * single send.
     */
    @Test
    void leavesTheConfigurationSetUnsetWhenItIsAbsentOrBlank() {
        option("region", "eu-central-1");
        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).configurationSetName(), is(nullValue()));

        option("configuration-set", "   ");
        assertThat(AwsSesConfig.from(options(), TestEnvironment.empty()).configurationSetName(), is(nullValue()));
    }

    @Test
    void readsTheConfigurationSetName() {
        option("region", "eu-central-1");
        option("configuration-set", "keycloak-transactional");

        AwsSesConfig config = AwsSesConfig.from(options(), TestEnvironment.empty());

        assertThat(config.configurationSetName(), is("keycloak-transactional"));
    }

    private void option(String key, String value) {
        String property = OPTION_PREFIX + key;
        System.setProperty(property, value);
        setProperties.add(property);
    }

    private static Config.Scope options() {
        return Config.scope("emailSender", "aws-ses");
    }
}
