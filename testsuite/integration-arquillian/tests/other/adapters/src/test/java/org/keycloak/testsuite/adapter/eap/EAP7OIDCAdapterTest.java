package org.keycloak.testsuite.adapter.eap;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
@Ignore //failing tests
public class EAP7OIDCAdapterTest extends AbstractDemoServletsAdapterTest {

}
