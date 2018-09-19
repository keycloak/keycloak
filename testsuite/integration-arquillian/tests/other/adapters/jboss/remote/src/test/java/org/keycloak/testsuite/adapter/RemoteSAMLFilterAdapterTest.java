package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.SAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-remote")
public class RemoteSAMLFilterAdapterTest extends SAMLFilterServletAdapterTest {
}
