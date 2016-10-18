package org.keycloak.testsuite.adapter;

import org.keycloak.testsuite.adapter.servlet.AbstractDemoFilterServletAdapterTest;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 * Created by zschwarz on 9/14/16.
 */

@AppServerContainer("app-server-wildfly")
public class WildflyOIDCFilterAdapterTest extends AbstractDemoFilterServletAdapterTest{
}
