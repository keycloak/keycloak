/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter.servlet.crossdc;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.keycloak.testsuite.adapter.page.EmployeeServletDistributable;
import org.keycloak.testsuite.adapter.AbstractSAMLAdapterClusteredTest;
import org.keycloak.testsuite.adapter.servlet.SendUsernameServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.InitialDcState;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

import org.keycloak.testsuite.crossdc.ServerSetup;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;
import org.keycloak.testsuite.arquillian.containers.InfinispanServerDeployableContainer;

/**
 *
 * @author hmlnarik
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP_CLUSTER)
@InitialDcState(authServers = ServerSetup.FIRST_NODE_IN_EVERY_DC, cacheServers = ServerSetup.FIRST_NODE_IN_EVERY_DC)
public class SAMLAdapterCrossDCTest extends AbstractSAMLAdapterClusteredTest {

    @BeforeClass
    public static void checkCrossDcTest() {
        Assume.assumeThat("Seems not to be running cross-DC tests", System.getProperty("cache.server"), not(isEmptyString()));
        Assume.assumeFalse(String.format("%s not supported with `cache-auth` profile.", SAMLAdapterCrossDCTest.class), 
                InfinispanServerDeployableContainer.CACHE_SERVER_AUTH);
    }

    private static final String SESSION_CACHE_NAME = EmployeeServletDistributable.DEPLOYMENT_NAME + "-cache";

    private static final String JBOSS_WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<jboss-web>\n"
      + "    <replication-config>\n"
      + "        <replication-granularity>SESSION</replication-granularity>\n"
      + "        <cache-name>" + "web." + SESSION_CACHE_NAME + "</cache-name>\n"
      + "    </replication-config>\n"
      + "</jboss-web>";

    @TargetsContainer(value = TARGET_CONTAINER_NODE_1)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive employee() {
        return samlServletDeployment(EmployeeServletDistributable.DEPLOYMENT_NAME,
          EmployeeServletDistributable.DEPLOYMENT_NAME + "/WEB-INF/web.xml",
          SendUsernameServlet.class)
          .addAsWebInfResource(new StringAsset(JBOSS_WEB_XML), "jboss-web.xml");
    }

    @TargetsContainer(value = TARGET_CONTAINER_NODE_2)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME + "_2", managed = false)
    protected static WebArchive employee2() {
        return employee();
    }

    @Override
    protected void prepareServerDirectories() throws Exception {
        prepareServerDirectory("standalone-crossdc", "standalone-" + NODE_1_NAME);
        prepareServerDirectory("standalone-crossdc", "standalone-" + NODE_2_NAME);
    }

}
