
package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;


/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
//@AdapterLibsLocationProperty("adapter.libs.wildfly")
public class WildflyJSConsoleExampleAdapterTest extends AbstractJSConsoleExampleAdapterTest {

}
