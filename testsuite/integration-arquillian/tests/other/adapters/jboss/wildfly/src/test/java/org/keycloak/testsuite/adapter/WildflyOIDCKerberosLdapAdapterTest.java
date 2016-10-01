package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.federation.AbstractKerberosLdapAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author pdrozd
 */
@AppServerContainer("app-server-wildfly")
public class WildflyOIDCKerberosLdapAdapterTest extends AbstractKerberosLdapAdapterTest {

}
