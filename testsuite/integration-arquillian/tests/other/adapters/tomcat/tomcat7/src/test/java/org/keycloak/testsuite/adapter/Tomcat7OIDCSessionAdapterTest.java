package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.SessionServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-tomcat7")
public class Tomcat7OIDCSessionAdapterTest extends SessionServletAdapterTest {

}
