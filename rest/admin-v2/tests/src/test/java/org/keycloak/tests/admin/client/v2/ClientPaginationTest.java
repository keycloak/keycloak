package org.keycloak.tests.admin.client.v2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.api.ListOptions;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = ClientPaginationTest.Config.class)
public class ClientPaginationTest extends AbstractClientApiV2Test {

    private static final int PAGINATION_TEST_CLIENT_COUNT = 110;
    private static final String PAGINATION_CLIENT_PREFIX = "pagination-test-";
    private static final String PAGINATION_QUERY_PREFIX = "pagination-query-";

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectRealm(config = PaginationRealmConfig.class)
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

    public static class PaginationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm;
        }
    }

    @BeforeEach
    public void setupPaginationTestClients() {
        var clientsApi = getClientsApi();
        IntStream.range(0, PAGINATION_TEST_CLIENT_COUNT).forEach(i -> {
            String clientId = PAGINATION_CLIENT_PREFIX + i;
            if (!testRealm.admin().clients().findByClientId(clientId).isEmpty()) {
                return;
            }
            var client = new OIDCClientRepresentation(clientId);
            client.setEnabled(true);
            client.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));
            client.setRedirectUris(Set.of("http://localhost/callback"));
            try (var response = clientsApi.createClient(client)) {
                assertThat(response.getStatus(), is(201));
                var created = response.readEntity(OIDCClientRepresentation.class);
                testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
            }
        });

        IntStream.range(0, 5).forEach(i -> {
            String clientId = PAGINATION_QUERY_PREFIX + i;
            if (!testRealm.admin().clients().findByClientId(clientId).isEmpty()) {
                return;
            }
            var client = new OIDCClientRepresentation(clientId);
            client.setEnabled(true);
            client.setDescription("pagination query test client");
            client.setLoginFlows(Set.of(OIDCClientRepresentation.Flow.STANDARD));
            client.setRedirectUris(Set.of("http://localhost/callback"));
            try (var response = clientsApi.createClient(client)) {
                assertThat(response.getStatus(), is(201));
                var created = response.readEntity(OIDCClientRepresentation.class);
                testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
            }
        });
    }

    @Test
    public void defaultLimitReturns100Clients() {
        try (var stream = getClientsApi().getClients()) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients, hasSize(100));
        }
    }

    @Test
    public void limitAndOffsetReturnPage() {
        try (var stream = getClientsApi().getClients(new ListOptions().offset(0).limit(10))) {
            assertThat(stream.toList(), hasSize(10));
        }

        try (var stream = getClientsApi().getClients(new ListOptions().offset(100).limit(50))) {
            List<BaseClientRepresentation> clients = stream.toList();
            assertThat(clients.size(), greaterThanOrEqualTo(1));
            assertThat(clients.size(), lessThanOrEqualTo(50));
        }
    }

    @Test
    public void limitAboveMaximumIsCapped() {
        try (var stream = getClientsApi().getClients(new ListOptions().limit(200))) {
            assertThat(stream.toList(), hasSize(121));
        }
    }

    @Test
    public void negativeOffsetIsRejected() {
        assertThrows(BadRequestException.class,
                () -> getClientsApi().getClients(new ListOptions().offset(-1)));
    }

    @Test
    public void negativeLimitIsRejected() {
        assertThrows(BadRequestException.class,
                () -> getClientsApi().getClients(new ListOptions().limit(-1)));
    }

    @Test
    public void negativeOffsetViaHttpIsRejected() throws IOException {
        HttpGet request = new HttpGet(getClientsApiUrl() + "?offset=-1");
        setAuthHeader(request);
        try (var response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
        }
    }

    @Test
    public void queryWithPagination() throws IOException {
        String query = "clientId sw \"" + PAGINATION_QUERY_PREFIX + "\"";
        List<BaseClientRepresentation> page = queryClients(query, 0, 2);
        assertThat(page, hasSize(2));

        List<BaseClientRepresentation> nextPage = queryClients(query, 2, 2);
        assertThat(nextPage, hasSize(2));
        assertThat(page.get(0).getClientId(), is(not(nextPage.get(0).getClientId())));
        // test that the last client of the first page is not the first client on the next page
        assertThat(page.get(1).getClientId(), is(not(nextPage.get(0).getClientId())));
    }

    @Test
    public void queryWithPaginationBeyondResultsReturnsEmpty() throws IOException {
        String query = "clientId sw \"" + PAGINATION_QUERY_PREFIX + "\"";
        List<BaseClientRepresentation> page = queryClients(query, 100, 10);
        assertThat(page, empty());
    }

    private List<BaseClientRepresentation> queryClients(String query, int offset, int limit) throws IOException {
        String url = getClientsApiUrl()
                + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&offset=" + offset
                + "&limit=" + limit;
        HttpGet request = new HttpGet(url);
        setAuthHeader(request);
        try (var response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String body = EntityUtils.toString(response.getEntity());
            return mapper.readValue(body, new TypeReference<>() {});
        }
    }
}
