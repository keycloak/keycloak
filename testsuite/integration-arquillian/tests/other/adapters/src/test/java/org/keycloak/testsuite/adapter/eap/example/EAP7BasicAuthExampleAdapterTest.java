package org.keycloak.testsuite.adapter.eap.example;

import org.keycloak.testsuite.adapter.example.AbstractBasicAuthExampleAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
public class EAP7BasicAuthExampleAdapterTest extends AbstractBasicAuthExampleAdapterTest {

}
