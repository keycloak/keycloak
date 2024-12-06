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

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.keycloak.representations.idm.ComponentRepresentation.SECRET_VALUE;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
@KeycloakIntegrationTest
public class SMTPConnectionTest {

    @InjectRealm(config = SMTPRealmWithClientAndUser.class)
    private ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    private Keycloak adminClient;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testWithNullSettings() throws Exception {
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings(null, null, null, null, null, null, null, null));
        assertStatus(response, 500);
    }

    @Test
    public void testWithProperSettings() throws Exception {
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", null, null, null, null, null));
        assertStatus(response, 204);
        assertMailReceived();
    }

    @Test
    public void testWithAuthEnabledCredentialsEmpty() throws Exception {
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null, null, null));
        assertStatus(response, 500);
    }

    @Test
    public void testWithAuthEnabledValidCredentials() throws Exception {
        String password = "admin";

        mailServer.credentials("admin@localhost", password);
        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null, "admin@localhost", password));
        assertStatus(response, 204);
    }

    @Test
    public void testAuthEnabledAndSavedCredentials() throws Exception {
        String password = "admin";
        RealmResource realm = adminClient.realms().realm(managedRealm.getName());

        RealmRepresentation realmRep = realm.toRepresentation();
        realmRep.setSmtpServer(smtpMap("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", password, null, null));
        managedRealm.updateWithCleanup(r -> r.update(realmRep));

        mailServer.credentials("admin@localhost", password);
        Response response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", SECRET_VALUE));
        assertStatus(response, 204);
    }

    private Map<String, String> settings(String host, String port, String from, String auth, String ssl, String starttls,
                                         String username, String password) throws Exception {
        return smtpMap(host, port, from, auth, ssl, starttls, username, password, "", "");
    }

    private Map<String, String> smtpMap(String host, String port, String from, String auth, String ssl, String starttls,
                                        String username, String password, String replyTo, String envelopeFrom) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", port);
        config.put("from", from);
        config.put("auth", auth);
        config.put("ssl", ssl);
        config.put("starttls", starttls);
        config.put("user", username);
        config.put("password", password);
        config.put("replyTo", replyTo);
        config.put("envelopeFrom", envelopeFrom);
        return config;
    }

    private void assertStatus(Response response, int status) {
        assertEquals(status, response.getStatus());
        response.close();
    }

    private void assertMailReceived() {
        if (mailServer.getReceivedMessages().length == 1) {
            try {
                MimeMessage message = mailServer.getReceivedMessages()[0];
                assertEquals("[KEYCLOAK] - SMTP test message", message.getSubject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fail("E-mail was not received");
        }
    }

    public static class SMTPRealmWithClientAndUser implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("myclient")
                    .secret("mysecret")
                    .directAccessGrants();

            realm.addUser("myadmin")
                    .name("My", "Admin")
                    .email("admin@localhost")
                    .emailVerified()
                    .password("myadmin")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN);

            return realm;
        }
    }
}
