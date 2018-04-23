package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.SessionServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap6")
public class EAP6OIDCSessionAdapterTest extends SessionServletAdapterTest {

}
