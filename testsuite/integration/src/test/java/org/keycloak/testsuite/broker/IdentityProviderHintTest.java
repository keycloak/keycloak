package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testutils.KeycloakServer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author pedroigor
 */
public class IdentityProviderHintTest {

    @ClassRule
    public static BrokerKeyCloakRule keycloakRule = new BrokerKeyCloakRule();

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(8082);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    private WebDriver driver;

    @WebResource
    private LoginPage loginPage;

    @WebResource
    private OAuthGrantPage grantPage;

    @Test
    public void testSuccessfulRedirect() {
        this.driver.navigate().to("http://localhost:8081/test-app?kc_idp_hint=kc-oidc-idp");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("test-user", "password");

        // grant access to broker-app
        this.grantPage.assertCurrent();
        this.grantPage.accept();

        // authenticated and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        assertTrue(this.driver.getPageSource().contains("idToken"));
    }

    @Test
    public void testInvalidIdentityProviderHint() {
        this.driver.navigate().to("http://localhost:8081/test-app?kc_idp_hint=invalid-idp-id");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        assertEquals("Could not find an identity provider with the identifier [invalid-idp-id].", this.driver.findElement(By.className("instruction")).getText());
    }
}
