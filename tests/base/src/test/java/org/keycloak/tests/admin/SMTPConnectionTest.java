/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.keycloak.representations.idm.ComponentRepresentation.SECRET_VALUE;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@KeycloakIntegrationTest
public class SMTPConnectionTest {

    @InjectRealm(config = SMTPRealmWithClientAndUser.class)
    private ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    private Keycloak adminClient;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectMailServer
    private MailServer mailServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    @Order(1)
    public void testWithNullSettings() throws Exception {
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings(null, null, null, null, null, null, null, null));
        assertStatus(response, 500);
    }

    @Test
    @Order(2)
    public void testWithProperSettings() throws Exception {
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", null, null, null, null, null));
        assertStatus(response, 204);
        assertMailReceived();
    }

    @Test
    @Order(3)
    public void testWithAuthEnabledCredentialsEmpty() throws Exception {
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null, null, null));
        assertStatus(response, 500);
    }

    @Test
    @Order(4)
    public void testWithAuthEnabledValidCredentials() throws Exception {
        String password = "admin";

        mailServer.credentials("admin@localhost", password);
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null, "admin@localhost", password));
        assertStatus(response, 204);
    }

    @Test
    @Order(5)
    public void testAuthEnabledAndSavedCredentials() throws Exception {
        String password = "admin";
        RealmResource realm = adminClient.realms().realm(managedRealm.getName());

        RealmRepresentation realmRep = realm.toRepresentation();
        realmRep.setSmtpServer(smtpMap("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", password, null, null, null));
        managedRealm.updateWithCleanup(r -> r.update(realmRep));

        mailServer.credentials("admin@localhost", password);
        Response response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", SECRET_VALUE));
        assertStatus(response, 204);

        // no reuse password if the server is different (localhost) to the saved one (127.0.0.1)
        mailServer.credentials("admin@localhost", password);
        response = realm.testSMTPConnection(settings("localhost", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", SECRET_VALUE));
        assertStatus(response, 500);
    }

    @Test
    @Order(6)
    public void testWithTokenAuthEnabledAndTokenCacheAndSavedCredentials() throws Exception {
        final var realm = adminClient.realms().realm(managedRealm.getName());

        final var realmRep = realm.toRepresentation();
        realmRep.setSmtpServer(smtpMapForTokenAuth("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", keycloakUrls.getToken(managedRealm.getName()), "test-smtp-client-I", "secret", "basic", null, null));
        managedRealm.updateWithCleanup(r -> RealmConfigBuilder.update(realmRep));

        //verify token sent to smtp
        mailServer.credentials("admin@localhost", token -> {
            var accessToken = oAuthClient.verifyToken(token, AccessToken.class);
            return accessToken.isActive();
        });

        final var firstResponse = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", keycloakUrls.getToken(managedRealm.getName()), "test-smtp-client-I", SECRET_VALUE, "basic"));

        assertStatus(firstResponse, 204);
        assertMailReceived();
        assertClientLoginEventsCountAndClear(realm, "test-smtp-client-I", 1);
        assertEventsEmpty(realm);

        final var secondResponse = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", keycloakUrls.getToken(managedRealm.getName()), "test-smtp-client-I", SECRET_VALUE, "basic"));

        assertStatus(secondResponse, 204);
        assertMailReceived();
        assertEventsEmpty(realm);

        // no reuse password if the server is different (localhost) to the saved one (127.0.0.1)
        final var thirdResponse = realm.testSMTPConnection(settings("localhost", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", keycloakUrls.getToken(managedRealm.getName()), "test-smtp-client-I", SECRET_VALUE, "basic"));
        assertStatus(thirdResponse, 500);
    }

    @Test
    @Order(7)
    public void testWithTokenAuthEnabledRetryGivesUp() throws Exception {
        RealmResource realm = adminClient.realms().realm(managedRealm.getName());
        RealmRepresentation realmRep = realm.toRepresentation();

        //decline token sent to smtp
        mailServer.credentials("admin@localhost", token -> false);

        final var response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", keycloakUrls.getToken(managedRealm.getName()), "test-smtp-client-II", "secret", "basic"));

        assertStatus(response, 500);
        assertMailNotReceived();
        assertClientLoginEventsCountAndClear(realm, "test-smtp-client-II", 2);
        assertEventsEmpty(realm);

    }

    @Test
    @Order(8)
    public void testWithTokenAuthEnabledAndRetryWithValidTokenInSecondTry() throws Exception {
        final var realm = adminClient.realms().realm(managedRealm.getName());

        final List<AccessToken> tempAccessToken = new ArrayList<>();

        //decline token sent to smtp
        mailServer.credentials("admin@localhost", token -> {
            var accessToken = oAuthClient
                    .verifyToken(token, AccessToken.class);
            if (tempAccessToken.isEmpty()) {
                tempAccessToken.add(accessToken);
                // even a valid token is declined for the test
                return false;
            } else {
                // make sure retry created a new token
                Assertions.assertNotEquals(tempAccessToken.stream().findFirst().orElseThrow().getId(),accessToken.getId());
                tempAccessToken.clear();
                return accessToken.isActive();
            }

        });

        final var response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", keycloakUrls.getToken(managedRealm.getName()), "test-smtp-client-III", "secret", "basic"));

        assertStatus(response, 204);
        assertMailReceived();
        assertClientLoginEventsCountAndClear(realm, "test-smtp-client-III", 2);
        assertEventsEmpty(realm);

    }

    @Test
    @Order(9)
    public void testAllowUTF8() throws Exception {
        // utf8 on from not allowed if allowutf8 not enabled
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "autoñ@keycloak.org",
                null, null, null, null, null));
        assertStatus(response, 500);

        // utf-8 on from but in domain part is allowed and transformed
        response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloakñ.org",
                null, null, null, null, null));
        assertStatus(response, 204);
        assertMailReceived("auto@xn--keycloak-k3a.org");

        // utf8 on from allowed if allowutf8 enabled
        response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(smtpMap("127.0.0.1", "3025", "autoñ@keycloak.org",
                null, null, null, null, null, null, null, "true"));
        assertStatus(response, 204);
        assertMailReceived();

        // utf8 on address
        RealmResource realmRes = adminClient.realms().realm(managedRealm.getName());
        RealmRepresentation realmRep = realmRes.toRepresentation();
        realmRep.getSmtpServer().put(EmailSenderProvider.CONFIG_ALLOW_UTF8, Boolean.TRUE.toString());
        realmRes.update(realmRep);

        AccessToken token = oAuthClient.parseToken(adminClient.tokenManager().getAccessToken().getToken(), AccessToken.class);
        UserResource userRes = adminClient.realm("default").users().get(token.getSubject());
        UserRepresentation userRep = userRes.toRepresentation();
        final String previousEmail = userRep.getEmail();
        userRep.setEmail("adminñ@localhost");
        userRes.update(userRep);

        try {
            // not allowed on address if allowutf8 not enabled
            response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org",
                    null, null, null, null, null));
            assertStatus(response, 500);

            // allowed on address if allowutf8 enabled
            response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(smtpMap("127.0.0.1", "3025", "auto@keycloak.org",
                    null, null, null, null, null, null, null, "true"));
            assertStatus(response, 204);
            assertMailReceived();
        } finally {
            userRep.setEmail(previousEmail);
            userRes.update(userRep);
            realmRep.getSmtpServer().remove(EmailSenderProvider.CONFIG_ALLOW_UTF8);
            realmRes.update(realmRep);
        }
    }

    private Map<String, String> settings(String host, String port, String from, String auth, String ssl, String starttls,
                                         String username, String password) throws Exception {
        return smtpMap(host, port, from, auth, ssl, starttls, username, password, "", "", null);
    }

    private Map<String, String> settings(String host, String port, String from, String auth, String ssl, String starttls,
                                         String username, String authTokenUrl, String authTokenClientId, String authTokenClientSecret, String authTokenScope) throws Exception {
        return smtpMapForTokenAuth(host, port, from, auth, ssl, starttls, username, authTokenUrl, authTokenClientId, authTokenClientSecret, authTokenScope,"", "");
    }

    private Map<String, String> smtpMapForTokenAuth(String host, String port, String from, String auth, String ssl, String starttls,
                                                    String username, String authTokenUrl, String authTokenClientId, String authTokenClientSecret, String authTokenScope, String replyTo, String envelopeFrom) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", port);
        config.put("from", from);
        config.put("ssl", ssl);
        config.put("starttls", starttls);
        config.put("user", username);
        config.put("auth", auth);
        config.put("authType", "token");
        config.put("authTokenUrl", authTokenUrl);
        config.put("authTokenClientId", authTokenClientId);
        config.put("authTokenClientSecret", authTokenClientSecret);
        config.put("authTokenScope", authTokenScope);
        config.put("replyTo", replyTo);
        config.put("envelopeFrom", envelopeFrom);
        return config;
    }

    private Map<String, String> smtpMap(String host, String port, String from, String auth, String ssl, String starttls,
                                        String username, String password, String replyTo, String envelopeFrom, String allowutf8) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", port);
        config.put("from", from);
        config.put("auth", auth);
        config.put("authType", "basic");
        config.put("ssl", ssl);
        config.put("starttls", starttls);
        config.put("user", username);
        config.put("password", password);
        config.put("replyTo", replyTo);
        config.put("envelopeFrom", envelopeFrom);
        if (allowutf8 != null) {
            config.put(EmailSenderProvider.CONFIG_ALLOW_UTF8, allowutf8);
        }
        return config;
    }

    private void assertClientLoginEventsCountAndClear(RealmResource realm, String clientId, int count) {
        var events = realm.getEvents();
        Assertions.assertEquals(count, events.stream().filter(e -> clientId.equals(e.getClientId())).count());
        events.stream().filter(e -> clientId.equals(e.getClientId())).forEach(event -> Assertions.assertEquals("CLIENT_LOGIN", event.getType()));
        realm.clearEvents();
    }

    private void assertEventsEmpty(RealmResource realm) {
        Assertions.assertTrue(realm.getEvents().isEmpty());
    }

    private void assertStatus(Response response, int status) {
        assertEquals(status, response.getStatus());
        response.close();
    }

    private void assertMailReceived(String... from) {
        if (mailServer.getReceivedMessages().length == 1) {
            try {
                MimeMessage message = mailServer.getReceivedMessages()[0];
                if (from.length > 0) {
                    assertArrayEquals(from, Arrays.stream(message.getFrom()).map(Address::toString).toArray(String[]::new));
                }
                assertEquals("[KEYCLOAK] - SMTP test message", message.getSubject());
                mailServer.runCleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fail("E-mail was not received");
        }
    }

    private void assertMailNotReceived() {
        assertEquals(0, mailServer.getReceivedMessages().length);
    }

    public static class SMTPRealmWithClientAndUser implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.eventsEnabled(true); //testing XOAUTH2 token caching behaviour

            realm.addClient("myclient")
                    .secret("mysecret")
                    .directAccessGrantsEnabled(true);

            //add client for token gathering (XOAUTH2)
            //reuse the same client does not work
            realm.addClient("test-smtp-client-I")
                    .secret("secret")
                    .serviceAccountsEnabled(true);
            realm.addClient("test-smtp-client-II")
                    .secret("secret")
                    .serviceAccountsEnabled(true);
            realm.addClient("test-smtp-client-III")
                    .secret("secret")
                    .serviceAccountsEnabled(true);

            realm.addUser("myadmin")
                    .name("My", "Admin")
                    .email("admin@localhost")
                    .emailVerified(true)
                    .password("myadmin")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }
}
