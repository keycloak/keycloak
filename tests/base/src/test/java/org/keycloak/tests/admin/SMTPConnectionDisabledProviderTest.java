package org.keycloak.tests.admin;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = SMTPConnectionDisabledProviderTest.SMTPDisabledProviderConfig.class)
public class SMTPConnectionDisabledProviderTest {

    @InjectRealm(config = SMTPConnectionTest.SMTPRealmWithClientAndUser.class)
    private ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    private Keycloak adminClient;

    @Test
    public void testUpdateRealmWithDisabledEmailSenderProviderReturns400() {
        RealmResource realmResource = adminClient.realms().realm(managedRealm.getName());
        RealmRepresentation realmRep = realmResource.toRepresentation();
        
        Map<String, String> smtpSettings = new HashMap<>();
        smtpSettings.put("host", "127.0.0.1");
        smtpSettings.put("port", "3025");
        smtpSettings.put("from", "auto@keycloak.org");
        realmRep.setSmtpServer(smtpSettings);
        
        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> realmResource.update(realmRep));
        Assertions.assertNotNull(exception.getResponse(), "Response should not be null");
        String body = exception.getResponse().readEntity(String.class);
        Assertions.assertTrue(body.contains("Email sender provider is disabled or not configured"), "Message should indicate disabled provider. Found: " + body);
    }

    @Test
    public void testSMTPConnectionWithDisabledEmailSenderProviderReturns400() throws Exception {
        Map<String, String> settings = new HashMap<>();
        settings.put("host", "127.0.0.1");
        settings.put("port", "3025");
        settings.put("from", "auto@keycloak.org");

        Response response = adminClient.realms().realm(managedRealm.getName()).testSMTPConnection(settings);
        Assertions.assertEquals(400, response.getStatus());
        String body = response.readEntity(String.class);
        Assertions.assertTrue(body.contains("Email sender provider is disabled or not configured"), "Message should indicate disabled provider. Found: " + body);
        response.close();
    }

    public static class SMTPDisabledProviderConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-email-sender-default-enabled", "false");
        }
    }
}
