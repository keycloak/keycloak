package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.SAMLServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-wildfly9")
public class Wildfly9SAMLAdapterTest extends SAMLServletsAdapterTest {

}
