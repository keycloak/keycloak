package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap6")
//@AdapterLibsLocationProperty("adapter.libs.eap6")
//@Ignore //failing tests
public class EAP6OIDCAdapterTest extends AbstractDemoServletsAdapterTest {

}
