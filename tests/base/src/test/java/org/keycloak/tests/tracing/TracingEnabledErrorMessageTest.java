package org.keycloak.tests.tracing;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

@KeycloakIntegrationTest(config = TracingProviderTest.ServerConfigWithTracing.class)
public class TracingEnabledErrorMessageTest {

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    ErrorPage errorPage;

    @Test
    public void traceIdPresentInErrorPage() {
        oauth.redirectUri("http://invalid");
        oauth.openLoginForm();
        errorPage.assertCurrent();

        assertThat(errorPage.getTraceId(), matchesPattern(".*: [0-9a-f]{32}"));
    }
}
