package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.SessionServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-remote")
public class RemoteOIDCSessionAdapterTest extends SessionServletAdapterTest {

}
