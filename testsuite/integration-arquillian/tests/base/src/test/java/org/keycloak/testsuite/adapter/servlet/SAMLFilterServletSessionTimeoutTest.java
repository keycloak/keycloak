package org.keycloak.testsuite.adapter.servlet;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 * @author mhajas
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@UseServletFilter(filterName = "saml-filter", filterClass = "org.keycloak.adapters.saml.servlet.SamlFilter",
        filterDependency = "org.keycloak:keycloak-saml-servlet-filter-adapter")
public class SAMLFilterServletSessionTimeoutTest extends SAMLServletSessionTimeoutTest {

    @BeforeClass
    public static void enabled() {
        String appServerJavaHome = System.getProperty("app.server.java.home", "");
        Assume.assumeFalse(appServerJavaHome.contains("1.7") || appServerJavaHome.contains("ibm-java-70"));
    }
}
