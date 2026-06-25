package org.keycloak.tests.admin.client.v2;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.keycloak.admin.api.ListOptions;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.services.client.ClientServiceFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ScimBackedClientQueryTest.Config.class)
public class ScimBackedClientQueryTest extends AbstractClientApiV2Test {

    private static final String SCIM_JPA_PREFIX = "scim-jpa-";
    private static final int SCIM_JPA_CLIENT_COUNT = 12;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectRealm
    ManagedRealm testRealm;

    @Override
    public String getRealmName() {
        return testRealm.getName();
    }

    public static class Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }

    @BeforeEach
    public void enableScimServiceAndSetupClients() {
        runOnServer.run(session -> System.setProperty(ClientServiceFactory.SCIM_SERVICE_ENABLED_PROPERTY, "true"));

        var clientsApi = getClientsApi();

        IntStream.range(0, SCIM_JPA_CLIENT_COUNT).forEach(i -> {
            String clientId = SCIM_JPA_PREFIX + i;
            if (!testRealm.admin().clients().findByClientId(clientId).isEmpty()) {
                return;
            }
            var client = new OIDCClientRepresentation(clientId);
            client.setEnabled(i % 2 == 0);
            client.setDisplayName("SCIM JPA Client " + i);
            client.setDescription("scim jpa test client " + i);
            client.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));
            client.setRedirectUris(Set.of("http://localhost/callback"));
            try (var response = clientsApi.createClient(client)) {
                assertThat(response.getStatus(), is(201));
                var created = response.readEntity(OIDCClientRepresentation.class);
                testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
            }
        });

        createClientIfMissing(clientsApi, buildOidcClient("scim-jpa-roles-test", Set.of("admin", "user")));
        createClientIfMissing(clientsApi, buildSamlClient("scim-jpa-saml-test", "SCIM SAML Client"));
    }

    @AfterEach
    public void disableScimService() {
        runOnServer.run(session -> System.clearProperty(ClientServiceFactory.SCIM_SERVICE_ENABLED_PROPERTY));
    }

    @Test
    public void queryWithPaginationAtDbLayer() {
        String query = "clientId sw \"" + SCIM_JPA_PREFIX + "\"";
        try (var page1 = getClientsApi().getClients(new ListOptions().query(query).offset(0).limit(2))) {
            List<BaseClientRepresentation> firstPage = page1.toList();
            assertThat(firstPage, hasSize(2));

            try (var page2 = getClientsApi().getClients(new ListOptions().query(query).offset(2).limit(2))) {
                List<BaseClientRepresentation> secondPage = page2.toList();
                assertThat(secondPage, hasSize(2));
                assertThat(firstPage.get(0).getClientId(), is(not(secondPage.get(0).getClientId())));
                assertThat(firstPage.get(1).getClientId(), is(not(secondPage.get(0).getClientId())));
            }
        }
    }

    @Test
    public void filterByClientId() {
        try (var stream = getClientsApi().getClients(
                new ListOptions().query("clientId eq \"scim-jpa-3\""))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, hasSize(1));
            assertThat(clients.get(0).getClientId(), is("scim-jpa-3"));
        }
    }

    @Test
    public void filterByProtocol() {
        try (var stream = getClientsApi().getClients(
                new ListOptions().query("protocol eq \"saml\""))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, not(empty()));
            assertTrue(clients.stream().allMatch(c -> c instanceof SAMLClientRepresentation));
        }
    }

    @Test
    public void filterByEnabled() {
        try (var stream = getClientsApi().getClients(
                new ListOptions().query("enabled eq false"))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, not(empty()));
            assertTrue(clients.stream().allMatch(c -> Boolean.FALSE.equals(c.getEnabled())));
        }
    }

    @Test
    public void filterByDisplayName() {
        try (var stream = getClientsApi().getClients(
                new ListOptions().query("displayName eq \"SCIM JPA Client 1\""))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, hasSize(1));
            assertThat(clients.get(0).getClientId(), is("scim-jpa-1"));
        }
    }

    @Test
    public void filterWithOrExpression() {
        try (var stream = getClientsApi().getClients(new ListOptions().query(
                "protocol eq \"saml\" or clientId eq \"scim-jpa-0\""))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, not(empty()));
            assertTrue(clients.stream().allMatch(c ->
                    c instanceof SAMLClientRepresentation || "scim-jpa-0".equals(c.getClientId())));
        }
    }

    @Test
    public void filterWithNotExpression() {
        try (var stream = getClientsApi().getClients(
                new ListOptions().query("not enabled eq false"))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, not(empty()));
            assertTrue(clients.stream().allMatch(c -> Boolean.TRUE.equals(c.getEnabled())));
        }
    }

    @Test
    public void fallbackForRolesQuery() {
        try (var stream = getClientsApi().getClients(
                new ListOptions().query("roles eq \"admin\" and roles eq \"user\""))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, hasSize(1));
            assertThat(clients.get(0).getClientId(), is("scim-jpa-roles-test"));
            assertTrue(clients.get(0).getRoles().containsAll(Set.of("admin", "user")));
        }
    }

    @Test
    public void projectionWithQuery() {
        try (var stream = getClientsApi().getClients(
                new ListOptions()
                        .query("clientId eq \"scim-jpa-2\"")
                        .fields(Set.of("clientId", "description")))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, hasSize(1));
            assertThat(clients.get(0).getClientId(), is("scim-jpa-2"));
            assertThat(clients.get(0).getDescription(), is("scim jpa test client 2"));
            assertNull(clients.get(0).getDisplayName());
            assertNull(clients.get(0).getEnabled());
        }
    }

    private void createClientIfMissing(org.keycloak.admin.api.client.ClientsApi clientsApi, BaseClientRepresentation client) {
        String clientId = client.getClientId();
        if (!testRealm.admin().clients().findByClientId(clientId).isEmpty()) {
            return;
        }
        try (var response = clientsApi.createClient(client)) {
            assertThat(response.getStatus(), is(201));
            var created = response.readEntity(BaseClientRepresentation.class);
            assertNotNull(created.getUuid());
            testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }
    }

    private OIDCClientRepresentation buildOidcClient(String clientId, Set<String> roles) {
        var client = new OIDCClientRepresentation(clientId);
        client.setEnabled(true);
        client.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));
        client.setRedirectUris(Set.of("http://localhost/callback"));
        client.setRoles(roles);
        return client;
    }

    private SAMLClientRepresentation buildSamlClient(String clientId, String displayName) {
        var client = new SAMLClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        client.setDisplayName(displayName);
        return client;
    }
}
