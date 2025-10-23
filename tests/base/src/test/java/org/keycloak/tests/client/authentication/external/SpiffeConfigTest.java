package org.keycloak.tests.client.authentication.external;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderConfig;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderFactory;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderShowInAccountConsole;
import org.keycloak.models.IdentityProviderStorageProvider;
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
import org.openqa.selenium.NoSuchElementException;

import java.io.IOException;

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

        checkNotDisplayOnLoginPages("testConfig");
        checkNoIdpsInAccountConsole();

        rep = realm.admin().identityProviders().get("testConfig").toRepresentation();
        Assertions.assertTrue(rep.isHideOnLogin());
        Assertions.assertEquals(IdentityProviderShowInAccountConsole.NEVER.name(), rep.getConfig().get(IdentityProviderModel.SHOW_IN_ACCOUNT_CONSOLE));

        runOnServer.run(s -> {
            IdentityProviderModel idp = s.getProvider(IdentityProviderStorageProvider.class, "jpa").getByAlias("testConfig");
            Assertions.assertTrue(idp.isHideOnLogin());
            Assertions.assertEquals(IdentityProviderShowInAccountConsole.NEVER.name(), idp.getConfig().get(IdentityProviderModel.SHOW_IN_ACCOUNT_CONSOLE));
        });
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
                .setAttribute(IdentityProviderModel.ISSUER, trustDomain)
                .setAttribute(SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY, bundleEndpoint).build();
    }

}
