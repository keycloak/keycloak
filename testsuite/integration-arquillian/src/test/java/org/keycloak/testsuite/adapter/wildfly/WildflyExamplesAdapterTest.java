package org.keycloak.testsuite.adapter.wildfly;

import org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class WildflyExamplesAdapterTest extends AbstractExamplesAdapterTest {

}
