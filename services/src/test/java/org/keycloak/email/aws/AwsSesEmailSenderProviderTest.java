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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.keycloak.Config;
import org.keycloak.email.EmailException;
import org.keycloak.email.aws.credentials.AwsCredentials;
import org.keycloak.email.aws.credentials.AwsCredentialsProvider;
import org.keycloak.email.aws.credentials.AwsCredentialsProviderChain;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The sender as a Keycloak server sees it: a realm's SMTP settings in, one signed SES request out.
 * <p>
 * The pieces below it each have their own test — the signature, the credential chain, the error
 * parsing. What only this test can catch is the wiring between them: a realm setting read under the
 * wrong key, a timeout passed in the wrong argument position, an envelope value that never reaches
 * the JSON. Every assertion here is made on the bytes that would have gone to AWS.
 */
class AwsSesEmailSenderProviderTest {

    private static final String ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    /** Frozen so the credential scope date in the {@code Authorization} header is a known value. */
    private static final Instant SEND_TIME = Instant.parse("2026-09-03T10:15:30Z");

    private static final int SPI_CONNECT_TIMEOUT_MILLIS = 2500;
    private static final int SPI_READ_TIMEOUT_MILLIS = 7500;

    /** The shape of a real SES v2 {@code SendEmail} 200, trimmed to the field the client reads. */
    private static final String ACCEPTED = "{\"MessageId\":\"010f0192a8f4b1d2-4b0f4a2c-0e5b-4a1f-9d3c-000000000000-000000\"}";

    private final FakeTransport transport = new FakeTransport();

    private AwsSesEmailSenderProvider provider;

    @BeforeEach
    void configureTheServerWideSender() {
        System.setProperty("keycloak.emailSender.aws-ses.region", "eu-central-1");
        System.setProperty("keycloak.emailSender.aws-ses.connect-timeout", String.valueOf(SPI_CONNECT_TIMEOUT_MILLIS));
        System.setProperty("keycloak.emailSender.aws-ses.read-timeout", String.valueOf(SPI_READ_TIMEOUT_MILLIS));
        Config.init(new Config.SystemPropertiesConfigProvider());

        AwsSesConfig sesConfig = AwsSesConfig.from(Config.scope("emailSender", "aws-ses"), TestEnvironment.empty());
        SesClient client = new SesClient(sesConfig, new AwsCredentialsProviderChain(List.of(new FixedCredentials())));
        provider = new AwsSesEmailSenderProvider(sesConfig, client, transport, Clock.fixed(SEND_TIME, ZoneOffset.UTC));
    }

    @AfterEach
    void restoreTheServerWideSender() {
        System.clearProperty("keycloak.emailSender.aws-ses.region");
        System.clearProperty("keycloak.emailSender.aws-ses.connect-timeout");
        System.clearProperty("keycloak.emailSender.aws-ses.read-timeout");
    }

    /**
     * The end-to-end proof. One realm configuration produces one request, addressed and signed for
     * SES, whose base64 payload is a MIME message carrying exactly what the caller passed in. A
     * sender that got any single hand-off wrong — the {@code from} key, the multipart order, the
     * base64, the signing service name — fails here rather than in an operator's inbox.
     */
    @Test
    void deliversTheComposedMessageToSesAsOneSignedRequest() throws Exception {
        transport.respondWith(200, ACCEPTED);
        Map<String, String> config = realmWithSender();
        config.put("replyTo", "support@example.com");
        config.put("replyToDisplayName", "Example Support");

        provider.send(config, "user@example.com", "Reset your password",
                "Open the link to reset your password.", "<html><body><a href=\"#\">Reset</a></body></html>");

        assertThat(transport.requestCount(), is(1));
        AwsHttpRequest request = transport.lastRequest();
        assertThat(request.method(), is("POST"));
        assertThat(request.uri(), is(URI.create("https://email.eu-central-1.amazonaws.com/v2/email/outbound-emails")));
        assertThat(request.headers().get("Host"), is("email.eu-central-1.amazonaws.com"));
        assertThat(request.headers().get("Content-Type"), is("application/json"));
        assertThat(request.headers().get("X-Amz-Date"), is("20260903T101530Z"));
        // The credential scope, not the signature: the MIME carries a Date header and a generated
        // multipart boundary, so the signed bytes are not reproducible. AwsV4SignerTest pins the
        // signature itself against AWS's published vectors; what matters here is that the region and
        // the service name reaching the signer are the SES ones.
        assertThat(request.headers().get("Authorization"), startsWith(
                "AWS4-HMAC-SHA256 Credential=" + ACCESS_KEY_ID + "/20260903/eu-central-1/ses/aws4_request,"));

        JsonNode payload = payloadOf(request);
        assertThat(payload.get("FromEmailAddress").asText(), is("Example <noreply@example.com>"));
        assertThat(payload.get("Destination").get("ToAddresses").size(), is(1));
        assertThat(payload.get("Destination").get("ToAddresses").get(0).asText(), is("user@example.com"));

        MimeMessage message = mimeOf(request);
        assertThat(message.getHeader("To", null), is("user@example.com"));
        assertThat(message.getHeader("From", null), is("Example <noreply@example.com>"));
        assertThat(message.getHeader("Reply-To", null), is("Example Support <support@example.com>"));
        assertThat(message.getSubject(), is("Reset your password"));

        MimeMultipart body = (MimeMultipart) message.getContent();
        assertThat(body.getContentType(), startsWith("multipart/alternative"));
        assertThat(body.getCount(), is(2));
        // Text first: RFC 2046 says the last alternative is the richest, so a client that renders
        // HTML must find it after the plain text, not before it.
        assertThat(body.getBodyPart(0).getContentType(), startsWith("text/plain"));
        assertThat(body.getBodyPart(0).getContent(), is("Open the link to reset your password."));
        assertThat(body.getBodyPart(1).getContentType(), startsWith("text/html"));
        assertThat(body.getBodyPart(1).getContent(), is("<html><body><a href=\"#\">Reset</a></body></html>"));
    }

    /**
     * {@code MimeMessage.writeTo()} emits a bare LF inside a 7bit part. SES accepts the raw message
     * anyway and then delivers something no MTA agrees on — most visibly, a body whose lines run
     * together or a part boundary that is not recognised. The normalisation has to survive the trip
     * through base64, which is why it is asserted on the decoded bytes rather than on the stream.
     */
    @Test
    void emitsTheRawMimeWithNoBareLineFeeds() throws Exception {
        transport.respondWith(200, ACCEPTED);

        provider.send(realmWithSender(), "user@example.com", "Your account",
                "first line\nsecond line\nthird line", "<p>one</p>\n<p>two</p>");

        byte[] raw = rawMimeOf(transport.lastRequest());
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == '\n') {
                assertThat("bare LF at offset " + i, i > 0 && raw[i - 1] == '\r', is(true));
            }
        }
        assertThat(new String(raw, StandardCharsets.UTF_8), containsString("first line\r\nsecond line\r\nthird line"));
    }

    /**
     * Keycloak's subjects are localised: "Verifica dell'indirizzo email" and worse. The subject is
     * RFC 2047 encoded on the way out, and a double encoding — easy to introduce, since
     * {@code MimeMessage.setSubject} encodes too — would put a literal {@code =?UTF-8?Q?…?=} in front
     * of every non-English recipient.
     */
    @Test
    void keepsANonAsciiSubjectReadableAfterRfc2047Encoding() throws Exception {
        transport.respondWith(200, ACCEPTED);

        provider.send(realmWithSender(), "user@example.com", "Verifica dell'indirizzo email — è urgente",
                "Testo", null);

        MimeMessage message = mimeOf(transport.lastRequest());
        assertThat(message.getSubject(), is("Verifica dell'indirizzo email — è urgente"));
        assertThat(message.getHeader("Subject", null), startsWith("=?UTF-8?"));
    }

    /**
     * The overload Keycloak's own flows call. It must read the address off the user and nothing else:
     * the double below throws on every other {@link UserModel} method, so any extra access — a
     * locale lookup, an attribute read — fails the test instead of reaching a real user store from a
     * code path that has no session.
     */
    @Test
    void sendsToTheAddressCarriedByTheUserModel() throws Exception {
        transport.respondWith(200, ACCEPTED);

        provider.send(realmWithSender(), userWithEmail("user@example.com"), "Your account", "Testo", null);

        assertThat(transport.requestCount(), is(1));
        assertThat(mimeOf(transport.lastRequest()).getHeader("To", null), is("user@example.com"));
    }

    /**
     * A user with no email is an administrator's data problem, not an AWS one: it has to be named as
     * such before anything is signed or sent, or the operator gets an opaque SES rejection instead.
     */
    @Test
    void refusesToSendToAUserThatHasNoEmailAddress() {
        EmailException failure = assertThrows(EmailException.class,
                () -> provider.send(realmWithSender(), userWithEmail(null), "Your account", "Testo", null));

        assertThat(failure.getMessage(), is("No email address configured for the user"));
        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void refusesANullRecipientAddress() {
        EmailException failure = assertThrows(EmailException.class,
                () -> provider.send(realmWithSender(), (String) null, "Your account", "Testo", null));

        assertThat(failure.getMessage(), is("No recipient address configured"));
        assertThat(transport.requestCount(), is(0));
    }

    /**
     * An administrator who tuned the realm's mail timeouts under the SMTP sender keeps them after the
     * switch to SES. Getting this backwards would silently double or halve the time a login request
     * can block while an email is being sent.
     */
    @Test
    void prefersTheRealmTimeoutsOverTheServerWideOnes() throws Exception {
        transport.respondWith(200, ACCEPTED);
        Map<String, String> config = realmWithSender();
        config.put("connectionTimeout", "1500");
        config.put("timeout", "4000");

        provider.send(config, "user@example.com", "Your account", "Testo", null);

        assertThat(transport.lastRequest().connectTimeoutMillis(), is(1500));
        assertThat(transport.lastRequest().readTimeoutMillis(), is(4000));
    }

    @Test
    void usesTheServerWideTimeoutsWhenTheRealmConfiguresNone() throws Exception {
        transport.respondWith(200, ACCEPTED);

        provider.send(realmWithSender(), "user@example.com", "Your account", "Testo", null);

        assertThat(transport.lastRequest().connectTimeoutMillis(), is(SPI_CONNECT_TIMEOUT_MILLIS));
        assertThat(transport.lastRequest().readTimeoutMillis(), is(SPI_READ_TIMEOUT_MILLIS));
    }

    /**
     * The admin console writes an empty string for a cleared timeout field, and a realm imported from
     * JSON can carry anything at all. None of those may become the effective timeout: {@code 0} and a
     * negative value mean "wait forever" to an HTTP client, which would pin a Keycloak transaction
     * open for as long as the network takes to give up.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-a-number", "-1", "0"})
    void fallsBackToTheServerWideTimeoutsWhenTheRealmValueIsUnusable(String realmValue) throws Exception {
        transport.respondWith(200, ACCEPTED);
        Map<String, String> config = realmWithSender();
        config.put("connectionTimeout", realmValue);
        config.put("timeout", realmValue);

        provider.send(config, "user@example.com", "Your account", "Testo", null);

        assertThat(transport.lastRequest().connectTimeoutMillis(), is(SPI_CONNECT_TIMEOUT_MILLIS));
        assertThat(transport.lastRequest().readTimeoutMillis(), is(SPI_READ_TIMEOUT_MILLIS));
    }

    /**
     * The realm's {@code envelopeFrom} is its bounce address. {@code FeedbackForwardingEmailAddress}
     * is the SES parameter with that meaning; dropping it would send bounces and complaints to the
     * sending identity instead of to the mailbox the realm nominates.
     */
    @Test
    void passesTheRealmEnvelopeFromAsTheSesFeedbackForwardingAddress() throws Exception {
        transport.respondWith(200, ACCEPTED);
        Map<String, String> config = realmWithSender();
        config.put("envelopeFrom", "bounces@example.com");

        provider.send(config, "user@example.com", "Your account", "Testo", null);

        assertThat(payloadOf(transport.lastRequest()).get("FeedbackForwardingEmailAddress").asText(),
                is("bounces@example.com"));
    }

    /**
     * Absent, not null and not empty: SES validates the parameter when it is present, so sending
     * {@code ""} for a realm that never set an envelope-from turns every email into a rejection.
     */
    @Test
    void omitsTheFeedbackForwardingAddressWhenTheRealmSetsNoEnvelopeFrom() throws Exception {
        transport.respondWith(200, ACCEPTED);
        Map<String, String> config = realmWithSender();
        config.put("envelopeFrom", "");

        provider.send(config, "user@example.com", "Your account", "Testo", null);

        assertThat(payloadOf(transport.lastRequest()).has("FeedbackForwardingEmailAddress"), is(false));
    }

    /**
     * The envelope-from is validated on the send path, not only in {@code validate(Map)}: servers
     * older than 26.2.8 never call that method, so this is the only check a malformed bounce address
     * gets before it reaches AWS as an opaque rejection.
     */
    @Test
    void refusesAMalformedEnvelopeFromBeforeSending() {
        Map<String, String> config = realmWithSender();
        config.put("envelopeFrom", "not-an-address");

        EmailException failure = assertThrows(EmailException.class,
                () -> provider.send(config, "user@example.com", "Your account", "Testo", null));

        assertThat(failure.getMessage(), containsString("Invalid envelope-from address"));
        assertThat(transport.requestCount(), is(0));
    }

    /** And it is converted like every other address, rather than sent to SES with a non-ASCII domain. */
    @Test
    void punycodesTheEnvelopeFromDomain() throws Exception {
        transport.respondWith(200, ACCEPTED);
        Map<String, String> config = realmWithSender();
        config.put("envelopeFrom", "bounces@società.it");

        provider.send(config, "user@example.com", "Your account", "Testo", null);

        assertThat(payloadOf(transport.lastRequest()).get("FeedbackForwardingEmailAddress").asText(),
                is("bounces@xn--societ-nta.it"));
    }

    /**
     * What an administrator sees when SES refuses. The three things that make the failure actionable
     * — the AWS exception name, AWS's own text and the request id AWS support traces — all have to
     * survive into the {@link EmailException}, because that message is the whole of what the admin
     * console shows.
     */
    @Test
    void reportsASesRefusalAsAnActionableEmailException() {
        transport.respondWith(400,
                Map.of("x-amzn-ErrorType", "MessageRejected:8f9e1c0a-1b2c-4d3e-9f8a-0123456789ab",
                        "x-amzn-RequestId", "8f9e1c0a-1b2c-4d3e-9f8a-0123456789ab"),
                "{\"message\":\"Email address is not verified.\"}");

        EmailException failure = assertThrows(EmailException.class,
                () -> provider.send(realmWithSender(), "user@example.com", "Your account", "Testo", null));

        assertThat(failure.getMessage(), is("Amazon SES rejected the message (HTTP 400, MessageRejected):"
                + " Email address is not verified. [aws-request-id: 8f9e1c0a-1b2c-4d3e-9f8a-0123456789ab]"));
    }

    /**
     * A send that died on the wire must not be replayed. SES {@code SendEmail} has no idempotency
     * token, so a retry of a request AWS may already have accepted is a second activation link in a
     * real person's inbox.
     */
    @Test
    void doesNotRetryASendThatFailedOnTheWire() {
        transport.failWith(new IOException("Connection reset"));

        EmailException failure = assertThrows(EmailException.class,
                () -> provider.send(realmWithSender(), "user@example.com", "Your account", "Testo", null));

        assertThat(failure.getMessage(),
                is("Could not reach Amazon SES at email.eu-central-1.amazonaws.com: Connection reset"));
        assertThat(transport.requestCount(), is(1));
    }

    /**
     * An SES realm has no host, port, user or password — those fields are meaningless over the API
     * and an administrator leaves them empty. If {@code validate} demanded them, the admin console
     * would refuse to save every legitimate SES realm.
     */
    @Test
    void acceptsARealmThatConfiguresNothingButTheSenderAddress() throws Exception {
        provider.validate(Map.of("from", "noreply@example.com", "fromDisplayName", "Example"));

        assertThat(transport.requestCount(), is(0));
    }

    /**
     * {@code validate} runs on the realm-update request path, where an administrator is waiting for a
     * save to return. A network call there would make saving a realm depend on SES being reachable —
     * and on the server holding usable AWS credentials, which a validation has no business needing.
     */
    @Test
    void validatesWithoutTouchingTheNetwork() throws Exception {
        Map<String, String> config = realmWithSender();
        config.put("replyTo", "support@example.com");
        config.put("envelopeFrom", "bounces@example.com");

        provider.validate(config);

        assertThat(transport.requestCount(), is(0));
    }

    @Test
    void rejectsARealmWithNoSenderAddress() {
        EmailException failure = assertThrows(EmailException.class, () -> provider.validate(Map.of()));

        assertThat(failure.getMessage(), is("No sender address configured"));
    }

    @Test
    void rejectsARealmWhoseSenderAddressIsMalformed() {
        EmailException failure = assertThrows(EmailException.class,
                () -> provider.validate(Map.of("from", "noreply@")));

        assertThat(failure.getMessage(), is("Invalid sender address 'noreply@'"));
    }

    @Test
    void rejectsARealmWhoseReplyToAddressIsMalformed() {
        Map<String, String> config = realmWithSender();
        config.put("replyTo", "support at example.com");

        EmailException failure = assertThrows(EmailException.class, () -> provider.validate(config));

        assertThat(failure.getMessage(), is("Invalid reply-to address 'support at example.com'"));
    }

    @Test
    void rejectsARealmWhoseEnvelopeFromAddressIsMalformed() {
        Map<String, String> config = realmWithSender();
        config.put("envelopeFrom", "bounces@@example.com");

        EmailException failure = assertThrows(EmailException.class, () -> provider.validate(config));

        assertThat(failure.getMessage(), is("Invalid envelope-from address 'bounces@@example.com'"));
    }

    /**
     * The HTTP client is Keycloak's, shared with everything else the server calls out to. Closing it
     * when a per-request provider is closed would break the first email and every outbound call after
     * it; sending again after {@code close()} is the observable form of "nothing was shut down".
     */
    @Test
    void keepsSendingAfterCloseBecauseTheHttpClientIsNotItsToClose() throws Exception {
        transport.respondWith(200, ACCEPTED);

        provider.close();
        provider.send(realmWithSender(), "user@example.com", "Your account", "Testo", null);

        assertThat(transport.requestCount(), is(1));
    }

    private static Map<String, String> realmWithSender() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("from", "noreply@example.com");
        config.put("fromDisplayName", "Example");
        return config;
    }

    /**
     * A {@link UserModel} that answers {@code getEmail()} and refuses everything else: the inherited
     * delegate is {@code null}, so any other call throws. That is the assertion — no mocking library
     * is on this classpath, and none is needed to state "only the address is read".
     */
    private static UserModel userWithEmail(String email) {
        return new UserModelDelegate(null) {
            @Override
            public String getEmail() {
                return email;
            }
        };
    }

    private static JsonNode payloadOf(AwsHttpRequest request) throws IOException {
        return JsonSerialization.mapper.readTree(request.body());
    }

    private static byte[] rawMimeOf(AwsHttpRequest request) throws IOException {
        return Base64.getDecoder().decode(payloadOf(request).get("Content").get("Raw").get("Data").asText());
    }

    private static MimeMessage mimeOf(AwsHttpRequest request) throws Exception {
        return new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(rawMimeOf(request)));
    }

    /**
     * Credentials fixed in the test rather than resolved from the chain: the real chain reads the
     * process environment and the filesystem, neither of which a build machine may be assumed to have
     * — or to lack.
     */
    private static final class FixedCredentials implements AwsCredentialsProvider {

        @Override
        public AwsCredentials resolve(AwsHttpTransport transport) {
            return AwsCredentials.of(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        }

        @Override
        public String name() {
            return "test credentials";
        }
    }
}
