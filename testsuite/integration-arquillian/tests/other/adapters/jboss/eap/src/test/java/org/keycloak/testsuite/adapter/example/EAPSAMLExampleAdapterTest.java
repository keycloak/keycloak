package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-eap")
//@AdapterLibsLocationProperty("adapter.libs.eap7")
public class EAPSAMLExampleAdapterTest extends AbstractSAMLExampleAdapterTest {

}