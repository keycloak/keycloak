package org.keycloak.tests.admin.client.v2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.wrapper.Clients;
import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ClientQueryTest.Config.class)
public class ClientQueryTest extends AbstractClientApiV2Test {

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @InjectRealm(config = QueryTestRealmConfig.class)
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

    public static class QueryTestRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm;
        }
    }

    @BeforeEach
    public void setupTestClients() {
        var clients = adminClient.clients(getRealmName(), Clients.class).v2();

        var oidcClient = new OIDCClientRepresentation("query-test-oidc");
        oidcClient.setEnabled(true);
        oidcClient.setDisplayName("Query Test OIDC Client");
        oidcClient.setDescription("An OIDC client for query tests");
        oidcClient.setLoginFlows(Set.of(
                OIDCClientRepresentation.Flow.STANDARD,
                OIDCClientRepresentation.Flow.DIRECT_GRANT));
        oidcClient.setRoles(Set.of("admin", "user", "viewer"));
        try (var response = clients.createClient(oidcClient)) {
            var created = response.readEntity(OIDCClientRepresentation.class);
            testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }

        var disabledClient = new OIDCClientRepresentation("query-test-disabled");
        disabledClient.setEnabled(false);
        disabledClient.setDescription("A disabled OIDC client");
        try (var response = clients.createClient(disabledClient)) {
            var created = response.readEntity(OIDCClientRepresentation.class);
            testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }

        var samlClient = new SAMLClientRepresentation();
        samlClient.setClientId("query-test-saml");
        samlClient.setEnabled(true);
        samlClient.setDisplayName("Query Test SAML Client");
        try (var response = clients.createClient(samlClient)) {
            var created = response.readEntity(SAMLClientRepresentation.class);
            testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }
    }

    @Test
    public void noQueryReturnsAllClients() throws IOException {
        var clients = queryClients(null);
        assertThat(clients.size(), greaterThan(0));
    }

    @Test
    public void filterByClientId() throws IOException {
        var clients = queryClients("clientId:query-test-oidc");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
    }

    @Test
    public void filterByProtocol() throws IOException {
        var clients = queryClients("protocol:saml");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c instanceof SAMLClientRepresentation));
    }

    @Test
    public void filterByEnabled() throws IOException {
        var clients = queryClients("enabled:false");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getEnabled() != null && !c.getEnabled()));
    }

    @Test
    public void filterByMultipleConditions() throws IOException {
        var clients = queryClients("protocol:openid-connect enabled:true");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c instanceof OIDCClientRepresentation && Boolean.TRUE.equals(c.getEnabled())));
    }

    @Test
    public void filterByQuotedValue() throws IOException {
        var clients = queryClients("displayName:\"Query Test OIDC Client\"");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
    }

    @Test
    public void filterByRolesListSubset() throws IOException {
        var clients = queryClients("roles:[admin,user]");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
        assertTrue(clients.get(0).getRoles().containsAll(Set.of("admin", "user")));
    }

    @Test
    public void filterByLoginFlows() throws IOException {
        var clients = queryClients("loginFlows:[STANDARD]");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c instanceof OIDCClientRepresentation oidc
                        && oidc.getLoginFlows().contains(OIDCClientRepresentation.Flow.STANDARD)));
    }

    @Test
    public void noMatchReturnsEmpty() throws IOException {
        var clients = queryClients("clientId:nonexistent-client-xyz");
        assertThat(clients, empty());
    }

    @Test
    public void invalidQueryReturns400() throws IOException {
        assertQueryReturns400("no-colon-here");
    }

    @Test
    public void unknownFieldReturns400() throws IOException {
        assertQueryReturns400("unknownField:value");
    }

    @Test
    public void caseSensitiveClientId() throws IOException {
        var clients = queryClients("clientId:QUERY-TEST-OIDC");
        assertThat(clients, empty());
    }

    @Test
    public void contradictoryFiltersReturnEmpty() throws IOException {
        var clients = queryClients("enabled:true enabled:false");
        assertThat(clients, empty());
    }

    @Test
    public void emptyQueryParamReturnsAll() throws IOException {
        HttpGet request = new HttpGet(getClientsApiUrl() + "?q=");
        setAuthHeader(request);
        try (var response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String body = EntityUtils.toString(response.getEntity());
            List<BaseClientRepresentation> clients = mapper.readValue(body, new TypeReference<>() {});
            assertThat(clients.size(), greaterThan(0));
        }
    }

    @Test
    public void filterByDescription() throws IOException {
        var clients = queryClients("description:\"An OIDC client for query tests\"");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
    }

    private void assertQueryReturns400(String query) throws IOException {
        HttpGet request = new HttpGet(getClientsApiUrl() + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        setAuthHeader(request);
        try (var response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
        }
    }

    private List<BaseClientRepresentation> queryClients(String query) throws IOException {
        String url = getClientsApiUrl();
        if (query != null) {
            url += "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }
        HttpGet request = new HttpGet(url);
        setAuthHeader(request);
        try (var response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String body = EntityUtils.toString(response.getEntity());
            return mapper.readValue(body, new TypeReference<>() {});
        }
    }
}
