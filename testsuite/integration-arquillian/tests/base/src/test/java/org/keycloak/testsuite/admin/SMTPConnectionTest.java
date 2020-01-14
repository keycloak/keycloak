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

package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.UserBuilder;

import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.representations.idm.ComponentRepresentation.SECRET_VALUE;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.util.JsonSerialization.writeValueAsPrettyString;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 */
public class SMTPConnectionTest extends AbstractKeycloakTest {

    public final String SMTP_PASSWORD = setSmtpPassword();

    @Rule
    public GreenMailRule greenMailRule = new GreenMailRule();
    private RealmResource realm;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    public String setSmtpPassword() {
        return "admin";
    }

    @Before
    public void before() {
        realm = adminClient.realm("master");
        List<UserRepresentation> admin = realm.users().search("admin", 0, 1);
        UserRepresentation user = UserBuilder.edit(admin.get(0)).email("admin@localhost").build();
        realm.users().get(user.getId()).update(user);
    }

    private String settings(String host, String port, String from, String auth, String ssl, String starttls,
                            String username, String password) throws Exception {
        Map<String, String> config = smtpMap(host, port, from, auth, ssl, starttls, username, password, "", "");
        return writeValueAsPrettyString(config);
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

    @Test
    public void testWithNullSettings() throws Exception {
        Response response = realm.testSMTPConnection(settings(null, null, null, null, null, null,
                null, null));
        assertStatus(response, 500);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testWithProperSettings() throws Exception {
        Response response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", null, null, null,
                null, null));
        assertStatus(response, 204);
        assertMailReceived();
    }

    @Test
    public void testWithAuthEnabledCredentialsEmpty() throws Exception {
        Response response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                null, null));
        assertStatus(response, 500);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testWithAuthEnabledValidCredentials() throws Exception {
        greenMailRule.credentials("admin@localhost", "admin");
        Response response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                "admin@localhost", SMTP_PASSWORD));
        assertStatus(response, 204);
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testAuthEnabledAndSavedCredentials() throws Exception {
        RealmRepresentation realmRep = realm.toRepresentation();
        Map<String, String> oldSmtp = realmRep.getSmtpServer();
        try {
            realmRep.setSmtpServer(smtpMap("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                    "admin@localhost", SMTP_PASSWORD, null, null));
            realm.update(realmRep);

            greenMailRule.credentials("admin@localhost", "admin");
            Response response = realm.testSMTPConnection(settings("127.0.0.1", "3025", "auto@keycloak.org", "true", null, null,
                    "admin@localhost", SECRET_VALUE));
            assertStatus(response, 204);
        } finally {
            // Revert SMTP back
            realmRep.setSmtpServer(oldSmtp);
            realm.update(realmRep);
        }
    }

    private void assertStatus(Response response, int status) {
        assertEquals(status, response.getStatus());
        response.close();
    }

    private void assertMailReceived() {
        if (greenMailRule.getReceivedMessages().length == 1) {
            try {
                MimeMessage message = greenMailRule.getReceivedMessages()[0];
                assertEquals("[KEYCLOAK] - SMTP test message", message.getSubject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fail("E-mail was not received");
        }
    }
}
