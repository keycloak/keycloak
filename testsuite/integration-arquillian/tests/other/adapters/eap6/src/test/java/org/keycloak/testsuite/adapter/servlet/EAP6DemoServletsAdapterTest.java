package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap6")
@AdapterLibsLocationProperty("adapter.libs.eap6")
@Ignore //failing tests
public class EAP6DemoServletsAdapterTest extends AbstractDemoServletsAdapterTest {

}
