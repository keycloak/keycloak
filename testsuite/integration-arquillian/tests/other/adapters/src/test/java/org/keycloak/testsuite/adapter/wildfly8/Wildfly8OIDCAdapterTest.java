package org.keycloak.testsuite.adapter.wildfly8;

import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class Wildfly8OIDCAdapterTest extends AbstractDemoServletsAdapterTest {

}
