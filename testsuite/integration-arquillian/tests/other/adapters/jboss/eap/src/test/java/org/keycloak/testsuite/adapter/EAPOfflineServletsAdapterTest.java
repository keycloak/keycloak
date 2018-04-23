package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.OfflineServletsAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
@AppServerContainer("app-server-eap")
public class EAPOfflineServletsAdapterTest extends OfflineServletsAdapterTest {
}
