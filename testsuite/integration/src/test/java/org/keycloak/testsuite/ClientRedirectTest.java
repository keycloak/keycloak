package org.keycloak.testsuite;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ClientRedirectTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {

            RealmModel testRealm = manager.getRealmByName("test");

            ClientModel launchpadClient = testRealm.addClient("launchpad-test");
            launchpadClient.setBaseUrl("");
            launchpadClient.setRootUrl("http://example.org/launchpad");

            ClientModel dummyClient = testRealm.addClient("dummy-test");
            dummyClient.setRootUrl("http://example.org/dummy");
            dummyClient.setBaseUrl("/base-path");
        }
    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver webDriver;

    private static int getKeycloakPort() {

        String keycloakPort = System.getProperty("keycloak.port", System.getenv("KEYCLOAK_DEV_PORT"));

        try {
            return Integer.parseInt(keycloakPort);
        } catch (Exception ex) {
            return 8081;
        }
    }

    /**
     * Integration test for {@link org.keycloak.services.resources.RealmsResource#getRedirect(String, String)}.
     *
     * @throws Exception
     */
    @Test
    public void testClientRedirectEndpoint() throws Exception {

        oauth.doLogin("test-user@localhost", "password");

        webDriver.get("http://localhost:" + getKeycloakPort() + "/auth/realms/test/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", webDriver.getCurrentUrl());

        webDriver.get("http://localhost:" + getKeycloakPort() + "/auth/realms/test/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", webDriver.getCurrentUrl());

        webDriver.get("http://localhost:" + getKeycloakPort() + "/auth/realms/test/clients/account/redirect");
        assertEquals("http://localhost:" + getKeycloakPort() + "/auth/realms/test/account", webDriver.getCurrentUrl());
    }
}
