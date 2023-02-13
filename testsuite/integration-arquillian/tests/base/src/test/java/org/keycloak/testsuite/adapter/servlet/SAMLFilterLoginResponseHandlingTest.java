package org.keycloak.testsuite.adapter.servlet;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 * @author mhajas
 */
@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@UseServletFilter(filterName = "saml-filter", filterClass = "org.keycloak.adapters.saml.servlet.SamlFilter",
        filterDependency = "org.keycloak:keycloak-saml-servlet-filter-adapter")
public class SAMLFilterLoginResponseHandlingTest extends SAMLLoginResponseHandlingTest {
    @Test
    @Override
    @Ignore
    public void testErrorHandlingUnsigned() {

    }

    @Test
    @Override
    @Ignore
    public void testErrorHandlingSigned() {

    }
}
