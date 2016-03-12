package org.keycloak.testsuite.adapter.eap6.example;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.example.AbstractCorsExampleAdapterTest;

/**
 * @author fkiss
 */
@AppServerContainer("app-server-eap6")
@AdapterLibsLocationProperty("adapter.libs.eap6")
@Ignore //cannot find web.xml in target/examples
public class EAP6CorsExampleAdapterTest extends AbstractCorsExampleAdapterTest {

}