package org.keycloak.testsuite.adapter.tomcat;

import org.junit.Ignore;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-tomcat")
@Ignore("Deployments have BASIC auth set-up. This opens browser prompt and webdriver halts.")
public class TomcatServletsAdapterTest extends AbstractServletsAdapterTest {

}
