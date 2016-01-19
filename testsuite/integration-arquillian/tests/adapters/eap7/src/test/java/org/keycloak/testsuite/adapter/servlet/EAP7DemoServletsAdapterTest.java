package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
@Ignore //failing tests
public class EAP7DemoServletsAdapterTest extends AbstractDemoServletsAdapterTest {

}
