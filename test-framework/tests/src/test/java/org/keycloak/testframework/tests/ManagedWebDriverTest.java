package org.keycloak.testframework.tests;

import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@KeycloakIntegrationTest
public class ManagedWebDriverTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(ref = "user", config = UserForManagedWebDriver.class)
    ManagedUser user;

    @InjectWebDriver(ref = "webDriver1", lifecycle = LifeCycle.CLASS)
    ManagedWebDriver webDriver1;

    @InjectWebDriver(ref = "webDriver2", lifecycle = LifeCycle.CLASS)
    ManagedWebDriver webDriver2;

    @InjectOAuthClient(ref = "oauth1", webDriverRef = "webDriver1")
    OAuthClient oauth1;

    @InjectOAuthClient(ref = "oauth2", webDriverRef = "webDriver2")
    OAuthClient oauth2;

    @InjectEvents
    Events events;

    @InjectPage(ref = "loginPage1", webDriverRef = "webDriver1")
    LoginPage loginPage1;

    @InjectPage(ref = "loginPage2", webDriverRef = "webDriver2")
    LoginPage loginPage2;

    @Test
    public void testMultipleWebDriverInstances() {
        oauth1.openLoginForm();

        loginPage1.fillLogin(user.getUsername(), user.getPassword());
        loginPage1.submit();

        Assertions.assertNotNull(oauth1.parseLoginResponse().getCode());
        EventAssertion.assertSuccess(events.poll()).userId(user.getId());

        oauth2.openLoginForm();

        loginPage2.fillLogin(user.getUsername(), user.getPassword());
        loginPage2.submit();

        Assertions.assertNotNull(oauth2.parseLoginResponse().getCode());
        EventAssertion.assertSuccess(events.poll()).userId(user.getId());
    }

    private static class UserForManagedWebDriver implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("user").password("password").email("user@localhost").name("User", "Last");
        }
    }
}
