package org.keycloak.testsuite.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;

@AuthServerContainerExclude(REMOTE)
public class DefaultHostnameTest extends AbstractHostnameTest {

    @ArquillianResource
    protected ContainerController controller;

    private String expectedBackendUrl;

    private String globalFrontEndUrl = "https://keycloak.127.0.0.1.nip.io/custom";

    private String realmFrontEndUrl = "https://my-realm.127.0.0.1.nip.io";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation test = RealmBuilder.create().name("test")
                .client(ClientBuilder.create().name("direct-grant").clientId("direct-grant").enabled(true).secret("password").directAccessGrants())
                .user(UserBuilder.create().username("test-user@localhost").password("password"))
                .build();
        testRealms.add(test);

        RealmRepresentation customHostname = RealmBuilder.create().name("frontendUrl")
                .client(ClientBuilder.create().name("direct-grant").clientId("direct-grant").enabled(true).secret("password").directAccessGrants())
                .user(UserBuilder.create().username("test-user@localhost").password("password"))
                .attribute("frontendUrl", realmFrontEndUrl)
                .build();
        testRealms.add(customHostname);
    }

    @Test
    public void fixedFrontendUrl() throws Exception {
        expectedBackendUrl = AUTH_SERVER_ROOT;

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), AuthServerTestEnricher.getAuthServerContextRoot())) {
            assertWellKnown("test", expectedBackendUrl);

            configureDefault(globalFrontEndUrl, false, null);

            assertWellKnown("test", globalFrontEndUrl);
            assertTokenIssuer("test", globalFrontEndUrl);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", globalFrontEndUrl);
            assertBackendForcedToFrontendWithMatchingHostname("test", globalFrontEndUrl);

            assertWelcomePage(globalFrontEndUrl);
            assertAdminPage("master", globalFrontEndUrl, globalFrontEndUrl);

            assertWellKnown("frontendUrl", realmFrontEndUrl);
            assertTokenIssuer("frontendUrl", realmFrontEndUrl);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"frontendUrl", realmFrontEndUrl);
            assertBackendForcedToFrontendWithMatchingHostname("frontendUrl", realmFrontEndUrl);

            assertAdminPage("frontendUrl", realmFrontEndUrl, realmFrontEndUrl);
        } finally {
            reset();
        }
    }

    // KEYCLOAK-12953
    @Test
    public void emptyRealmFrontendUrl() throws URISyntaxException {
        expectedBackendUrl = AUTH_SERVER_ROOT;
        oauth.clientId("direct-grant");

        RealmResource realmResource = realmsResouce().realm("frontendUrl");
        RealmRepresentation rep = realmResource.toRepresentation();

        try {
            rep.getAttributes().put("frontendUrl", "");
            realmResource.update(rep);

            assertWellKnown("frontendUrl", AUTH_SERVER_ROOT);
        } finally {
            rep.getAttributes().put("frontendUrl", realmFrontEndUrl);
            realmResource.update(rep);
        }
    }

    @Test
    public void fixedAdminUrl() throws Exception {
        expectedBackendUrl = AUTH_SERVER_ROOT;
        String adminUrl = "https://admin.127.0.0.1.nip.io/custom-admin";

        oauth.clientId("direct-grant");

        try {
            assertWellKnown("test", expectedBackendUrl);

            configureDefault(globalFrontEndUrl, false, adminUrl);

            assertWelcomePage(adminUrl);

            assertAdminPage("master", globalFrontEndUrl, adminUrl);
            assertAdminPage("frontendUrl", realmFrontEndUrl, adminUrl);
        } finally {
            reset();
        }
    }

    @Test
    public void forceBackendUrlToFrontendUrl() throws Exception {
        expectedBackendUrl = AUTH_SERVER_ROOT;

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), AuthServerTestEnricher.getAuthServerContextRoot())) {
            assertWellKnown("test", expectedBackendUrl);

            configureDefault(globalFrontEndUrl, true, null);

            expectedBackendUrl = globalFrontEndUrl;

            assertWellKnown("test", globalFrontEndUrl);
            assertTokenIssuer("test", globalFrontEndUrl);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", globalFrontEndUrl);

            expectedBackendUrl = realmFrontEndUrl;

            assertWellKnown("frontendUrl", realmFrontEndUrl);
            assertTokenIssuer("frontendUrl", realmFrontEndUrl);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"frontendUrl", realmFrontEndUrl);
        } finally {
            reset();
        }
    }

    private void assertInitialAccessTokenFromMasterRealm(Keycloak testAdminClient, String realm, String expectedBaseUrl) throws JWSInputException, ClientRegistrationException {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(1);
        rep.setExpiration(10000);

        ClientInitialAccessPresentation initialAccess = testAdminClient.realm(realm).clientInitialAccess().create(rep);
        JsonWebToken token = new JWSInput(initialAccess.getToken()).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/realms/" + realm, token.getIssuer());

        ClientRegistration clientReg = ClientRegistration.create().url(AUTH_SERVER_ROOT, realm).build();
        clientReg.auth(Auth.token(initialAccess.getToken()));

        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        ClientRepresentation response = clientReg.create(client);

        String registrationAccessToken = response.getRegistrationAccessToken();
        JsonWebToken registrationToken = new JWSInput(registrationAccessToken).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/realms/" + realm, registrationToken.getIssuer());
    }

    private void assertTokenIssuer(String realm, String expectedBaseUrl) throws Exception {
        oauth.realm(realm);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        AccessToken token = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(AccessToken.class);
        assertEquals(expectedBaseUrl + "/realms/" + realm, token.getIssuer());

        String introspection = oauth.introspectAccessTokenWithClientCredential(oauth.getClientId(), "password", tokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode introspectionNode = objectMapper.readTree(introspection);
        assertTrue(introspectionNode.get("active").asBoolean());
        assertEquals(expectedBaseUrl + "/realms/" + realm, introspectionNode.get("iss").asText());
    }

    private void assertWellKnown(String realm, String expectedFrontendUrl) throws URISyntaxException {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);
        assertEquals(expectedFrontendUrl + "/realms/" + realm, config.getIssuer());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/auth", config.getAuthorizationEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo", config.getUserinfoEndpoint());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/logout", config.getLogoutEndpoint());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/protocol/openid-connect/certs", config.getJwksUri());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/login-status-iframe.html", config.getCheckSessionIframe());
        assertEquals(expectedBackendUrl + "/realms/" + realm + "/clients-registrations/openid-connect", config.getRegistrationEndpoint());
    }

    // Test backend is forced to frontend if the request hostname matches the frontend
    private void assertBackendForcedToFrontendWithMatchingHostname(String realm, String expectedFrontendUrl) throws URISyntaxException {
        String host = new URI(expectedFrontendUrl).getHost();

        // Scheme and port doesn't matter as we force based on hostname only, so using http and bind port as we can't make requests on configured frontend URL since reverse proxy is not available
        oauth.baseUrl("http://" + host + ":" + System.getProperty("auth.server.http.port") + "/auth");

        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);

        assertEquals(expectedFrontendUrl + "/realms/" + realm, config.getIssuer());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/auth", config.getAuthorizationEndpoint());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo", config.getUserinfoEndpoint());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/logout", config.getLogoutEndpoint());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/certs", config.getJwksUri());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/protocol/openid-connect/login-status-iframe.html", config.getCheckSessionIframe());
        assertEquals(expectedFrontendUrl + "/realms/" + realm + "/clients-registrations/openid-connect", config.getRegistrationEndpoint());

        oauth.baseUrl(AUTH_SERVER_ROOT);
    }

    private void assertWelcomePage(String expectedAdminUrl) throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String welcomePage = SimpleHttp.doGet(AUTH_SERVER_ROOT + "/", client).asString();
            assertTrue(welcomePage.contains("<a href=\"" + expectedAdminUrl + "/admin/\">"));
        }
    }

    private void assertAdminPage(String realm, String expectedFrontendUrl, String expectedAdminUrl) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String indexPage = SimpleHttp.doGet(AUTH_SERVER_ROOT + "/admin/" + realm +"/console/", client).asString();

            assertTrue(indexPage.contains("authServerUrl = '" + expectedFrontendUrl +"'"));
            assertTrue(indexPage.contains("authUrl = '" + expectedAdminUrl +"'"));
            assertTrue(indexPage.contains("consoleBaseUrl = '" + new URI(expectedAdminUrl).getPath() +"/admin/" + realm + "/console/'"));
            assertTrue(indexPage.contains("resourceUrl = '" + new URI(expectedAdminUrl).getPath() +"/resources/"));
        }
    }

}
