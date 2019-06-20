package org.keycloak.testsuite.adapter.servlet.jetty;

import org.junit.Ignore;
import org.keycloak.testsuite.adapter.servlet.SAMLServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

@AppServerContainer(ContainerConstants.APP_SERVER_JETTY94)
@AppServerContainer(ContainerConstants.APP_SERVER_JETTY93)
@AppServerContainer(ContainerConstants.APP_SERVER_JETTY92)
public class JettySAMLServletAdapterTest extends SAMLServletAdapterTest {

    @Ignore("KEYCLOAK-9687")
    @Override
    public void multiTenant1SamlTest() throws Exception {

    }

    @Ignore("KEYCLOAK-9687")
    @Override
    public void multiTenant2SamlTest() throws Exception {

    }
}
