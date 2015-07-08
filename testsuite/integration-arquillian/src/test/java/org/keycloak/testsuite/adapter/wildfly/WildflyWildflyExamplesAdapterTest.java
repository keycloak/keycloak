package org.keycloak.testsuite.adapter.wildfly;

import org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainer("auth-server-wildfly")
@AppServerContainer("app-server-wildfly")
public class WildflyWildflyExamplesAdapterTest extends AbstractExamplesAdapterTest {

}
