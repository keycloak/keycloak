package org.keycloak.testsuite.adapter.wildfly;

import org.junit.Ignore;
import org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
@AdapterLibsLocationProperty("adapter.libs.wildfly")
@Ignore
//@Jira("KEYCLOAK-1546") // TODO allow @Jira annotation for classes too
public class WildflyExamplesAdapterTest extends AbstractExamplesAdapterTest {

}
