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

import java.time.Clock;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailSenderProviderFactory;
import org.keycloak.email.aws.credentials.AwsCredentialsProviderChain;
import org.keycloak.email.aws.credentials.AwsEnvironment;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.jboss.logging.Logger;

/**
 * Registers the Amazon SES email sender under the id {@code aws-ses}.
 * <p>
 * Selecting it is an explicit act: {@code --spi-email-sender-provider=aws-ses} (or
 * {@code KC_SPI_EMAIL_SENDER_PROVIDER=aws-ses}). Merely dropping the jar into {@code providers/}
 * changes nothing, which is deliberate — the factory keeps the inherited {@code order()} of 0, since
 * any positive order would make SES win Keycloak's default-provider resolution and quietly take over
 * every realm's transactional email the moment the jar landed.
 * <p>
 * Configuration is read in {@link #init(Config.Scope)} rather than in the constructor because the
 * constructor also runs during {@code kc.sh build}, inside an image that has none of the runtime
 * environment: anything read there would capture build-time values and look, at runtime, like a
 * configuration that was never applied.
 */
public class AwsSesEmailSenderProviderFactory implements EmailSenderProviderFactory {

    public static final String PROVIDER_ID = "aws-ses";

    /**
     * The name of the SPI this factory belongs to. Used to ask whether this provider is the one the
     * server was told to use, which decides whether a broken configuration is fatal.
     */
    private static final String EMAIL_SENDER_SPI = "emailSender";

    private static final Logger logger = Logger.getLogger(AwsSesEmailSenderProviderFactory.class);

    private final Clock clock;

    private AwsSesConfig config;
    private SesClient client;
    private AwsSesConfigurationException configurationError;

    public AwsSesEmailSenderProviderFactory() {
        this(Clock.systemUTC());
    }

    AwsSesEmailSenderProviderFactory(Clock clock) {
        this.clock = clock;
    }

    @Override
    public EmailSenderProvider create(KeycloakSession session) {
        if (config == null) {
            // configurationError is null only if create() ran before init(), which Keycloak does not
            // do — but "throw null" would then surface as a NullPointerException with nothing in it,
            // and a provider that cannot be built should say why.
            throw configurationError != null ? configurationError
                    : new AwsSesConfigurationException("The Amazon SES email sender was not initialised");
        }
        HttpClientProvider httpClientProvider = session.getProvider(HttpClientProvider.class);
        return new AwsSesEmailSenderProvider(config, client,
                new KeycloakHttpTransport(httpClientProvider.getHttpClient()), clock);
    }

    @Override
    public void init(Config.Scope scope) {
        AwsEnvironment environment = AwsEnvironment.SYSTEM;
        try {
            config = AwsSesConfig.from(scope, environment);
            client = new SesClient(config, AwsCredentialsProviderChain.defaultChain(environment));
            if (isSelectedEmailSender()) {
                logger.infof("Amazon SES email sender configured for region %s (endpoint %s)",
                        config.region(), config.sendEmailUri());
                if (!"https".equalsIgnoreCase(config.sendEmailUri().getScheme())) {
                    // AWS_ENDPOINT_URL is a cross-service variable: an environment that sets it for a
                    // local emulator, and shares that environment with Keycloak, would silently send
                    // activation links and their tokens over plaintext to whatever answers there.
                    logger.warnf("The Amazon SES endpoint %s is not HTTPS; email, including action"
                            + " tokens, will be sent in clear text", config.sendEmailUri());
                }
            } else {
                // Keycloak initialises every factory of an SPI, not only the selected one, so an
                // INFO line here would tell every server in the world that SES is "configured".
                logger.debugf("Amazon SES email sender is available for region %s but the server uses the '%s' sender",
                        config.region(), configuredEmailSender());
            }
        } catch (AwsSesConfigurationException e) {
            configurationError = e;
            if (isSelectedEmailSender()) {
                // Fail the boot rather than start an identity provider that cannot send an
                // activation link, a password reset or an invitation, and would only reveal it the
                // first time a user needed one.
                throw e;
            }
            // The jar is on the classpath but another sender is in use: nothing is wrong.
            logger.debugf("Amazon SES email sender is present but not configured (%s); the server is using the '%s' sender",
                    e.getMessage(), configuredEmailSender());
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (config == null || !isSelectedEmailSender()) {
            // A region in the ambient environment is enough to build a configuration, so without this
            // guard every AWS-hosted Keycloak would be warned about duplicate emails from a provider
            // it does not use.
            return;
        }
        int httpRetries = Config.scope("connectionsHttpClient", "default").getInt("max-retries", 0);
        if (httpRetries > 0) {
            // Keycloak's shared HTTP client retries a request whose bytes already reached the wire
            // when this is set. SES SendEmail has no idempotency token, so a retried send is a second
            // email in a real person's inbox — and it cannot be opted out of per request.
            logger.warnf("spi-connections-http-client-default-max-retries is set to %d: a retried Amazon SES request"
                    + " can deliver the same email twice, because the SES API has no idempotency token", httpRetries);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("region")
                .type("string")
                .helpText("AWS region hosting the verified SES identity, for example eu-central-1."
                        + " Defaults to the AWS_REGION or AWS_DEFAULT_REGION environment variable."
                        + " SES identities are per region: a domain verified in one region does not exist in another.")
                .add()
                .property()
                .name("endpoint")
                .type("string")
                .helpText("Overrides the SES endpoint URL, which defaults to https://email.<region>.amazonaws.com."
                        + " Set it for a VPC endpoint, a FIPS endpoint, or a local stub in tests."
                        + " Defaults to the AWS_ENDPOINT_URL_SESV2 or AWS_ENDPOINT_URL environment variable.")
                .add()
                .property()
                .name("configuration-set")
                .type("string")
                .helpText("Name of the SES configuration set to send through, which is how open, bounce and"
                        + " complaint events are published to CloudWatch, SNS or EventBridge. Optional.")
                .add()
                .property()
                .name("connect-timeout")
                .type("int")
                .defaultValue(AwsSesConfig.DEFAULT_TIMEOUT_MILLIS)
                .helpText("Milliseconds to wait for the TCP connection to SES. The realm's own connection timeout,"
                        + " when set, takes precedence.")
                .add()
                .property()
                .name("read-timeout")
                .type("int")
                .defaultValue(AwsSesConfig.DEFAULT_TIMEOUT_MILLIS)
                .helpText("Milliseconds to wait for the SES response. Kept short on purpose: the send happens inside"
                        + " a Keycloak transaction. The realm's own timeout, when set, takes precedence.")
                .add()
                .build();
    }

    private boolean isSelectedEmailSender() {
        return PROVIDER_ID.equals(configuredEmailSender());
    }

    /**
     * Which email sender the server was told to use, resolved the way Keycloak's own session factory
     * resolves it: {@code --spi-email-sender-provider} wins, and only when it is unset does
     * {@code --spi-email-sender-provider-default} apply. Both spellings select a provider, so a
     * check that knew about only one would fail open for operators who used the other.
     */
    private String configuredEmailSender() {
        String provider = Config.getProvider(EMAIL_SENDER_SPI);
        return provider != null ? provider : Config.getDefaultProvider(EMAIL_SENDER_SPI);
    }
}
