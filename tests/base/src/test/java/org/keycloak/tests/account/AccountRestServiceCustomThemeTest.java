package org.keycloak.tests.account;

import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class AccountRestServiceCustomThemeTest extends AbstractRestServiceTest {

    @Test
    public void testCustomAccountResourceTheme() throws Exception {
        managedRealm.updateWithCleanup(r -> r.accountTheme("custom-account-provider"));

        try (SimpleHttpResponse response = simpleHttp.doGet(getAccountUrl(null))
                .header("Accept", "text/html")
                .asResponse()) {
            Assertions.assertEquals(200, response.getStatus());

            String html = response.asString();
            Assertions.assertTrue(html.contains("Custom Account Console"));
        }
    }
}
