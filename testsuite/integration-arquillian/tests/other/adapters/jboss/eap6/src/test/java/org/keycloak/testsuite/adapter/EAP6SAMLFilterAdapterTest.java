package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.AbstractSAMLFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-eap6")
public class EAPSAMLFilterAdapterTest extends AbstractSAMLFilterServletAdapterTest {
}
