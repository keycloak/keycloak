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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = SMTPConnectionVaultTest.SMTPVaultConfig.class)
public class SMTPConnectionVaultTest {

    @InjectRealm(config = SMTPConnectionTest.SMTPRealmWithClientAndUser.class)
    private ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    private Keycloak adminClient;

    @InjectMailServer
    private MailServer mailServer;

    @Test
    public void testWithAuthEnabledValidCredentials() throws Exception {
        // The value of password must match the value of vaultPassword stored in the vault file: resources/vault/default_smtp__password.
        // Prefix default in the file default_smtp__password is the name of the managed realm.
        String password = "admin";
        String vaultPassword = "${vault.smtp_password}";

        mailServer.credentials("admin@localhost", password);

        Map<String, String> settings = new HashMap<>();
        settings.put("host", "127.0.0.1");
        settings.put("port", "3025");
        settings.put("from", "auto@keycloak.org");
        settings.put("auth", "true");
        settings.put("ssl", null);
        settings.put("starttls", null);
        settings.put("user", "admin@localhost");
        settings.put("password", vaultPassword);
        settings.put("replyTo", "");
        settings.put("envelopeFrom", "");

        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings);
        assertEquals(204, response.getStatus());
        response.close();
    }

    public static class SMTPVaultConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            URL url = SMTPConnectionVaultTest.class.getResource("vault");
            if (url == null) {
                throw new RuntimeException("Unable to find the vault folder in the classpath for the default_smtp__password file!");
            }
            return config.option("vault", "file").option("vault-dir", url.getPath());
        }
    }
}
