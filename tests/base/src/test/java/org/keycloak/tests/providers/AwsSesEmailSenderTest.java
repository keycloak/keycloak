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
package org.keycloak.tests.providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.WebApplicationException;

import org.keycloak.email.aws.AwsSesEmailSenderProviderFactory;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end test of the Amazon SES email sender against a stub of the SES v2 API.
 * <p>
 * A real Keycloak boots with {@code --spi-email-sender--provider=aws-ses}, an administrator triggers
 * a real execute-actions email, and the assertions are made on the bytes that left the server. No AWS
 * account, no credentials and no network are involved: the provider's {@code endpoint} option points
 * SES at the test framework's shared HTTP server, and the AWS credential chain is fed a throwaway key
 * pair through the two JVM system properties that sit first in it.
 * <p>
 * What this covers that the unit tests cannot: that the provider is reachable through Keycloak's own
 * provider resolution, that it signs with the right credential scope, and that the message Keycloak
 * composed survives the round trip to the wire as a valid MIME document.
 */
@KeycloakIntegrationTest(config = AwsSesEmailSenderTest.AwsSesServerConfig.class)
public class AwsSesEmailSenderTest {

    /**
     * The injected HTTP server is a global singleton shared by every test class, so the context path
     * has to be unique and has to be removed again after each test.
     */
    private static final String STUB_CONTEXT_PATH = "/aws-ses-integration-test";

    /**
     * The framework's server binds 127.0.0.1:8500. The address cannot be read from the injected
     * instance here because the server configuration is built before injection happens, so it is
     * repeated as a constant and checked against the real one in {@link #registerSesStub()}.
     */
    private static final String STUB_AUTHORITY = "127.0.0.1:8500";
    private static final String SES_ENDPOINT = "http://" + STUB_AUTHORITY + STUB_CONTEXT_PATH;

    private static final String REGION = "eu-central-1";

    // The example key pair from the AWS documentation: valid in shape, useless as a credential.
    private static final String ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    private static final String FROM = "no-reply@keycloak.org";
    private static final String FROM_DISPLAY_NAME = "Keycloak SES";
    private static final String RECIPIENT = "ses-recipient@keycloak.org";

    private static final String MESSAGE_ID = "0100018f-aws-ses-it";

    @InjectRealm(config = SesRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = SesUserConfig.class)
    ManagedUser user;

    @InjectHttpServer
    HttpServer httpServer;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private final List<RecordedRequest> recorded = Collections.synchronizedList(new ArrayList<>());

    private volatile HttpHandler responder = AwsSesEmailSenderTest::sendAccepted;

    @BeforeEach
    public void registerSesStub() {
        // Credentials are deliberately not a server option — the provider has none, so that no secret
        // can end up in Keycloak's configuration — so they have to arrive through the standard AWS
        // chain, whose first link is these two JVM system properties. They are set inside the server's
        // own JVM rather than this one because the framework's default managed server is the
        // distribution, a separate process that does not share this JVM's properties or environment.
        runOnServer.run(session -> {
            System.setProperty("aws.accessKeyId", ACCESS_KEY_ID);
            System.setProperty("aws.secretAccessKey", SECRET_ACCESS_KEY);
        });

        assertThat("the SES endpoint option must point at the injected HTTP server",
                httpServer.getAddress().getHostString() + ":" + httpServer.getAddress().getPort(),
                equalTo(STUB_AUTHORITY));

        httpServer.createContext(STUB_CONTEXT_PATH, exchange -> {
            recorded.add(RecordedRequest.read(exchange));
            responder.handle(exchange);
        });
    }

    @AfterEach
    public void unregisterSesStub() {
        httpServer.removeContext(STUB_CONTEXT_PATH);
        runOnServer.run(session -> {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        });
    }

    @Test
    public void sendsSignedRawMessageToSesEndpoint() throws Exception {
        executeActionsEmail();

        assertThat(recorded, hasSize(1));
        RecordedRequest request = recorded.get(0);
        assertThat(request.method(), equalTo("POST"));
        assertThat(request.path(), endsWith("/v2/email/outbound-emails"));

        String authorization = request.headers().getFirst("Authorization");
        assertThat(authorization, startsWith("AWS4-HMAC-SHA256 Credential="));
        // The signing service is "ses", not the "email" hostname prefix; getting it wrong is a silent
        // 403 in production.
        assertThat(authorization, containsString("/" + REGION + "/ses/aws4_request"));
        assertThat(request.headers().getFirst("X-Amz-Date"), notNullValue());

        JsonNode payload = JsonSerialization.mapper.readTree(request.body());
        byte[] mime = Base64.getDecoder().decode(payload.get("Content").get("Raw").get("Data").asText());

        assertThat("SES refuses a raw message containing a bare LF", indexOfBareLineFeed(mime), equalTo(-1));

        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(mime));
        assertThat(message.getHeader("To", null), equalTo(RECIPIENT));
        assertThat(message.getContentType(), startsWith("multipart/alternative"));

        String from = message.getHeader("From", null);
        InternetAddress fromAddress = InternetAddress.parse(from)[0];
        assertThat(fromAddress.getAddress(), equalTo(FROM));
        assertThat(fromAddress.getPersonal(), equalTo(FROM_DISPLAY_NAME));

        // AWS never documents which identity wins when the envelope sender and the header disagree,
        // so the provider derives both from one address; this is the assertion that keeps it that way.
        assertThat(payload.get("FromEmailAddress").asText(), equalTo(from));
    }

    @Test
    public void failsTheAdminOperationWhenSesRejectsTheMessage() {
        responder = AwsSesEmailSenderTest::sendRejected;

        WebApplicationException failure = assertThrows(WebApplicationException.class, this::executeActionsEmail);

        assertThat(failure.getResponse().getStatus(), equalTo(500));
        assertThat(failure.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage(),
                allOf(containsString("MessageRejected"), containsString("Email address is not verified.")));

        // SES SendEmail has no idempotency token, so a refused send must not be replayed into a second
        // message in a real inbox.
        assertThat(recorded, hasSize(1));
    }

    private void executeActionsEmail() {
        realm.admin().users().get(user.getId())
                .executeActionsEmail(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
    }

    private static void sendAccepted(HttpExchange exchange) throws IOException {
        respond(exchange, 200, Map.of("Content-Type", "application/json"),
                "{\"MessageId\":\"" + MESSAGE_ID + "\"}");
    }

    private static void sendRejected(HttpExchange exchange) throws IOException {
        respond(exchange, 400, Map.of("Content-Type", "application/json", "x-amzn-ErrorType", "MessageRejected"),
                "{\"message\":\"Email address is not verified.\"}");
    }

    /**
     * Not {@code HttpServerUtil.sendResponse}: that helper sets the response headers after
     * {@code sendResponseHeaders}, by which point they are no longer sent — and this test depends on
     * {@code x-amzn-ErrorType} reaching the client.
     */
    private static void respond(HttpExchange exchange, int status, Map<String, String> headers, String body)
            throws IOException {
        headers.forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } finally {
            exchange.close();
        }
    }

    /** @return the index of the first {@code 0x0A} not preceded by a {@code 0x0D}, or -1 if there is none */
    private static int indexOfBareLineFeed(byte[] mime) {
        for (int i = 0; i < mime.length; i++) {
            if (mime[i] == '\n' && (i == 0 || mime[i - 1] != '\r')) {
                return i;
            }
        }
        return -1;
    }

    private record RecordedRequest(String method, String path, Headers headers, byte[] body) {

        static RecordedRequest read(HttpExchange exchange) throws IOException {
            Headers headers = new Headers();
            headers.putAll(exchange.getRequestHeaders());
            try (InputStream in = exchange.getRequestBody()) {
                return new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        headers, in.readAllBytes());
            }
        }
    }

    public static class AwsSesServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config
                    .option("spi-email-sender--provider", AwsSesEmailSenderProviderFactory.PROVIDER_ID)
                    .spiOption("email-sender", AwsSesEmailSenderProviderFactory.PROVIDER_ID, "region", REGION)
                    .spiOption("email-sender", AwsSesEmailSenderProviderFactory.PROVIDER_ID, "endpoint", SES_ENDPOINT);
        }
    }

    public static class SesRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // The SES sender takes the sender address and display name from the realm's existing SMTP
            // block, exactly as the SMTP sender does; host and port have no meaning for it.
            return realm.update(rep -> rep.setSmtpServer(Map.of("from", FROM, "fromDisplayName", FROM_DISPLAY_NAME)));
        }
    }

    public static class SesUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("ses-recipient").name("Ses", "Recipient").email(RECIPIENT);
        }
    }
}
