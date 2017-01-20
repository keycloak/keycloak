package org.keycloak.testsuite.adapter.example;

import org.junit.Ignore;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author fkiss
 */
@AppServerContainer("app-server-eap6")
@Ignore //cannot find web.xml in target/examples
public class EAP6CorsExampleAdapterTest extends AbstractCorsExampleAdapterTest {

}