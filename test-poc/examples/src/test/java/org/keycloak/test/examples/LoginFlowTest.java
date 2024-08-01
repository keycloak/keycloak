package org.keycloak.test.examples;

import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.annotations.InjectWebDriver;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.flow.LoginFlow;
import org.openqa.selenium.WebDriver;

@KeycloakIntegrationTest
public class LoginFlowTest {

    @InjectWebDriver
    private WebDriver driver;

    @Test
    public void testFlow() {
        LoginFlow flow = new LoginFlow(driver);
        flow.execute();

        System.out.println(driver.getCurrentUrl());

        flow.rollback();
    }
}
