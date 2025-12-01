package org.keycloak.tests.client.authentication.external;

import java.io.IOException;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderConfig;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderFactory;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.NoSuchElementException;

@KeycloakIntegrationTest(config = SpiffeClientAuthTest.SpiffeServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SpiffeConfigTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testConfig() throws IOException {
        IdentityProvidersResource idps = realm.admin().identityProviders();
        IdentityProviderRepresentation rep = createConfig("testConfig", "spiffe://test", "https://localhost");
        Assertions.assertEquals(201, idps.create(rep).getStatus());

        IdentityProviderRepresentation createdRep = realm.admin().identityProviders().get(rep.getAlias()).toRepresentation();

        Assertions.assertTrue(createdRep.isEnabled());
        MatcherAssert.assertThat(createdRep.getConfig(), Matchers.equalTo(Map.of("bundleEndpoint", "https://localhost", "trustDomain", "spiffe://test")));

        Assertions.assertNull(createdRep.getUpdateProfileFirstLoginMode());
        Assertions.assertNull(createdRep.getFirstBrokerLoginFlowAlias());
        Assertions.assertNull(createdRep.getPostBrokerLoginFlowAlias());
        Assertions.assertNull(createdRep.getOrganizationId());
        Assertions.assertNull(createdRep.isAddReadTokenRoleOnCreate());
        Assertions.assertNull(createdRep.isAuthenticateByDefault());
        Assertions.assertNull(createdRep.isHideOnLogin());
        Assertions.assertNull(createdRep.isLinkOnly());
        Assertions.assertNull(createdRep.isTrustEmail());
        Assertions.assertNull(createdRep.isStoreToken());

        checkNotDisplayOnLoginPages("testConfig");
        checkNoIdpsInAccountConsole();
    }

    @Test
    public void testInvalidConfig() {
        testInvalidConfig("testInvalidConfig1", "with-port:8080", "https://localhost");
        testInvalidConfig("testInvalidConfig2", "without-spiffe-scheme", "https://localhost");
        testInvalidConfig("testInvalidConfig3", "spiffe://valid", "invalid-url");
    }

    private void checkNotDisplayOnLoginPages(String alias) {
        oAuthClient.openLoginForm();
        Assertions.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(alias));
    }

    private void checkNoIdpsInAccountConsole() throws IOException {
        String accessToken = oAuthClient.passwordGrantRequest(user.getUsername(), user.getPassword()).send().getAccessToken();
        String accountUrl = realm.getBaseUrl() + "/account//linked-accounts";
        JsonNode json = simpleHttp.doGet(accountUrl).auth(accessToken).asJson();
        Assertions.assertEquals(0, json.size());
    }

    private void testInvalidConfig(String alias, String trustDomain, String bundleEndpoint) {
        IdentityProviderRepresentation idp = createConfig(alias, trustDomain, bundleEndpoint);
        try (Response r = realm.admin().identityProviders().create(idp)) {
            Assertions.assertEquals(400, r.getStatus());
        }
    }

    private IdentityProviderRepresentation createConfig(String alias, String trustDomain, String bundleEndpoint) {
        return IdentityProviderBuilder.create().providerId(SpiffeIdentityProviderFactory.PROVIDER_ID)
                .alias(alias)
                .setAttribute(SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY, trustDomain)
                .setAttribute(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, bundleEndpoint).build();
    }

}
