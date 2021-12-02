/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.adapter.servlet;

import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.CustomerPortal;
import org.keycloak.testsuite.adapter.spi.TestSessionIdMapper;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.openqa.selenium.WebDriver;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import static org.junit.Assert.assertTrue;

@AppServerContainer(ContainerConstants.APP_SERVER_UNDERTOW)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
@UseServletFilter(filterName = "oidc-filter", filterClass = "org.keycloak.adapters.servlet.KeycloakOIDCFilter",
        filterDependency = "org.keycloak:keycloak-servlet-filter-adapter", skipPattern = "/error.html",
        idMapper = "org.keycloak.testsuite.adapter.spi.TestSessionIdMapper")
public class DemoFilterServletAdapterTestForCustomizedIdMapper extends AbstractServletsAdapterTest {

    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    protected CustomerPortal customerPortal;

    @Deployment(name = CustomerPortal.DEPLOYMENT_NAME)
    protected static WebArchive customerPortal() {
        return servletDeployment(CustomerPortal.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    // KEYCLOAK-13745
    @Test
    public void testCustomizedSessionIdMapper() {
        customerPortal.navigateTo();
        TestSessionIdMapper singleton = TestSessionIdMapper.getInstance();
        assertTrue(singleton.isCalledBy(getClass().getAnnotation(UseServletFilter.class).filterClass()));
        singleton.clear();
    }
}
