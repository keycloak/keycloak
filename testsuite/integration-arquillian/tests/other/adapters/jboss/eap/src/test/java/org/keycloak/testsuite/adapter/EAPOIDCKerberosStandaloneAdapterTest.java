package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.junit.Ignore;
import org.keycloak.testsuite.adapter.federation.AbstractKerberosStandaloneAdapterTest;
import org.keycloak.testsuite.adapter.servlet.AbstractDemoServletsAdapterTest;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-eap")
public class EAPOIDCKerberosStandaloneAdapterTest extends AbstractKerberosStandaloneAdapterTest {

}
