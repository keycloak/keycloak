package org.keycloak.testsuite.adapter;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.adapter.servlet.AbstractSAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

@AppServerContainer("app-server-wls")
public class WLSSAMLFilterAdapterTest extends AbstractSAMLFilterServletAdapterTest {

    @Ignore // KEYCLOAK-6152
    @Override
    @Test
    public void testDifferentCookieName() {}
}
