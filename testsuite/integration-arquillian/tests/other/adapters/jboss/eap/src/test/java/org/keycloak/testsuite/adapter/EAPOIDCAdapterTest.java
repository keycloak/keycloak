package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap")
//@AdapterLibsLocationProperty("adapter.libs.eap7")
//@Ignore //failing tests
public class EAPOIDCAdapterTest extends AbstractDemoServletsAdapterTest {

}
