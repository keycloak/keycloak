package org.keycloak.testsuite.adapter.wildfly8.example;

import org.keycloak.testsuite.adapter.example.AbstractBasicAuthExampleAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class Wildfly8BasicAuthExampleAdapterTest extends AbstractBasicAuthExampleAdapterTest {

}
