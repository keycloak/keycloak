package org.keycloak.testsuite.adapter.wildfly;

import org.junit.Ignore;
import org.keycloak.testsuite.adapter.AbstractMultitenancyAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AdapterLibsLocationProperty;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-wildfly")
@AdapterLibsLocationProperty("adapter.libs.wildfly")
@Ignore("Doesn't work yet. App server returns 403 forbidden.")
public class Wildfly8MultitenancyAdapterTest extends AbstractMultitenancyAdapterTest {

}
