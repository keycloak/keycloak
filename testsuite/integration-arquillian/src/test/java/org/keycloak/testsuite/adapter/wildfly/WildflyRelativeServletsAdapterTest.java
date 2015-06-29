package org.keycloak.testsuite.adapter.wildfly;

import org.junit.Ignore;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
@AuthServerContainer("auth-server-wildfly")
@Ignore // FIXME - Need to somehow override "app.server.base.url" system property set in arquillian.xml for the relative test scenario.
public class WildflyRelativeServletsAdapterTest extends AbstractServletsAdapterTest {
    
}
