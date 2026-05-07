package org.keycloak.tests.error;

import java.net.URI;

import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
class EscapeErrorPageTest {

    @InjectPage
    ErrorPage errorPage;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    void innerScript() {
        checkMessage("\"<img src=<script>alert(1)</script>/>\"", "\"alert(1)/>\"");
    }

    @Test
    void innerURL() {
        checkMessage("\"See https://www.keycloak.org/docs\"", "\"See https://www.keycloak.org/docs\"");
    }

    @Test
    void url() {
        checkMessage("See https://www.keycloak.org/docs", "See https://www.keycloak.org/docs");
    }

    @Test
    void ampersandEscape() {
        checkMessage("&lt;img src=&quot;something&quot;&gt;", "");
    }

    @Test
    void hexEscape() {
        checkMessage("&#x3C;img src&#61;something&#x2F;&#x3E;", "");
    }

    @Test
    void plainText() {
        checkMessage("It doesn't work", "It doesn't work");
    }

    @Test
    void textWithPlus() {
        checkMessage("Fact: 1+1=2", "Fact: 1+1=2");
    }

    private void checkMessage(String queryParam, String expected) {
        URI uri = KeycloakUriBuilder.fromUri(keycloakUrls.getMasterRealm())
                .path("error-testing/display-error-message")
                .queryParam("message", queryParam)
                .build();
        driver.open(uri.toString());
        errorPage.assertCurrent();
        assertThat("Sanitized error message for input: " + queryParam, errorPage.getError(), is(expected));
    }
}
