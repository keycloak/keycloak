package org.keycloak.testsuite.adapter.eap6.example;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.adapter.example.AbstractDemoExampleAdapterTest;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap6")
@AdapterLibsLocationProperty("adapter.libs.eap6")
public class EAP6DemoExampleAdapterTest extends AbstractDemoExampleAdapterTest {

}