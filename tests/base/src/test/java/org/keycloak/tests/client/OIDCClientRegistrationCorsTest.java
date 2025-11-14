package org.keycloak.tests.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.RegistrationAccessToken;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.impl.RegistrationWebOriginsPolicyFactory;
import org.keycloak.services.clientregistration.policy.impl.TrustedHostClientRegistrationPolicyFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class OIDCClientRegistrationCorsTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectOAuthClient
    OAuthClient  oAuthClient;

    @Test
    public void testPreflight() throws IOException {
        try (SimpleHttpResponse response = simpleHttp.doOptions(getUrl()).header("Origin", "https://origin1").asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("https://origin1", response.getFirstHeader("Access-Control-Allow-Origin"));

            String sortedAllowMethods = Arrays.stream(response.getFirstHeader("Access-Control-Allow-Methods").split(", ")).sorted().collect(Collectors.joining(", "));
            Assertions.assertEquals("DELETE, GET, OPTIONS, POST, PUT", sortedAllowMethods);

            String sortedAllowHeaders = Arrays.stream(response.getFirstHeader("Access-Control-Allow-Headers").split(", ")).sorted().collect(Collectors.joining(", "));
            Assertions.assertEquals("Accept, Access-Control-Request-Headers, Access-Control-Request-Method, Authorization, Content-Type, DPoP, Origin, X-Requested-With", sortedAllowHeaders);

            Assertions.assertEquals("true", response.getFirstHeader("Access-Control-Allow-Credentials"));
        }

        try (SimpleHttpResponse response = simpleHttp.doOptions(getUrl() + "/someclient").header("Origin", "https://origin1").asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("https://origin1", response.getFirstHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test
    public void testCreateClientValidOrigin() throws IOException {
        createClient("testCreateClientValidOrigin", "https://origin1", true);
    }

    @Test
    public void testWithServiceAccount() throws IOException {
        String realmManagementId = realm.admin().clients().findByClientId("realm-management").get(0).getId();
        RoleRepresentation createClientRoleRep = realm.admin().clients().get(realmManagementId).roles().get("create-client").toRepresentation();
        Response response = realm.admin().clients().create(ClientConfigBuilder.create().clientId("testWithServiceAccount").secret("secret").serviceAccountsEnabled(true).webOrigins("https://origin1").build());
        String clientUuid = CreatedResponseUtil.getCreatedId(response);
        String serviceAccountUuid = realm.admin().clients().get(clientUuid).getServiceAccountUser().getId();
        realm.admin().users().get(serviceAccountUuid).roles().clientLevel(realmManagementId).add(List.of(createClientRoleRep));

        String accessToken = oAuthClient.client("testWithServiceAccount", "secret").doClientCredentialsGrantAccessTokenRequest().getAccessToken();

        createClient(accessToken, "testWithServiceAccount1", "https://origin1", true);
        createClient(accessToken, "testWithServiceAccount2", "https://invalid", false);
    }

    @Test
    public void testAnonymous() throws IOException {
        List<ComponentRepresentation> components = realm.admin().components().query(null, ClientRegistrationPolicy.class.getCanonicalName()).stream().filter(c -> c.getProviderId().equals(RegistrationWebOriginsPolicyFactory.PROVIDER_ID)).toList();
        for (ComponentRepresentation component : components) {
            realm.updateComponentWithCleanup(component.getId(), c -> c.getConfig().put(RegistrationWebOriginsPolicyFactory.WEB_ORIGINS, List.of("https://origin3")));
        }

        components = realm.admin().components().query(null, ClientRegistrationPolicy.class.getCanonicalName()).stream().filter(c -> c.getProviderId().equals(TrustedHostClientRegistrationPolicyFactory.PROVIDER_ID)).toList();
        for (ComponentRepresentation component : components) {
            realm.updateComponentWithCleanup(component.getId(), c -> {
                c.getConfig().put(TrustedHostClientRegistrationPolicyFactory.TRUSTED_HOSTS, List.of("127.0.0.1"));
            });
        }

        createClient(null, "testWithClientPolicy1", "https://origin3", true);
    }

    @Test
    public void testWithClientPolicy() throws IOException {
        List<ComponentRepresentation> components = realm.admin().components().query(null, ClientRegistrationPolicy.class.getCanonicalName()).stream().filter(c -> c.getProviderId().equals(RegistrationWebOriginsPolicyFactory.PROVIDER_ID)).toList();
        for (ComponentRepresentation component : components) {
            realm.updateComponentWithCleanup(component.getId(), c -> c.getConfig().put(RegistrationWebOriginsPolicyFactory.WEB_ORIGINS, List.of("https://origin3")));
        }

        OIDCClientRepresentation client = createClient(createInitialAccessToken(), "testWithClientPolicy1", "https://origin3", true);
        createClient(createInitialAccessToken(), "testWithClientPolicy2", "https://origin4", false);

        client = simpleHttp.doGet(getUrl(client)).auth(client.getRegistrationAccessToken()).header("Origin", "https://origin3").asJson(OIDCClientRepresentation.class);

        Assertions.assertEquals(204, simpleHttp.doDelete(getUrl(client)).auth(client.getRegistrationAccessToken()).header("Origin", "https://origin3").asStatus());
    }

    @Test
    public void testCreateClientInValidOrigin() throws IOException {
        createClient("testCreateClientInValidOrigin", "https://invalid", false);
    }

    @Test
    public void testGetClientValidOrigin() throws IOException {
        OIDCClientRepresentation client = createClient("testGetClientValidOrigin", "https://origin1", true);

        SimpleHttpRequest request = simpleHttp.doGet(getUrl(client)).auth(client.getRegistrationAccessToken()).header("Origin", "https://origin1");
        try (SimpleHttpResponse response = request.asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
        }
    }

    @Test
    public void testGetClientInValidOrigin() throws IOException {
        OIDCClientRepresentation client = createClient("testGetClientInValidOrigin", "https://origin1", true);

        SimpleHttpRequest request = simpleHttp.doGet(getUrl(client)).auth(client.getRegistrationAccessToken()).header("Origin", "https://invalid");
        try (SimpleHttpResponse response = request.asResponse()) {
            Assertions.assertEquals(403, response.getStatus());
        }
    }

    @Test
    public void testUpdateClientValidOrigin() throws IOException {
        OIDCClientRepresentation client = createClient("testUpdateClientValidOrigin", "https://origin1", true);
        client.setClientName("updated");

        RegistrationAccessToken registrationAccessToken = oAuthClient.parseToken(client.getRegistrationAccessToken(), RegistrationAccessToken.class);
        Assertions.assertTrue(registrationAccessToken.getAllowedOrigins().contains("https://origin1"));

        SimpleHttpRequest request = simpleHttp.doPut(getUrl(client)).auth(client.getRegistrationAccessToken()).json(client).header("Origin", "https://origin1");
        try (SimpleHttpResponse response = request.asResponse()) {
            Assertions.assertEquals(200, response.getStatus());
        }

        Assertions.assertEquals("updated", realm.admin().clients().findByClientId(client.getClientId()).get(0).getName());
    }

    @Test
    public void testUpdateClientInValidOrigin() throws IOException {
        OIDCClientRepresentation client = createClient("testUpdateClientInValidOrigin", "https://origin1", true);
        client.setClientName("updated");

        SimpleHttpRequest request = simpleHttp.doPut(getUrl(client)).auth(client.getRegistrationAccessToken()).json(client).header("Origin", "https://invalid");
        try (SimpleHttpResponse response = request.asResponse()) {
            Assertions.assertEquals(403, response.getStatus());
        }

        Assertions.assertNotEquals("updated", realm.admin().clients().findByClientId(client.getClientId()).get(0).getName());
    }

    private OIDCClientRepresentation createClient(String name, String origin, boolean expectSuccess) throws IOException {
        return createClient(createInitialAccessToken(), name, origin, expectSuccess);
    }

    private OIDCClientRepresentation createClient(String token, String name, String origin, boolean expectSuccess) throws IOException {
        SimpleHttpRequest request = simpleHttp.doPost(getUrl()).json(createClient(name)).header("Origin", origin);
        if (token != null) {
            request.auth(token);
        }
        OIDCClientRepresentation createdClient;
        try (SimpleHttpResponse response = request.asResponse()) {
            if (expectSuccess) {
                Assertions.assertEquals(201, response.getStatus());
                Assertions.assertEquals(origin, response.getFirstHeader("Access-Control-Allow-Origin"));
                createdClient = response.asJson(OIDCClientRepresentation.class);
            } else {
                Assertions.assertEquals(403, response.getStatus());
                Assertions.assertNull(response.getHeader("Access-Control-Allow-Origin"));
                createdClient = null;
            }
        }
        assertClientExists(expectSuccess, name);
        return createdClient;
    }

    private String createInitialAccessToken() {
        ClientInitialAccessCreatePresentation initialAccessCreatePresentation = new ClientInitialAccessCreatePresentation(0, 10);
        initialAccessCreatePresentation.setWebOrigins(List.of("https://origin1", "https://origin2"));
        return realm.admin().clientInitialAccess().create(initialAccessCreatePresentation).getToken();
    }

    private OIDCClientRepresentation createClient(String name) {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName(name);
        return client;
    }

    private String getUrl() {
        return realm.getBaseUrl() + "/clients-registrations/openid-connect";
    }

    private String getUrl(OIDCClientRepresentation client) {
        return getUrl() + "/" + client.getClientId();
    }

    private void assertClientExists(boolean expected, String clientName) {
        Optional<ClientRepresentation> clientCreated = realm.admin().clients().findAll().stream().filter(f -> clientName.equals(f.getName())).findFirst();
        Assertions.assertEquals(expected, clientCreated.isPresent());
    }

}
