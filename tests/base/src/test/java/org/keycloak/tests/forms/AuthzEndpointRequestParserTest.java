package org.keycloak.tests.forms;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for default configuration of OIDC login protocol factory
 */
@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AuthzEndpointRequestParserTest {

    @InjectWebDriver
    WebDriver driver;

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(ref = "test-user", config = TestUserConfig.class)
    ManagedUser managedUser;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testAuthenticationBackwardsCompatible() {
        SecretGenerator.getInstance().randomString(2000 + 1);

        oauth.loginForm()
                .param("paramkey1_too_long", SecretGenerator.getInstance().randomString(2000 + 1))
                .param("paramkey2", "paramvalue2")
                .param("paramkey3", "paramvalue3")
                .param("paramkey4", "paramvalue4")
                .param("paramkey5", "paramvalue5")
                .param("paramkey6_too_many", "paramvalue6").open();
        loginPage.assertCurrent();

    }

    @Test
    public void testParamsLength() {
        // Login hint with length 200 allowed, state with length 200 allowed
        String loginHint200 = SecretGenerator.getInstance().randomString(200);
        String state200 = SecretGenerator.getInstance().randomString(200);
        oauth.loginForm()
                .loginHint(loginHint200)
                .state(state200)
                .open();
        assertLogin(loginHint200, state200);

        // Login hint with length 500 not allowed, state with length 500 allowed
        String loginHint500 = SecretGenerator.getInstance().randomString(500);
        String state500 = SecretGenerator.getInstance().randomString(500);
        oauth.loginForm()
                .loginHint(loginHint500)
                .state(state500)
                .open();
        assertLogin("", state500);

        // state with length 4100 not allowed
        String state4100 = SecretGenerator.getInstance().randomString(4100);
        oauth.loginForm()
                .state(state4100)
                .open();
        assertLogin("", null);
    }

    protected void assertLogin(String loginHintExpected, String stateExpected) {
        loginPage.assertCurrent();
        Assertions.assertEquals(loginHintExpected, loginPage.getUsername());
        loginPage.clearUsernameInput();
        loginPage.fillLogin("test-user", "password");
        loginPage.submit();

        assertTrue(driver.getPageSource().contains("Happy days"));
        // String currentUrl = driver.getCurrentUrl();
        String state = oauth.parseLoginResponse().getState();
        Assertions.assertEquals(stateExpected, state);

        AccountHelper.logout(realm.admin(), "test-user");
    }


    private static class TestUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            user.username("test-user");
            user.password("password");
            user.name("My", "Test");
            user.email("test@email.org");
            user.emailVerified(true);

            return user;
        }
    }

}
