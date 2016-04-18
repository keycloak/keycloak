package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.federation.AbstractKerberosLdapAdapterTest;
import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;

/**
 *
 * @author pdrozd
 */
@AppServerContainer("app-server-wildfly")
public class WildflyOIDCKerberosLdapAdapterTest extends AbstractKerberosLdapAdapterTest {

}
