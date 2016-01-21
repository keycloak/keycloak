package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
public class EAP7SAMLExampleAdapterTest extends AbstractSAMLExampleAdapterTest {

}