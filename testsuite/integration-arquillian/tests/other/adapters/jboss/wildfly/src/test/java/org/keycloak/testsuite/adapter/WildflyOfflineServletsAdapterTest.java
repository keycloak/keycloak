package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.AbstractOfflineServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
@AppServerContainer("app-server-wildfly")
public class WildflyOfflineServletsAdapterTest extends AbstractOfflineServletsAdapterTest {
}
