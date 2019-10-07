package org.keycloak.testsuite.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
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
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

public class FixedHostnameTest extends AbstractKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    private String authServerUrl;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);

        RealmRepresentation customHostname = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        customHostname.setId("hostname");
        customHostname.setRealm("hostname");
        customHostname.setAttributes(new HashMap<>());
        customHostname.getAttributes().put("hostname", "custom-domain.127.0.0.1.nip.io");

        testRealms.add(customHostname);
    }

    @BeforeClass
    public static void enabled() {
        ContainerAssume.assumeNotAuthServerRemote();
    }

    @Test
    public void fixedHostname() throws Exception {
        authServerUrl = oauth.AUTH_SERVER_ROOT;
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), AuthServerTestEnricher.getAuthServerContextRoot())) {
            assertWellKnown("test", AUTH_SERVER_SCHEME + "://localhost:" + AUTH_SERVER_PORT);

            configureFixedHostname(-1, -1, false);

            assertWellKnown("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertWellKnown("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);

            assertTokenIssuer("test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertTokenIssuer("hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);

            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", AUTH_SERVER_SCHEME + "://keycloak.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"hostname", AUTH_SERVER_SCHEME + "://custom-domain.127.0.0.1.nip.io:" + AUTH_SERVER_PORT);
        } finally {
            clearFixedHostname();
        }
    }

    @Test
    public void fixedHttpPort() throws Exception {
        // Make sure request are always sent with http
        authServerUrl = "http://localhost:8180/auth";
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), "http://localhost:8180")) {
            assertWellKnown("test", "http://localhost:8180");

            configureFixedHostname(80, -1, false);

            assertWellKnown("test", "http://keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "http://custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "http://keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "http://custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm(testAdminClient,"test", "http://keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm(testAdminClient,"hostname", "http://custom-domain.127.0.0.1.nip.io");
        } finally {
            clearFixedHostname();
        }
    }

    @Test
    public void fixedHostnameAlwaysHttpsHttpsPort() throws Exception {
        // Make sure request are always sent with http
        authServerUrl = "http://localhost:8180/auth";
        oauth.baseUrl(authServerUrl);

        oauth.clientId("direct-grant");

        try (Keycloak testAdminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), "http://localhost:8180")) {
            assertWellKnown("test", "http://localhost:8180");

            configureFixedHostname(-1, 443, true);

            assertWellKnown("test", "https://keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "https://custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "https://keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "https://custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm(testAdminClient, "test", "https://keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm(testAdminClient, "hostname", "https://custom-domain.127.0.0.1.nip.io");
        } finally {
            clearFixedHostname();
        }
    }

    private void assertInitialAccessTokenFromMasterRealm(Keycloak testAdminClient, String realm, String expectedBaseUrl) throws JWSInputException, ClientRegistrationException {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(1);
        rep.setExpiration(10000);

        ClientInitialAccessPresentation initialAccess = testAdminClient.realm(realm).clientInitialAccess().create(rep);
        JsonWebToken token = new JWSInput(initialAccess.getToken()).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, token.getIssuer());

        ClientRegistration clientReg = ClientRegistration.create().url(authServerUrl, realm).build();
        clientReg.auth(Auth.token(initialAccess.getToken()));

        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        ClientRepresentation response = clientReg.create(client);

        String registrationAccessToken = response.getRegistrationAccessToken();
        JsonWebToken registrationToken = new JWSInput(registrationAccessToken).readJsonContent(JsonWebToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, registrationToken.getIssuer());
    }

    private void assertTokenIssuer(String realm, String expectedBaseUrl) throws Exception {
        oauth.realm(realm);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        AccessToken token = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(AccessToken.class);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, token.getIssuer());

        String introspection = oauth.introspectAccessTokenWithClientCredential(oauth.getClientId(), "password", tokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode introspectionNode = objectMapper.readTree(introspection);
        assertTrue(introspectionNode.get("active").asBoolean());
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm, introspectionNode.get("iss").asText());
    }

    private void assertWellKnown(String realm, String expectedBaseUrl) {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);
        assertEquals(expectedBaseUrl + "/auth/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
    }

    private void configureFixedHostname(int httpPort, int httpsPort, boolean alwaysHttps) throws Exception {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            configureUndertow("fixed", "keycloak.127.0.0.1.nip.io", httpPort, httpsPort, alwaysHttps);
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            configureWildFly("fixed", "keycloak.127.0.0.1.nip.io", httpPort, httpsPort, alwaysHttps);
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();

    }

    private void clearFixedHostname() throws Exception {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            configureUndertow("request", "localhost", -1, -1,false);
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            configureWildFly("request", "localhost", -1, -1, false);
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    private void configureUndertow(String provider, String hostname, int httpPort, int httpsPort, boolean alwaysHttps) {
        controller.stop(suiteContext.getAuthServerInfo().getQualifier());

        System.setProperty("keycloak.hostname.provider", provider);
        System.setProperty("keycloak.hostname.fixed.hostname", hostname);
        System.setProperty("keycloak.hostname.fixed.httpPort", String.valueOf(httpPort));
        System.setProperty("keycloak.hostname.fixed.httpsPort", String.valueOf(httpsPort));
        System.setProperty("keycloak.hostname.fixed.alwaysHttps", String.valueOf(alwaysHttps));

        controller.start(suiteContext.getAuthServerInfo().getQualifier());
    }

    private void configureWildFly(String provider, String hostname, int httpPort, int httpsPort, boolean alwaysHttps) throws Exception {
        OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
        Administration administration = new Administration(client);

        client.execute("/subsystem=keycloak-server/spi=hostname:write-attribute(name=default-provider, value=" + provider + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.hostname,value=" + hostname + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.httpPort,value=" + httpPort + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.httpsPort,value=" + httpsPort + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.alwaysHttps,value=" + alwaysHttps + ")");

        administration.reloadIfRequired();

        client.close();
    }

}
