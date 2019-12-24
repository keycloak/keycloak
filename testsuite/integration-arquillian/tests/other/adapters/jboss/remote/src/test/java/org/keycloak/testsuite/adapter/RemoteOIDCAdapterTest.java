package org.keycloak.testsuite.adapter;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.adapter.servlet.DemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-remote")
public class RemoteOIDCAdapterTest extends DemoServletsAdapterTest {

    @Test
    @Ignore
    @Override
    public void testBasicAuth() {
    }
}
