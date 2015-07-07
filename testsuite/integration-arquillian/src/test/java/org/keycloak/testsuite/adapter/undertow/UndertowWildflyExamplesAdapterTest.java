package org.keycloak.testsuite.adapter.undertow;

import org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainer("auth-server-undertow")
@AppServerContainer("app-server-wildfly")
public class UndertowWildflyExamplesAdapterTest extends AbstractExamplesAdapterTest {

}
