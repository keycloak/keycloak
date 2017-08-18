package org.keycloak.testsuite.adapter.servlet;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;

/**
 * Created by zschwarz on 9/14/16.
 */

@UseServletFilter(filterName = "oidc-filter", filterClass = "org.keycloak.adapters.servlet.KeycloakOIDCFilter",
        filterDependency = "org.keycloak:keycloak-servlet-filter-adapter", skipPattern = "/error.html")
public abstract class AbstractDemoFilterServletAdapterTest extends AbstractDemoServletsAdapterTest {


    @Test
    @Override
    @Ignore
    public void testAuthenticated() {

    }

    @Test
    @Override
    @Ignore
    public void testAuthenticatedWithCustomSessionConfig() {

    }

    @Test
    @Override
    @Ignore
    public void testOIDCParamsForwarding() {

    }

}
