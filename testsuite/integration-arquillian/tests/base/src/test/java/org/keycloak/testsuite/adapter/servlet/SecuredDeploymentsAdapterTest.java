/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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


import static org.hamcrest.Matchers.containsString;

import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.CURRENT_APP_SERVER;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.enableHTTPSForAppServer;
import static org.keycloak.testsuite.util.ServerURLs.APP_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.CustomerDb;
import org.keycloak.testsuite.adapter.page.CustomerPortalSubsystem;
import org.keycloak.testsuite.adapter.page.ProductPortalSubsystem;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.arquillian.containers.SelfManagedAppContainerLifecycle;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.operations.OperationException;

@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP71)
public class SecuredDeploymentsAdapterTest extends AbstractServletsAdapterTest implements SelfManagedAppContainerLifecycle {

    @ArquillianResource
    private ContainerController controller;

    @Page
    private CustomerPortalSubsystem customerPortalSubsystem;

    @Page
    private ProductPortalSubsystem productPortalSubsystem;

    @Deployment(name = CustomerPortalSubsystem.DEPLOYMENT_NAME)
    protected static WebArchive customerPortalSubsystem() {
        return servletDeployment(CustomerPortalSubsystem.DEPLOYMENT_NAME, CustomerServlet.class, ErrorServlet.class, ServletTestUtils.class);
    }

    @Deployment(name = ProductPortalSubsystem.DEPLOYMENT_NAME)
    protected static WebArchive productPortalSubsystem() {
        return servletDeployment(ProductPortalSubsystem.DEPLOYMENT_NAME, ProductServlet.class);
    }

    @Deployment(name = CustomerDb.DEPLOYMENT_NAME)
    protected static WebArchive customerDb() {
        return servletDeployment(CustomerDb.DEPLOYMENT_NAME, AdapterActionsFilter.class, CustomerDatabaseServlet.class);
    }

    @BeforeClass
    public static void assumeTLSEnabled() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    @Before
    @Override
    public void startServer() throws InterruptedException, IOException, OperationException, TimeoutException, CommandFailedException, CliException {
        try {
            AppServerTestEnricher.prepareServerDir("standalone-secured-deployments");
        } catch (IOException ex) {
            throw new RuntimeException("Wasn't able to prepare server dir.", ex);
        }

        controller.start(testContext.getAppServerInfo().getQualifier());

        if (!sslConfigured && super.shouldConfigureSSL()) {
            enableHTTPSForAppServer();
            sslConfigured = true;
        }
    }

    // This is SelfManagedAppContainerLifecycle, we can't enable ssl in before in parent class, because it will fail as 
    // the container is not started yet
    @Override
    public boolean shouldConfigureSSL() {
        return false;
    }

    @After
    @Override
    public void stopServer() {
        controller.stop(testContext.getAppServerInfo().getQualifier());
    }

    @Test
    public void testSecuredDeployments() {
        customerPortalSubsystem.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertPageContains("Bill Burke");
        assertPageContains("Stian Thorgersen");

        productPortalSubsystem.navigateTo();
        assertCurrentUrlEquals(productPortalSubsystem);
        assertPageContains("iPhone");
        assertPageContains("iPad");
    }

    private void assertPageContains(String string) {
        String pageSource = driver.getPageSource();
        assertThat(pageSource, containsString(string));
    }
}
