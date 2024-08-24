package org.keycloak.test.examples;

import com.nimbusds.oauth2.sdk.GeneralException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.EventType;
import org.keycloak.test.framework.annotations.InjectEvents;
import org.keycloak.test.framework.annotations.InjectOAuthClient;
import org.keycloak.test.framework.annotations.InjectPage;
import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.events.Events;
import org.keycloak.test.framework.oauth.OAuthClient;
import org.keycloak.test.framework.page.LoginPage;
import org.keycloak.test.framework.server.KeycloakTestServerConfig;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.URL;

@KeycloakIntegrationTest(config = EventsTest.ServerConfig.class)
public class EventsTest {

    @InjectEvents
    private Events events;

    @InjectOAuthClient
    private OAuthClient oAuthClient;

    @InjectWebDriver
    private WebDriver webDriver;

    @InjectPage
    private LoginPage loginPage;

    @Test
    public void testFailedLogin() throws GeneralException, IOException {
        URL authorizationRequestURL = oAuthClient.authorizationRequest();
        webDriver.navigate().to(authorizationRequestURL);
        loginPage.fillLogin("invalid", "invalid");
        loginPage.submit();

        Assertions.assertEquals(EventType.LOGIN_ERROR, events.poll().getType());
    }

    @Test
    public void testClientLogin() throws GeneralException, IOException {
        oAuthClient.clientCredentialGrant();

        Assertions.assertEquals(EventType.CLIENT_LOGIN, events.poll().getType());
    }

    public static class ServerConfig implements KeycloakTestServerConfig {
        @Override
        public boolean enableSysLog() {
            return true;
        }
    }

}
