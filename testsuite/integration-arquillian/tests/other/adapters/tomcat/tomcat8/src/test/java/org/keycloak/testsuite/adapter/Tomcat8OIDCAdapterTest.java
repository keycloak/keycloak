package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.DemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-tomcat8")
public class Tomcat8OIDCAdapterTest extends DemoServletsAdapterTest {

}
