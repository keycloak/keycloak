package org.keycloak.testsuite.adapter.as7;

import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-as7")
@AdapterLibsLocationProperty("adapter.libs.as7")
public class AS7OIDCAdapterTest extends AbstractDemoServletsAdapterTest {

}
