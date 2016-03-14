package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-wildfly")
//@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class WildflySAMLExampleAdapterTest extends AbstractSAMLExampleAdapterTest {

}