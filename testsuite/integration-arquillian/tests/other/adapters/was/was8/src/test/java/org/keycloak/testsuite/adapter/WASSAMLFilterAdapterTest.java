package org.keycloak.testsuite.adapter;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.adapter.servlet.SAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

@AppServerContainer("app-server-was")
public class WASSAMLFilterAdapterTest extends SAMLFilterServletAdapterTest {
    @Override
    @Ignore // KEYCLOAK-6152
    @Test
    public void testPostBadAssertionSignature() {}

    @Override
    @Ignore // KEYCLOAK-6152
    @Test
    public void salesPostEncRejectConsent() {}

    @Override
    @Ignore // KEYCLOAK-6152
    @Test
    public void salesPostRejectConsent() {}

    @Override
    @Ignore // KEYCLOAK-6152
    @Test
    public void testDifferentCookieName() {}

    @Override
    @Ignore
    @Test
    public void testMissingAssertionSignature() {}

    @Override
    @Ignore // KEYCLOAK-6152
    @Test
    public void testRelayStateEncoding() {}
}
