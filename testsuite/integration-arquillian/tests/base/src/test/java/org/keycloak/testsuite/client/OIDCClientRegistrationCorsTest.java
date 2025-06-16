package org.keycloak.testsuite.client;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

public class OIDCClientRegistrationCorsTest extends AbstractKeycloakTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        realm.getClients().add(ClientBuilder.create()
                .redirectUris(VALID_CORS_URL + "/realms/master/app")
                .addWebOrigin(VALID_CORS_URL)
                .clientId("test-app2")
                .publicClient()
                .directAccessGrants()
                .build());
        testRealms.add(realm);
    }

    @Test
    public void preflightRequest() throws Exception {
        Map<String, String> headers = getRegistrationPreflightResponseHeaders(oauth);
        Set<String> allowed = Arrays.stream(headers.get(Cors.ACCESS_CONTROL_ALLOW_METHODS).split(", ")).collect(Collectors.toSet());
        assertTrue(allowed.containsAll(Arrays.asList("POST", "GET", "PUT", "DELETE", "OPTIONS")));
    }

    @Test
    public void registrationCorsRequest() throws Exception {
        ClientInitialAccessPresentation token = adminClient.realm("test").clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 1));
        OIDCClientRepresentation clientRep = new OIDCClientRepresentation();
        clientRep.setClientName("cors-client");
        clientRep.setRedirectUris(List.of("http://localhost"));

        String url = oauth.getEndpoints().getRegistration();
        try (SimpleHttp.Response resp = SimpleHttpDefault.doPost(url, oauth.httpClient().get())
                .header(Cors.ORIGIN_HEADER, VALID_CORS_URL)
                .header(Cors.AUTHORIZATION_HEADER, "Bearer " + token.getToken())
                .json(clientRep)
                .asResponse()) {
            assertEquals(201, resp.getStatus());
            assertCors(resp);
        }

        try (SimpleHttp.Response resp = SimpleHttpDefault.doPost(url, oauth.httpClient().get())
                .header(Cors.ORIGIN_HEADER, INVALID_CORS_URL)
                .header(Cors.AUTHORIZATION_HEADER, "Bearer " + token.getToken())
                .json(clientRep)
                .asResponse()) {
            assertEquals(201, resp.getStatus());
            assertNotCors(resp);
        }
    }

    private static Map<String, String> getRegistrationPreflightResponseHeaders(OAuthClient oAuthClient) {
        HttpOptions options = new HttpOptions(oAuthClient.getEndpoints().getRegistration());
        options.setHeader("Origin", "http://example.com");
        try (CloseableHttpResponse response = oAuthClient.httpClient().get().execute(options)) {
            return Arrays.stream(response.getAllHeaders()).collect(Collectors.toMap(Header::getName, Header::getValue));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static void assertCors(SimpleHttp.Response response) throws Exception {
        assertEquals("true", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals(VALID_CORS_URL, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals(Cors.ACCESS_CONTROL_ALLOW_METHODS, response.getFirstHeader(Cors.ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    private static void assertNotCors(SimpleHttp.Response response) throws Exception {
        assertNull(response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertNull(response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(response.getFirstHeader(Cors.ACCESS_CONTROL_EXPOSE_HEADERS));
    }
}
