package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.DemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly9")
public class Wildfly9OIDCAdapterTest extends DemoServletsAdapterTest {

}
