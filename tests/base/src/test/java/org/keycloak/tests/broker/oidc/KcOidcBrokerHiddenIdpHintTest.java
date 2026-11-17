package org.keycloak.tests.broker.oidc;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerHiddenIdpHintTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = HiddenIdpConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    // The IDP is hidden from the login page, so the inherited base login tests (which click the social
    // button to reach it) don't apply here - this class only exercises the hint-based redirect below.
    @Override
    @Test
    @Disabled("IdP is hidden on the login page; use kc_idp_hint instead of clicking the social button")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    @Disabled("IdP is hidden on the login page; use kc_idp_hint instead of clicking the social button")
    public void testLoginWithExistingUser() {
    }

    @Test
    public void testSuccessfulRedirectToProviderHiddenOnLoginPage() {
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
    }

    static class HiddenIdpConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .hideOnLoginPage());
        }
    }
}
