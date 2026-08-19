package org.keycloak.tests.broker;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.ReverseProxy;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.tests.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public final class KcOidcBrokerFrontendUrlTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @Rule
    public ReverseProxy proxy = new ReverseProxy();

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override 
            public RealmRepresentation createConsumerRealm() {
                RealmRepresentation realm = super.createConsumerRealm();

                Map<String, String> attributes = new HashMap<>();

                attributes.put("frontendUrl", proxy.getUrl());

                realm.setAttributes(attributes);
                
                return realm;
            }

            @Override 
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clients = super.createProviderClients();

                List<String> redirectUris = new ArrayList<>();

                redirectUris.add(proxy.getUrl() + "/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*");

                clients.get(0).setRedirectUris(redirectUris);
                
                return clients;
            }
        };
    }

    @Test
    @Override 
    public void testLogInAsUserInIDP() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.consumerRealmName(), "consumer", "password", "FirstName", "LastName", "consumer@localhost.com");

        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.baseUrl(proxy.getUrl());
        oauth.openLoginForm();

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        log.debug("Logging in");

        // make sure the frontend url is used to build the redirect uri when redirecting to the broker
        assertTrue(driver.getCurrentUrl().contains("redirect_uri=" + URLEncoder.encode(proxy.getUrl(), StandardCharsets.UTF_8)));

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        waitForPage(driver, "AUTH_RESPONSE", true);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Ignore
    @Test
    @Override
    public void loginWithExistingUser() {
        // no-op
    }
}
