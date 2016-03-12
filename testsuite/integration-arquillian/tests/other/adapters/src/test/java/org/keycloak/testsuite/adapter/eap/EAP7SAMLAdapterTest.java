package org.keycloak.testsuite.adapter.eap;

import org.keycloak.testsuite.adapter.servlet.AbstractSAMLServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author mhajas
 */
@AppServerContainer("app-server-eap7")
@AdapterLibsLocationProperty("adapter.libs.eap7")
public class EAP7SAMLAdapterTest extends AbstractSAMLServletsAdapterTest {

}
