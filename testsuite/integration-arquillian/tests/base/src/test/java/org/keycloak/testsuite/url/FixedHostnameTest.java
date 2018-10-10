package org.keycloak.testsuite.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
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
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

public class FixedHostnameTest extends AbstractKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

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

    @Test
    public void fixedHostname() throws Exception {
        oauth.clientId("direct-grant");

        try {
            assertWellKnown("test", "http","localhost");

            configureFixedHostname(null);

            assertWellKnown("test", "http","keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "http","custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "http","keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "http","custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm("test","http","keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm("hostname", "http","custom-domain.127.0.0.1.nip.io");
        } finally {
            clearFixedHostname();
        }
    }

    @Test
    public void fixedHostnameAndScheme() throws Exception {
        oauth.clientId("direct-grant");

        try {
            assertWellKnown("test", "http","localhost");

            configureFixedHostname("https");

            assertWellKnown("test", "https","keycloak.127.0.0.1.nip.io");
            assertWellKnown("hostname", "https","custom-domain.127.0.0.1.nip.io");

            assertTokenIssuer("test", "https","keycloak.127.0.0.1.nip.io");
            assertTokenIssuer("hostname", "https","custom-domain.127.0.0.1.nip.io");

            assertInitialAccessTokenFromMasterRealm("test", "https", "keycloak.127.0.0.1.nip.io");
            assertInitialAccessTokenFromMasterRealm("hostname", "https", "custom-domain.127.0.0.1.nip.io");
        } finally {
            clearFixedHostname();
        }
    }

    private void assertInitialAccessTokenFromMasterRealm(String realm, String expectedScheme, String expectedHostname) throws JWSInputException, ClientRegistrationException {
        ClientInitialAccessCreatePresentation rep = new ClientInitialAccessCreatePresentation();
        rep.setCount(1);
        rep.setExpiration(10000);

        ClientInitialAccessPresentation initialAccess = adminClient.realm(realm).clientInitialAccess().create(rep);
        JsonWebToken token = new JWSInput(initialAccess.getToken()).readJsonContent(JsonWebToken.class);
        assertEquals(expectedScheme + "://" + expectedHostname + ":8180/auth/realms/" + realm, token.getIssuer());

        ClientRegistration clientReg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", realm).build();
        clientReg.auth(Auth.token(initialAccess.getToken()));

        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        ClientRepresentation response = clientReg.create(client);

        String registrationAccessToken = response.getRegistrationAccessToken();
        JsonWebToken registrationToken = new JWSInput(registrationAccessToken).readJsonContent(JsonWebToken.class);
        assertEquals(expectedScheme + "://" + expectedHostname + ":8180/auth/realms/" + realm, registrationToken.getIssuer());
    }

    private void assertTokenIssuer(String realm, String expectedScheme, String expectedHostname) throws Exception {
        oauth.realm(realm);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        AccessToken token = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(AccessToken.class);
        assertEquals(expectedScheme + "://" + expectedHostname + ":8180/auth/realms/" + realm, token.getIssuer());

        String introspection = oauth.introspectAccessTokenWithClientCredential(oauth.getClientId(), "password", tokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode introspectionNode = objectMapper.readTree(introspection);
        assertTrue(introspectionNode.get("active").asBoolean());
        assertEquals(expectedScheme + "://" + expectedHostname + ":8180/auth/realms/" + realm, introspectionNode.get("iss").asText());
    }

    private void assertWellKnown(String realm, String expectedScheme, String expectedHostname) {
        OIDCConfigurationRepresentation config = oauth.doWellKnownRequest(realm);
        assertEquals(expectedScheme + "://" + expectedHostname + ":8180/auth/realms/" + realm + "/protocol/openid-connect/token", config.getTokenEndpoint());
    }

    private void configureFixedHostname(String scheme) throws Exception {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            configureUndertow("fixed", "keycloak.127.0.0.1.nip.io", scheme);
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            configureWildFly("fixed", "keycloak.127.0.0.1.nip.io", scheme);
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();

    }

    private void clearFixedHostname() throws Exception {
        if (suiteContext.getAuthServerInfo().isUndertow()) {
            configureUndertow("request", "localhost", null);
        } else if (suiteContext.getAuthServerInfo().isJBossBased()) {
            configureWildFly("request", "localhost", null);
        } else {
            throw new RuntimeException("Don't know how to config");
        }

        reconnectAdminClient();
    }

    private void configureUndertow(String provider, String hostname, String scheme) {
        controller.stop(suiteContext.getAuthServerInfo().getQualifier());

        System.setProperty("keycloak.hostname.provider", provider);
        System.setProperty("keycloak.hostname.fixed.hostname", hostname);
        if (scheme != null) {
            System.setProperty("keycloak.hostname.fixed.scheme", scheme);
        } else {
            System.getProperties().remove("keycloak.hostname.fixed.scheme");
        }

        controller.start(suiteContext.getAuthServerInfo().getQualifier());
    }

    private void configureWildFly(String provider, String hostname, String scheme) throws Exception {
        OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
        Administration administration = new Administration(client);

        client.execute("/subsystem=keycloak-server/spi=hostname:write-attribute(name=default-provider, value=" + provider + ")");
        client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.hostname,value=" + hostname + ")");
        if (scheme != null) {
            client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:write-attribute(name=properties.scheme,value=" + scheme + ")");
        } else {
            client.execute("/subsystem=keycloak-server/spi=hostname/provider=fixed:map-remove(name=properties,key=scheme)");
        }

        administration.reloadIfRequired();

        client.close();
    }

}
