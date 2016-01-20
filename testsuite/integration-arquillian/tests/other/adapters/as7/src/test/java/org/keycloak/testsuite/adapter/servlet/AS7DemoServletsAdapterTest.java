package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-as7")
@AdapterLibsLocationProperty("adapter.libs.as7")
public class AS7DemoServletsAdapterTest extends AbstractDemoServletsAdapterTest {

}
