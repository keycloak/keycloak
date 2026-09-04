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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;

import org.keycloak.Config;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The opt-in and fail-closed contract, which is the whole safety story of this extension.
 * <p>
 * Dropping the jar into {@code providers/} must change nothing on a server that sends over SMTP, and
 * selecting it without configuring it must stop the boot rather than surface at the first password
 * reset. Both are decided in {@link AwsSesEmailSenderProviderFactory#init(Config.Scope)} and in
 * {@link AwsSesEmailSenderProviderFactory#order()}, and both are the kind of thing a well-meant
 * one-line change quietly inverts.
 * <p>
 * The server configuration is a real {@link Config.SystemPropertiesConfigProvider} over JVM system
 * properties rather than a test double, because {@code Config.Scope} has gained methods since 26.0
 * and a hand-written implementation would stop compiling on the next server that does.
 */
class AwsSesEmailSenderProviderFactoryTest {

    private static final String REGION_OPTION = "keycloak.emailSender.aws-ses.region";

    /** What {@code --spi-email-sender-provider} / {@code KC_SPI_EMAIL_SENDER_PROVIDER} lands in. */
    private static final String SELECTED_EMAIL_SENDER = "keycloak.emailSender.provider";

    /**
     * What {@code --spi-email-sender-provider-default} lands in. Keycloak's own resolution falls back
     * to it when {@code provider} is unset, so it selects this sender just as effectively.
     */
    private static final String DEFAULT_EMAIL_SENDER = "keycloak.emailSender.provider.default";

    /**
     * A configuration the provider cannot use, expressed as a region that is not a region name
     * rather than as no region at all.
     * <p>
     * The distinction matters for the test, not for the provider: both raise the same
     * {@link AwsSesConfigurationException} out of {@link AwsSesConfig#from}, and it is that exception
     * that decides whether the boot survives. But {@code init} reads the process environment through
     * {@link org.keycloak.email.aws.credentials.AwsEnvironment#SYSTEM}, and a test cannot unset
     * {@code AWS_REGION} — so "no region anywhere" is only reproducible on a machine that exports
     * none, while an SPI option that outranks the environment is reproducible everywhere. What the
     * missing-region message tells the operator is pinned in {@code AwsSesConfigTest} instead.
     */
    private static final String UNUSABLE_REGION = "Frankfurt";

    private final AwsSesEmailSenderProviderFactory factory = new AwsSesEmailSenderProviderFactory();

    @BeforeEach
    void useSystemPropertiesAsTheServerConfiguration() {
        Config.init(new Config.SystemPropertiesConfigProvider());
        clearProperties();
    }

    @AfterEach
    void clearProperties() {
        System.clearProperty(REGION_OPTION);
        System.clearProperty(SELECTED_EMAIL_SENDER);
        System.clearProperty(DEFAULT_EMAIL_SENDER);
    }

    @Test
    void registersItselfUnderTheIdOperatorsSelectItBy() {
        assertThat(factory.getId(), is("aws-ses"));
    }

    /**
     * Keycloak prefers any factory whose {@code order()} is greater than zero over the one called
     * "default". A non-zero order here would therefore make SES the email sender of every realm on
     * the server the moment the jar landed in {@code providers/}, with no setting changed and
     * nothing in the log to say so. This assertion is the guard against someone helpfully raising it.
     */
    @Test
    void neverOutranksTheDefaultEmailSender() {
        assertThat(factory.order(), is(0));
    }

    /**
     * A successful init leaves no return value to assert on, so it is asserted through what
     * {@code create} then produces: a real sender. Asserting instead that {@code create} throws
     * would assert nothing at all — a factory whose {@code init} never ran throws there too, from
     * {@code throw configurationError} with a null field — so the provider that comes back is the
     * only observation that tells the initialised state from the broken one.
     */
    @Test
    void initialisesTheSenderWhenARegionIsConfigured() {
        System.setProperty(REGION_OPTION, "eu-central-1");

        factory.init(Config.scope("emailSender", "aws-ses"));

        assertThat(factory.create(sessionOfferingKeycloaksHttpClient()),
                is(instanceOf(AwsSesEmailSenderProvider.class)));
    }

    /**
     * The jar being present must never break a server that sends over SMTP: an unconfigured SES
     * sender that nobody selected is not an error, and throwing here would take down every Keycloak
     * that merely has the provider on its classpath. The error is remembered rather than discarded,
     * so a later {@code create} still refuses to hand back a half-built provider — asserting that is
     * also how this test proves {@code init} returned normally instead of throwing.
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"default"})
    void startsQuietlyWhenTheServerUsesAnotherEmailSender(String selectedSender) {
        System.setProperty(REGION_OPTION, UNUSABLE_REGION);
        if (selectedSender != null) {
            System.setProperty(SELECTED_EMAIL_SENDER, selectedSender);
        }

        factory.init(Config.scope("emailSender", "aws-ses"));

        AwsSesConfigurationException thrown =
                assertThrows(AwsSesConfigurationException.class, () -> factory.create(null));
        assertThat(thrown.getMessage(), containsString(UNUSABLE_REGION));
    }

    /**
     * The same quiet start when the <em>default</em> provider names someone else. A check that read
     * only {@code --spi-email-sender-provider} gets this branch right by accident;
     * {@link #failsTheBootWhenItIsTheDefaultSenderAndCannotBeConfigured()} is where the same check
     * gets it wrong in the direction that matters.
     */
    @Test
    void startsQuietlyWhenTheDefaultEmailSenderIsAnotherProvider() {
        System.setProperty(REGION_OPTION, UNUSABLE_REGION);
        System.setProperty(DEFAULT_EMAIL_SENDER, "default");

        factory.init(Config.scope("emailSender", "aws-ses"));

        assertThrows(AwsSesConfigurationException.class, () -> factory.create(null));
    }

    /**
     * The mirror image: once the server has been told to send through SES, a configuration it cannot
     * use is fatal at boot. A Keycloak that starts but cannot send an activation link, a password
     * reset or an invitation reports the problem for the first time to a locked-out user.
     */
    @Test
    void failsTheBootWhenItIsTheSelectedSenderAndCannotBeConfigured() {
        System.setProperty(REGION_OPTION, UNUSABLE_REGION);
        System.setProperty(SELECTED_EMAIL_SENDER, "aws-ses");

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> factory.init(Config.scope("emailSender", "aws-ses")));

        assertThat(thrown.getMessage(), containsString(UNUSABLE_REGION));
    }

    /**
     * {@code --spi-email-sender-provider-default=aws-ses} selects this sender exactly as
     * {@code --spi-email-sender-provider=aws-ses} does: Keycloak's session factory resolves the
     * default provider whenever the explicit one is unset. A fail-closed check that read only the
     * explicit setting would let such a server boot with a broken SES configuration and report it,
     * for the first time, to a user waiting for a password-reset mail.
     */
    @Test
    void failsTheBootWhenItIsTheDefaultSenderAndCannotBeConfigured() {
        System.setProperty(REGION_OPTION, UNUSABLE_REGION);
        System.setProperty(DEFAULT_EMAIL_SENDER, "aws-ses");

        AwsSesConfigurationException thrown = assertThrows(AwsSesConfigurationException.class,
                () -> factory.init(Config.scope("emailSender", "aws-ses")));

        assertThat(thrown.getMessage(), containsString(UNUSABLE_REGION));
    }

    /**
     * With both set, only {@code provider} decides — which is what Keycloak's own resolution does,
     * and the reason it has to be mirrored here rather than approximated. Taking the union of the
     * two instead would stop the boot of a server that deliberately overrode an inherited
     * {@code provider-default=aws-ses} back to SMTP; taking only the default would let a server that
     * asked for SES boot without it.
     */
    @Test
    void letsTheExplicitProviderOutrankTheDefaultProvider() {
        System.setProperty(REGION_OPTION, UNUSABLE_REGION);
        System.setProperty(DEFAULT_EMAIL_SENDER, "aws-ses");
        System.setProperty(SELECTED_EMAIL_SENDER, "default");

        // The explicit setting says SMTP, so the broken SES configuration is not fatal.
        factory.init(Config.scope("emailSender", "aws-ses"));
        assertThrows(AwsSesConfigurationException.class, () -> factory.create(null));

        // Swapped, and a fresh factory because init() is a once-per-boot call: now the explicit
        // setting says SES, and the same configuration must stop the boot.
        System.setProperty(DEFAULT_EMAIL_SENDER, "default");
        System.setProperty(SELECTED_EMAIL_SENDER, "aws-ses");

        assertThrows(AwsSesConfigurationException.class,
                () -> new AwsSesEmailSenderProviderFactory().init(Config.scope("emailSender", "aws-ses")));
    }

    /**
     * This list is what upstream renders into the all-config guide, so an option added to
     * {@link AwsSesConfig} and forgotten here is an option nobody can discover. The order is asserted
     * too: it is the order the guide prints, and region before endpoint before the optional rest is
     * the order an operator configures them in.
     */
    @Test
    void documentsEveryOptionAnOperatorCanSet() {
        List<ProviderConfigProperty> metadata = factory.getConfigMetadata();

        assertThat(metadata.stream().map(ProviderConfigProperty::getName).toList(),
                contains("region", "endpoint", "configuration-set", "connect-timeout", "read-timeout"));
        for (ProviderConfigProperty property : metadata) {
            assertThat("help text for " + property.getName(), property.getHelpText(), is(not(blankOrNullString())));
        }
    }

    /**
     * Credentials deliberately do not come from Keycloak configuration, and this is the assertion
     * that keeps it that way: {@code kc.sh show-config} prints SPI options unmasked, so an
     * {@code access-key} option added here for convenience would print the secret to the console and
     * into whatever collects it. Credentials belong to the AWS chain — an IAM role, or the process
     * environment.
     */
    @Test
    void exposesNoOptionThatCouldHoldACredential() {
        for (ProviderConfigProperty property : factory.getConfigMetadata()) {
            String name = property.getName().toLowerCase(Locale.ROOT);
            for (String secretish : List.of("key", "secret", "password", "token")) {
                assertThat(name, not(containsString(secretish)));
            }
        }
    }

    /**
     * A session that answers {@code getProvider} with an {@link HttpClientProvider} and does nothing
     * else. Both interfaces are JDK proxies rather than hand-written classes for the same reason this
     * test drives a real {@link Config.Scope}: both gain methods between server versions, and an
     * implementation of either would stop compiling on the next one. There is no mocking framework on
     * this classpath by design. The HTTP client itself is never touched — {@code create} only stores
     * it — so the provider proxy may answer {@code null}.
     */
    private static KeycloakSession sessionOfferingKeycloaksHttpClient() {
        Object httpClientProvider = Proxy.newProxyInstance(HttpClientProvider.class.getClassLoader(),
                new Class<?>[]{HttpClientProvider.class}, (proxy, method, arguments) -> null);
        return (KeycloakSession) Proxy.newProxyInstance(KeycloakSession.class.getClassLoader(),
                new Class<?>[]{KeycloakSession.class}, (proxy, method, arguments) ->
                        "getProvider".equals(method.getName()) ? httpClientProvider : null);
    }
}
