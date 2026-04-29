package org.keycloak.tests.forms;

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.keycloak.testsuite.util.runonserver.RunHelpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class LoginTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = TestUserConfig.class)
    ManagedUser managedUser;

    @InjectEvents
    Events events;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void loginWithRememberMe() {
        setRememberMe(true);

        oauth.openLoginForm();
        assertFalse(loginPage.isRememberMe());
        loginPage.rememberMe(true);
        assertTrue(loginPage.isRememberMe());
        loginPage.fillLogin(managedUser.getUsername(), managedUser.getPassword());
        loginPage.submit();

//        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .userId(managedUser.getId())
                .details(Details.USERNAME, managedUser.getUsername())
                .details(Details.REMEMBER_ME, "true");
        String sessionId = loginEvent.getSessionId();

        // Expire session
        String realmName = managedRealm.getName();
        runOnServer.run(RunHelpers.removeUserSession(realmName, sessionId));

        // Assert rememberMe checked and username/email prefilled
        oauth.openLoginForm();
        assertTrue(loginPage.isRememberMe());
        Assertions.assertEquals(managedUser.getUsername(), loginPage.getUsername());

        loginPage.rememberMe(false);
    }


    private void setRememberMe(boolean enabled) {
        this.setRememberMe(enabled, null, null);
    }

    private void setRememberMe(boolean enabled, Integer idleTimeout, Integer maxLifespan) {
        managedRealm.updateWithCleanup(r -> r.setRememberMe(enabled).ssoSessionIdleTimeoutRememberMe(idleTimeout).ssoSessionMaxLifespanRememberMe(maxLifespan));
    }


    private static class TestUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("login-test")
                    .email("login@test.com")
                    .enabled(true)
                    .name("John", "Doe")
                    .password("password");
        }
    }

}
