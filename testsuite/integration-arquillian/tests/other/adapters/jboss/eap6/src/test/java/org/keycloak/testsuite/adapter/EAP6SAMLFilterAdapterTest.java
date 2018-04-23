package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.SAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-eap6")
public class EAP6SAMLFilterAdapterTest extends SAMLFilterServletAdapterTest {
}
