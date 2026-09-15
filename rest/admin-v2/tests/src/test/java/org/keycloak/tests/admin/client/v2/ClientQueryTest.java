package org.keycloak.tests.admin.client.v2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.keycloak.common.Profile;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        public RealmBuilder configure(RealmBuilder realm) {
            return realm;
        }
    }

    @BeforeEach
    public void setupTestClients() {
        var clients = getClientsApi();

        var oidcClient = new OIDCClientRepresentation("query-test-oidc");
        oidcClient.setEnabled(true);
        oidcClient.setDisplayName("Query Test OIDC Client");
        oidcClient.setDescription("An OIDC client for query tests");
        oidcClient.setLoginFlows(Set.of(
                OIDCClientRepresentation.Flow.STANDARD,
                OIDCClientRepresentation.Flow.DIRECT_GRANT));
        oidcClient.setRedirectUris(Set.of("http://localhost/callback"));
        oidcClient.setRoles(Set.of("admin", "user", "viewer"));
        try (var response = clients.createClient(oidcClient)) {
            var created = response.readEntity(OIDCClientRepresentation.class);
            testRealm.cleanup().add(realm -> realm.clients().delete(created.getUuid()));
        }

        var authClient = new OIDCClientRepresentation("query-test-auth");
        authClient.setEnabled(true);
        var auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-secret");
        authClient.setAuth(auth);
        try (var response = clients.createClient(authClient)) {
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
        
        samlClient = new SAMLClientRepresentation();
        samlClient.setClientId("query-test-saml-single-role");
        samlClient.setEnabled(true);
        samlClient.setDisplayName("Query Test SAML Client Single Role");
        samlClient.setRoles(Set.of("viewer"));
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
        var clients = queryClients("clientId eq \"query-test-oidc\"");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
    }

    @Test
    public void filterByProtocol() throws IOException {
        var clients = queryClients("protocol eq \"saml\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c instanceof SAMLClientRepresentation));
    }

    @Test
    public void filterByEnabled() throws IOException {
        var clients = queryClients("enabled eq false");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getEnabled() != null && !c.getEnabled()));
    }

    @Test
    public void filterByMultipleConditions() throws IOException {
        var clients = queryClients("protocol eq \"openid-connect\" and enabled eq true");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c instanceof OIDCClientRepresentation && Boolean.TRUE.equals(c.getEnabled())));
    }

    @Test
    public void filterByStringValue() throws IOException {
        var clients = queryClients("displayName eq \"Query Test OIDC Client\"");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
    }

    /**
     * TODO: the current scim jpa predicate logic are not correct.
     * 
     * Outside of a value context, each multi-valued predicate should be evaluated logically as a subquery - not a join. 
     * 
     * current behavior flattens to a single join: from client join roles on (...) where roles.name = "admin" and roles.name = "user"
     * 
     * logical needed behavior (could be expressed as in or exists): from client where exists (from roles where ...) and exists (from roles where ...)
     * 
     * Or if implemented via a join: from client join roles on (...) where roles.name IN ("admin", "user") - but this gets complicated with grouping / NOT
     */
    @Disabled
    @Test
    public void filterByRolesWithAnd() throws IOException {
        var clients = queryClients("roles eq \"admin\" and roles eq \"user\"");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
        assertTrue(clients.get(0).getRoles().containsAll(Set.of("admin", "user")));
    }

    @Test
    public void filterWithOrExpression() throws IOException {
        var clients = queryClients("protocol eq \"saml\" or clientId eq \"query-test-disabled\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c instanceof SAMLClientRepresentation
                        || "query-test-disabled".equals(c.getClientId())));
    }

    @Test
    public void filterWithNotExpression() throws IOException {
        var clients = queryClients("not enabled eq false");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> Boolean.TRUE.equals(c.getEnabled())));
    }

    @Test
    public void filterWithPresenceOperator() throws IOException {
        var clients = queryClients("description pr");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getDescription() != null));
    }

    @Test
    public void noMatchReturnsEmpty() throws IOException {
        var clients = queryClients("clientId eq \"nonexistent-client-xyz\"");
        assertThat(clients, empty());
    }

    @Test
    public void invalidQueryReturns400() throws IOException {
        assertQueryReturns400("not-a-valid-query");
    }

    @Test
    public void unknownFieldReturns400() throws IOException {
        assertQueryReturns400("unknownField eq \"value\"");
    }

    @Test
    public void unsupportedOperatorReturns400() throws IOException {
        assertQueryReturns400("clientId gt \"test\"");
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
    public void filterWithProjectionWorks() throws IOException {
        String url = getClientsApiUrl() + "?fields=clientId&q=" +
                URLEncoder.encode("enabled eq true", StandardCharsets.UTF_8);
        HttpGet request = new HttpGet(url);
        setAuthHeader(request);
        try (var response = httpClient.execute(request)) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String body = EntityUtils.toString(response.getEntity());
            List<BaseClientRepresentation> clients = mapper.readValue(body, new TypeReference<>() {});
            assertThat(clients, not(empty()));
            for (var client : clients) {
                assertNotNull(client.getClientId());
                assertNull(client.getDescription());
                assertNull(client.getDisplayName());
            }
        }
    }

    @Test
    public void filterByRoleMembership() throws IOException {
        var clients = queryClients("roles eq \"admin\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getRoles().contains("admin")));
    }

    @Test
    public void filterWithParenthesizedGrouping() throws IOException {
        var clients = queryClients("(clientId eq \"query-test-oidc\" or clientId eq \"query-test-disabled\") and enabled eq true");
        assertThat(clients.size(), is(1));
        assertThat(clients.get(0).getClientId(), is("query-test-oidc"));
    }

    @Test
    public void filterByDotNotation() throws IOException {
        var clients = queryClients("auth.method eq \"client-secret\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c instanceof OIDCClientRepresentation oidc
                        && oidc.getAuth() != null
                        && "client-secret".equals(oidc.getAuth().getMethod())));
    }

    @Test
    public void filterByAbsence() throws IOException {
        var clients = queryClients("not description pr");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getDescription() == null));
    }

    @Test
    public void filterByNotEquals() throws IOException {
        var clients = queryClients("clientId ne \"query-test-oidc\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().noneMatch(c -> "query-test-oidc".equals(c.getClientId())));
    }

    @Test
    public void filterByContains() throws IOException {
        var clients = queryClients("description co \"OIDC\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getDescription() != null && c.getDescription().contains("OIDC")));
    }

    @Test
    public void filterByStartsWith() throws IOException {
        var clients = queryClients("clientId sw \"query-test\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getClientId().startsWith("query-test")));
    }

    @Test
    public void filterByEndsWith() throws IOException {
        var clients = queryClients("clientId ew \"oidc\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getClientId().endsWith("oidc")));
    }

    @Test
    public void filterByContainsOnCollection() throws IOException {
        var clients = queryClients("roles co \"adm\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c.getRoles().stream().anyMatch(r -> r.contains("adm"))));
    }

    @Test
    public void filterByNotEqualOnCollection() throws IOException {
        var clients = queryClients("roles ne \"viewer\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c -> c.getRoles().stream().filter(s -> !s.equals("viewer")).count() > 0));
    }

    /**
     * There is a transposition here from model fields to an array.
     * It may be possible to support searchability - but it is not trivial
     * 
     * The JPA logic will need to look across the predicate, not just the column and value separately
     * and the attribute and value separately and fully rewrite based upon the projection. 
     */
    @Disabled
    @Test
    public void filterByLoginFlows() throws IOException {
        var clients = queryClients("loginFlows eq \"STANDARD\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().allMatch(c ->
                c instanceof OIDCClientRepresentation oidc
                        && oidc.getLoginFlows().contains(OIDCClientRepresentation.Flow.STANDARD)));
    }

    @Test
    public void filterByCaseSensitiveMatch() throws IOException {
        var clients = queryClients("displayName eq \"query test oidc client\"");
        assertThat(clients, empty());
    }

    @Test
    public void filterByOidcFieldExcludesSaml() throws IOException {
        var clients = queryClients("auth.method eq \"client-secret\"");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().noneMatch(c -> c instanceof SAMLClientRepresentation));
    }

    @Test
    public void filterAndBindsTighterThanOr() throws IOException {
        // "enabled eq false or (clientId eq \"query-test-oidc\" and enabled eq true)" should include query-test-oidc
        var clients = queryClients("enabled eq false or clientId eq \"query-test-oidc\" and enabled eq true");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().anyMatch(c -> "query-test-oidc".equals(c.getClientId())));
        assertTrue(clients.stream().allMatch(c ->
                !Boolean.TRUE.equals(c.getEnabled()) || "query-test-oidc".equals(c.getClientId())));
    }

    @Test
    public void filterWithNotParenthesized() throws IOException {
        var clients = queryClients("not (clientId eq \"query-test-oidc\")");
        assertThat(clients, not(empty()));
        assertTrue(clients.stream().noneMatch(c -> "query-test-oidc".equals(c.getClientId())));
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
