package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;

/**
 * @author fkiss
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
@Ignore //cannot find web.xml in target/examples
public class EAP7CorsExampleAdapterTest extends AbstractCorsExampleAdapterTest {

}