package org.keycloak.tests.cluster;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class AuthenticationSessionClusterTest extends AbstractClusterTest {

    @Test
    public void authenticationSessionCluster() {
        loadBalancer.node(0);
        oauth.baseUrl(backendNode(0).getContextRoot());

        String loginUrl = oauth.loginForm().build();
        Set<String> visitedRoutes = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            driver.open(loginUrl);
            String authSessionCookie = AuthenticationSessionFailoverClusterTest.getAuthSessionCookieValue(driver.driver());

            int routeSeparator = authSessionCookie.indexOf('.');
            assertNotEquals(-1, routeSeparator);

            String route = authSessionCookie.substring(routeSeparator + 1);
            assertNotNull(route);
            assertFalse(route.isBlank());
            visitedRoutes.add(route);

            driver.driver().manage().deleteAllCookies();
        }

        assertFalse(visitedRoutes.isEmpty());
    }

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;
}
