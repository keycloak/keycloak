package org.keycloak.tests.cluster;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.remote.providers.runonserver.ClusterTestTasks;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class AuthenticationSessionClusterTest extends AbstractClusterTest {

    @Test
    public void testAuthSessionCookieWithAttachedRoute() {
        loadBalancer.node(0);
        oauth.baseUrl(backendNode(0).getContextRoot());

        String loginUrl = oauth.loginForm().build();
        Set<String> visitedRoutes = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            driver.open(loginUrl);
            String authSessionCookie = ClusterTestUtils.getAuthSessionCookieValue(driver.driver());

            int routeSeparator = authSessionCookie.indexOf('.');
            assertNotEquals(-1, routeSeparator);

            String route = authSessionCookie.substring(routeSeparator + 1);
            assertNotNull(route);
            assertFalse(route.isBlank());
            visitedRoutes.add(route);

            getTestingClientFor(backendNode(0)).server().run(
                    new ClusterTestTasks.AssertAuthSessionRoute(managedRealm.getName(), authSessionCookie, "node1"));

            driver.driver().manage().deleteAllCookies();
        }

        assertThat(visitedRoutes, Matchers.contains(Matchers.startsWith("node1")));
    }

    @Test
    public void testAuthSessionCookieWithoutRoute() {
        loadBalancer.node(0);
        oauth.baseUrl(backendNode(0).getContextRoot());

        String loginUrl = oauth.loginForm().build();

        getTestingClientFor(backendNode(0)).server().run(new ClusterTestTasks.SetShouldAttachRoute(false));

        try {
            for (int i = 0; i < 20; i++) {
                driver.open(loginUrl);
                String authSessionCookie = ClusterTestUtils.getAuthSessionCookieValue(driver.driver());

                assertEquals(-1, authSessionCookie.indexOf('.'));

                getTestingClientFor(backendNode(0)).server().run(
                        new ClusterTestTasks.AssertAuthSessionWithoutRoute(managedRealm.getName(), authSessionCookie));

                driver.driver().manage().deleteAllCookies();
            }
        } finally {
            getTestingClientFor(backendNode(0)).server().run(new ClusterTestTasks.SetShouldAttachRoute(true));
        }
    }

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;
}
