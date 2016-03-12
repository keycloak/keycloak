package org.keycloak.testsuite.adapter.eap.example;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.example.AbstractDemoExampleAdapterTest;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
public class EAP7DemoExampleAdapterTest extends AbstractDemoExampleAdapterTest {

}