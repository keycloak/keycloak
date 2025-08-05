package org.keycloak.tests.admin.api.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.api.client.ClientsApiSpi;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.admin.client.TestClientsApiFactory;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = CustomClientsApiTest.CustomClientsProvider.class)
public class CustomClientsApiTest {
    private static final String HOSTNAME_LOCAL_ADMIN = "http://localhost:8080/admin/api/v2";
    private static ObjectMapper mapper;

    @InjectHttpClient
    CloseableHttpClient client;

    @BeforeAll
    public static void setupMapper() {
        mapper = new ObjectMapper();
    }

    @Test
    public void getClients() throws Exception {
        HttpGet request = new HttpGet(HOSTNAME_LOCAL_ADMIN + "/realms/master/clients/");
        try (var response = client.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            var clients = (List<ClientRepresentation>) mapper
                    .createParser(response.getEntity().getContent())
                    .readValueAs(new TypeReference<List<ClientRepresentation>>() {
                    });

            assertThat(clients, notNullValue());
            assertThat(clients.size(), is(1));
            var client = clients.get(0);
            assertThat(client.getClientId(), is("test"));
            assertThat(client.getDisplayName(), is("testCdi"));
        }
    }

    public static class CustomClientsProvider implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.option("spi-%s--provider".formatted(ClientsApiSpi.NAME), TestClientsApiFactory.PROVIDER_ID);
            builder.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
            return builder;
        }
    }
}
