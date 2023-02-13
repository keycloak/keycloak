package org.keycloak.testsuite.adapter.servlet.jetty;

import org.junit.Ignore;
import org.keycloak.testsuite.adapter.servlet.DemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

@AppServerContainer(ContainerConstants.APP_SERVER_JETTY94)
public class JettyDemoServletAdapterTest extends DemoServletsAdapterTest {

    @Ignore("KEYCLOAK-9614")
    @Override
    public void testAuthenticated() {

    }

    @Ignore("KEYCLOAK-9614")
    @Override
    public void testAuthenticatedWithCustomSessionConfig() {

    }

    @Ignore("KEYCLOAK-9616")
    @Override
    public void testOIDCParamsForwarding() {

    }

    @Ignore("KEYCLOAK-9616")
    @Override
    public void testOIDCUiLocalesParamForwarding() {

    }

    @Ignore("KEYCLOAK-9615")
    @Override
    public void testInvalidTokenCookie() {

    }

    @Ignore("KEYCLOAK-9615")
    @Override
    public void testTokenInCookieRefresh() {

    }

    @Ignore("KEYCLOAK-9615")
    @Override
    public void testTokenInCookieSSO() {

    }

    @Ignore("KEYCLOAK-9615")
    @Override
    public void testTokenInCookieSSORoot() {

    }

    @Ignore("KEYCLOAK-9617")
    @Override
    public void testWithoutKeycloakConf() {

    }
}
