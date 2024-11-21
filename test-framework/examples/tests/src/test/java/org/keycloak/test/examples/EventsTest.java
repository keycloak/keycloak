package org.keycloak.test.examples;

import com.nimbusds.oauth2.sdk.GeneralException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.EventType;
import org.keycloak.test.framework.annotations.InjectEvents;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.events.Events;
import org.keycloak.test.framework.oauth.nimbus.OAuthClient;
import org.keycloak.test.framework.oauth.nimbus.annotations.InjectOAuthClient;
import org.keycloak.test.framework.ui.annotations.InjectPage;
import org.keycloak.test.framework.ui.annotations.InjectWebDriver;
import org.keycloak.test.framework.ui.page.LoginPage;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.URL;

@KeycloakIntegrationTest
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

}
