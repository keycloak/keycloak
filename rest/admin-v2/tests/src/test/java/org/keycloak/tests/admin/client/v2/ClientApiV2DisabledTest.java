package org.keycloak.tests.admin.client.v2;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.tests.admin.client.v2.ClientApiV2Test.HOSTNAME_LOCAL_ADMIN;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest
public class ClientApiV2DisabledTest {
    @InjectHttpClient
    CloseableHttpClient client;

    @Test
    public void getClient() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/account");
        try (var response = client.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode());
        }
    }
}
