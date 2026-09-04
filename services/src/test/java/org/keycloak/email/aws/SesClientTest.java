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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.email.EmailException;
import org.keycloak.email.aws.credentials.AwsCredentials;
import org.keycloak.email.aws.credentials.AwsCredentialsProvider;
import org.keycloak.email.aws.credentials.AwsCredentialsProviderChain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The exact HTTP call this provider makes to SES, asserted on the captured request rather than on a
 * live account.
 * <p>
 * Every value pinned here is one AWS validates and nobody else does: a wrong signing service, a
 * {@code Host} carrying a port, a {@code Data} field that is not base64 of the MIME, all come back
 * as an opaque 403 or {@code BadRequestException} the first time a user asks for a password reset.
 */
class SesClientTest {

    private static final String REGION = "eu-central-1";
    private static final String SPI_PREFIX = "keycloak.emailSender.aws-ses.";

    private static final String ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final String SESSION_TOKEN = "FwoGZXIvYXdzEExampleSessionToken==";

    private static final String RECIPIENT = "user@example.com";
    private static final Instant SEND_TIME = Instant.parse("2026-09-03T10:15:30Z");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final FakeTransport transport = new FakeTransport();

    /** SPI options are JVM-wide system properties here; leaving one set would leak into the next test. */
    @AfterEach
    void clearSpiOptions() {
        System.getProperties().stringPropertyNames().stream()
                .filter(name -> name.startsWith(SPI_PREFIX))
                .forEach(System::clearProperty);
    }

    /**
     * A 200 whose MessageId is present but empty is as useless as one without it: the id is the only
     * handle AWS support can trace, so accepting "" would log a successful send that cannot be
     * followed up.
     */
    @Test
    void refusesAnAcceptanceWithABlankMessageId() {
        transport.respondWith(200, "{\"MessageId\":\"   \"}");

        EmailException failure = assertThrows(EmailException.class,
                () -> client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME));

        assertThat(failure.getMessage(), containsString("returned no message id"));
    }

    @Test
    void postsTheSendEmailRequestToTheRegionalSesEndpoint() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");

        client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);

        AwsHttpRequest request = transport.lastRequest();
        assertThat(request.method(), is("POST"));
        assertThat(request.uri().toString(), is("https://email.eu-central-1.amazonaws.com/v2/email/outbound-emails"));
        // Signed, so it must be the bare hostname: an explicit ":443" the signature did not cover is
        // a 403 with nothing in the response explaining why.
        assertThat(request.headers().get("Host"), is("email.eu-central-1.amazonaws.com"));
        assertThat(request.headers().get("Content-Type"), is("application/json"));
    }

    /**
     * SES answers on {@code email.<region>.amazonaws.com} but signs as {@code ses}. Signing as
     * "email" is the classic mistake and it is silent: every send comes back 403 with no hint that
     * the credential scope is what AWS objected to.
     */
    @Test
    void signsWithTheSesServiceNameAndTheDateOfTheSendingInstant() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");

        client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);

        AwsHttpRequest request = transport.lastRequest();
        assertThat(request.headers().get("X-Amz-Date"), is("20260903T101530Z"));
        assertThat(request.headers().get("Authorization"), startsWith("AWS4-HMAC-SHA256 Credential="
                + ACCESS_KEY_ID + "/20260903/" + REGION + "/ses/aws4_request"));
    }

    /**
     * The body is the whole API contract. {@code Data} is base64 of the MIME on the plain HTTPS API
     * — only the AWS SDKs encode it for the caller — and {@code FromEmailAddress} must be the very
     * string in the {@code From} header, since AWS documents no behaviour for the two disagreeing.
     */
    @Test
    void sendsTheComposedMimeBytesAsBase64RawContent() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");
        SesRawMessage message = message();

        client().sendEmail(transport, message, null, 10_000, 10_000, SEND_TIME);

        JsonNode body = bodyOf(transport.lastRequest());
        assertThat(body.get("FromEmailAddress").asText(), is("AldoTime <noreply@aldotime.it>"));
        assertThat(body.get("FromEmailAddress").asText(), is(headerValue(message.mime(), "From")));

        JsonNode toAddresses = body.get("Destination").get("ToAddresses");
        assertThat(toAddresses.size(), is(1));
        assertThat(toAddresses.get(0).asText(), is(RECIPIENT));

        byte[] decoded = Base64.getDecoder().decode(body.get("Content").get("Raw").get("Data").asText());
        assertThat(decoded, is(equalTo(message.mime())));
        assertThat(new String(decoded, StandardCharsets.UTF_8), containsString("To: " + RECIPIENT + "\r\n"));
    }

    /**
     * {@code ReplyToAddresses} stays out of the request on purpose: Keycloak already wrote the
     * {@code Reply-To} header into the MIME and AWS documents no interaction between the parameter
     * and a raw message's own headers. Sending both would be betting on undocumented behaviour.
     */
    @Test
    void omitsReplyToAddressesAndLeavesReplyToInTheMime() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");
        Map<String, String> realm = realmConfig();
        realm.put("replyTo", "support@aldotime.it");
        SesRawMessage message = SesRawMessage.compose(realm, RECIPIENT, "Reset your password", "text", "<p>html</p>");

        client().sendEmail(transport, message, null, 10_000, 10_000, SEND_TIME);

        assertThat(headerValue(message.mime(), "Reply-To"), is("support@aldotime.it"));
        assertThat(bodyOf(transport.lastRequest()).has("ReplyToAddresses"), is(false));
    }

    /** The realm's envelopeFrom is its bounce address, and this is the SES parameter with that meaning. */
    @Test
    void mapsTheRealmEnvelopeFromToFeedbackForwardingEmailAddress() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");

        client().sendEmail(transport, message(), "bounces@aldotime.it", 10_000, 10_000, SEND_TIME);

        assertThat(bodyOf(transport.lastRequest()).get("FeedbackForwardingEmailAddress").asText(),
                is("bounces@aldotime.it"));
    }

    /**
     * Absent, not present-and-empty: SES rejects {@code FeedbackForwardingEmailAddress: ""} with a
     * validation error, so a realm that simply never set an envelopeFrom — or set it to whitespace —
     * must produce a body without the field at all.
     */
    @Test
    void omitsFeedbackForwardingEmailAddressWhenTheRealmSetNoEnvelopeFrom() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"1\"}").respondWith(200, "{\"MessageId\":\"2\"}");
        SesClient client = client();

        client.sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);
        client.sendEmail(transport, message(), "   ", 10_000, 10_000, SEND_TIME);

        assertThat(bodyOf(transport.request(0)).has("FeedbackForwardingEmailAddress"), is(false));
        assertThat(bodyOf(transport.request(1)).has("FeedbackForwardingEmailAddress"), is(false));
    }

    /**
     * A configuration set is how open, bounce and complaint events are published. Naming one that
     * does not exist fails the send, so an unconfigured server must not send the field at all.
     */
    @Test
    void includesTheConfigurationSetOnlyWhenOneIsConfigured() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"1\"}");
        new SesClient(config(Map.of("configuration-set", "keycloak-events")), chain(longLivedCredentials()))
                .sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);
        assertThat(bodyOf(transport.lastRequest()).get("ConfigurationSetName").asText(), is("keycloak-events"));

        clearSpiOptions();
        transport.respondWith(200, "{\"MessageId\":\"2\"}");
        client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);
        assertThat(bodyOf(transport.lastRequest()).has("ConfigurationSetName"), is(false));
    }

    /**
     * With an IAM role — the way the provider is meant to run — the credentials are temporary. The
     * session token has to be a signed header as well as a sent one: AWS rejects a request whose
     * token is outside {@code SignedHeaders}, and a token sent but unsigned looks like a valid
     * request until it reaches AWS.
     */
    @Test
    void signsAndSendsTheSessionTokenOfTemporaryCredentials() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");
        AwsCredentials temporary = new AwsCredentials("ASIAIOSFODNN7EXAMPLE", SECRET_ACCESS_KEY, SESSION_TOKEN,
                SEND_TIME.plusSeconds(3600));

        new SesClient(config(Map.of()), chain(temporary))
                .sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);

        AwsHttpRequest request = transport.lastRequest();
        assertThat(request.headers().get("X-Amz-Security-Token"), is(SESSION_TOKEN));
        assertThat(request.headers().get("Authorization"),
                containsString("SignedHeaders=content-type;host;x-amz-date;x-amz-security-token,"));
    }

    @Test
    void returnsTheMessageIdSesAcknowledgedTheSendWith() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"010f0192a9b4c5d6-11112222-3333-4444-5555-666677778888-000000\"}");

        String messageId = client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);

        assertThat(messageId, is("010f0192a9b4c5d6-11112222-3333-4444-5555-666677778888-000000"));
    }

    /**
     * A 2xx with no message id is not a successful send: it is a proxy, a stub or a captive portal
     * answering in SES's place. Returning null from here would be recorded as a delivered email.
     */
    @Test
    void refusesASuccessResponseThatCarriesNoMessageId() {
        transport.respondWith(200, "{\"ResponseMetadata\":{}}");

        EmailException failure = assertThrows(EmailException.class,
                () -> client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME));

        assertThat(failure.getMessage(), is("Amazon SES accepted the message but returned no message id"));
    }

    /**
     * What an administrator sees when SES refuses the message. It must carry the AWS error name and
     * the AWS request id — the only handle AWS support can follow — and it must not carry the
     * signing secret, the Authorization header or the session token, because this string reaches the
     * admin console and the server log.
     */
    @Test
    void reportsASesRejectionWithTheAwsErrorNameAndRequestIdButNoCredentials() {
        transport.respondWith(400,
                Map.of("x-amzn-ErrorType", "MessageRejected:8c3f", "x-amzn-RequestId", "b7e6a1d2-0b6f-4c2e-9c3a-2b1f"),
                "{\"message\":\"Email address is not verified in region EU-CENTRAL-1\"}");

        EmailException failure = assertThrows(EmailException.class,
                () -> client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME));

        assertThat(failure.getMessage(), containsString("400"));
        assertThat(failure.getMessage(), containsString("MessageRejected"));
        assertThat(failure.getMessage(), containsString("Email address is not verified in region EU-CENTRAL-1"));
        assertThat(failure.getMessage(), containsString("b7e6a1d2-0b6f-4c2e-9c3a-2b1f"));
        assertThat(failure.getMessage(), not(containsString(SECRET_ACCESS_KEY)));
        assertThat(failure.getMessage(), not(containsString(SESSION_TOKEN)));
        assertThat(failure.getMessage(), not(containsString("AWS4-HMAC-SHA256")));
    }

    /**
     * The request is attempted exactly once, deliberately. SES {@code SendEmail} takes no idempotency
     * token, so a call that failed after its bytes reached AWS cannot be told apart from one that
     * never arrived — and a retry of the former puts a second activation email in a real person's
     * inbox. If someone ever adds a retry loop here, this is the assertion that stops it.
     */
    @Test
    void doesNotRetryWhenTheTransportFails() {
        transport.failWith(new IOException("Connection reset"));

        EmailException failure = assertThrows(EmailException.class,
                () -> client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME));

        assertThat(failure.getMessage(), containsString("email.eu-central-1.amazonaws.com"));
        assertThat(failure.getMessage(), containsString("Connection reset"));
        assertThat(transport.requestCount(), is(1));
    }

    /**
     * The send happens inside a Keycloak transaction, so the timeouts the caller resolved — realm
     * settings first, SPI options second — have to reach the request the transport executes.
     */
    @Test
    void putsTheCallerSuppliedTimeoutsOnTheRequest() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");

        client().sendEmail(transport, message(), null, 1_234, 5_678, SEND_TIME);

        assertThat(transport.lastRequest().connectTimeoutMillis(), is(1_234));
        assertThat(transport.lastRequest().readTimeoutMillis(), is(5_678));
    }

    /** No credential source reaches the network here, so the only request captured is the SES send itself. */
    @Test
    void resolvesCredentialsWithoutIssuingAnyAdditionalRequest() throws Exception {
        transport.respondWith(200, "{\"MessageId\":\"0100019\"}");

        client().sendEmail(transport, message(), null, 10_000, 10_000, SEND_TIME);

        assertThat(transport.requestCount(), is(1));
        assertThat(transport.lastRequest().headers().get("X-Amz-Security-Token"), is(nullValue()));
    }

    private SesClient client() {
        return new SesClient(config(Map.of()), chain(longLivedCredentials()));
    }

    private static AwsCredentials longLivedCredentials() {
        return AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
    }

    /**
     * A real {@code Config.Scope}: Keycloak's own {@code SystemPropertiesScope} reading
     * {@code keycloak.emailSender.aws-ses.*}. Hand-writing a {@code Config.Scope} would compile here
     * and stop compiling on the first server release that adds a method to the interface.
     */
    private static AwsSesConfig config(Map<String, String> options) {
        Config.init(new Config.SystemPropertiesConfigProvider());
        System.setProperty(SPI_PREFIX + "region", REGION);
        options.forEach((key, value) -> System.setProperty(SPI_PREFIX + key, value));
        return AwsSesConfig.from(Config.scope("emailSender", "aws-ses"), TestEnvironment.empty());
    }

    private static AwsCredentialsProviderChain chain(AwsCredentials credentials) {
        return new AwsCredentialsProviderChain(List.of(new FixedCredentialsProvider(credentials)));
    }

    private static SesRawMessage message() throws EmailException {
        return SesRawMessage.compose(realmConfig(), RECIPIENT, "Reset your password", "text", "<p>html</p>");
    }

    private static Map<String, String> realmConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("from", "noreply@aldotime.it");
        config.put("fromDisplayName", "AldoTime");
        return config;
    }

    private static JsonNode bodyOf(AwsHttpRequest request) throws IOException {
        return JSON.readTree(request.body());
    }

    /** Reads a header out of the serialised MIME, which is CRLF-normalised and not folded here. */
    private static String headerValue(byte[] mime, String name) {
        for (String line : new String(mime, StandardCharsets.UTF_8).split("\r\n")) {
            if (line.isEmpty()) {
                break;
            }
            if (line.startsWith(name + ": ")) {
                return line.substring(name.length() + 2);
            }
        }
        throw new AssertionError("No " + name + " header in the MIME message");
    }

    private record FixedCredentialsProvider(AwsCredentials credentials) implements AwsCredentialsProvider {

        @Override
        public AwsCredentials resolve(AwsHttpTransport transport) {
            return credentials;
        }

        @Override
        public String name() {
            return "fixed test credentials";
        }
    }
}
