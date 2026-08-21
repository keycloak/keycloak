package org.keycloak.tests.broker.oidc;

import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.OidcBrokerConfigSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerIdpHintTest implements OidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @InjectPage
    OAuthGrantPage grantPage;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @Test
    public void testSuccessfulRedirect() {
        oauth.loginForm()
                .param("kc_idp_hint", IDP_OIDC_ALIAS)
                .open();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        updateAccountInformation();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        assertTrue(webDriver.getCurrentUrl().contains(CONSUMER_REALM));
    }

    @Test
    public void testSuccessfulRedirectToProviderAfterLoginPageShown() {
        oauth.openLoginForm();
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));

        String urlWithHint = webDriver.getCurrentUrl() + "&kc_idp_hint=" + IDP_OIDC_ALIAS;
        webDriver.driver().navigate().to(urlWithHint);
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");

        // do the same thing a second time
        webDriver.driver().navigate().to(urlWithHint);
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");

        // redirect shouldn't happen with a fresh login form (no hint)
        oauth.openLoginForm();
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));
        assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"),
                "Driver should be on the consumer realm page");
    }

    @Test
    public void testInvalidIdentityProviderHint() {
        oauth.openLoginForm();
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));

        String url = webDriver.getCurrentUrl() + "&kc_idp_hint=bogus-idp";
        webDriver.driver().navigate().to(url);
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));

        assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));
    }

    @Test
    public void testIdpHintWithErrorResponseReturnsToLoginPage() {
        List<ClientRepresentation> clients = providerRealm.admin().clients().findByClientId(CLIENT_ID);
        assertEquals(1, clients.size());
        ClientRepresentation brokerClient = clients.get(0);
        brokerClient.setConsentRequired(true);
        providerRealm.admin().clients().get(brokerClient.getId()).update(brokerClient);

        try {
            oauth.loginForm()
                    .param("kc_idp_hint", IDP_OIDC_ALIAS)
                    .open();

            webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
            assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                    "Driver should be on the provider realm page right now");

            loginPage.fillLogin(getUserLogin(), getUserPassword());
            loginPage.submit();

            waitForProfilePageAndUpdate();

            grantPage.assertCurrent();
            grantPage.cancel();

            webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"));
            assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"),
                    "Driver should be back on consumer login page after denial");

            assertTrue(webDriver.driver().getPageSource().contains("Access denied"),
                    "Error message should be displayed");
        } finally {
            brokerClient.setConsentRequired(false);
            providerRealm.admin().clients().get(brokerClient.getId()).update(brokerClient);
        }
    }
}
