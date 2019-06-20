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
package org.keycloak.testsuite.adapter.servlet.cluster;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.testsuite.adapter.AbstractSAMLAdapterClusteredTest;
import org.keycloak.testsuite.adapter.page.EmployeeServletDistributable;
import org.keycloak.testsuite.adapter.servlet.SendUsernameServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;

/**
 *
 * @author hmlnarik
 */
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY_DEPRECATED_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP_CLUSTER)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP6_CLUSTER)
public class SAMLAdapterClusterTest extends AbstractSAMLAdapterClusteredTest {

    @TargetsContainer(value = TARGET_CONTAINER_NODE_1)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive employee() {
        return samlServletDeployment(
                EmployeeServletDistributable.DEPLOYMENT_NAME, 
                EmployeeServletDistributable.DEPLOYMENT_NAME + "/WEB-INF/web.xml", 
                SendUsernameServlet.class);
    }

    @TargetsContainer(value = TARGET_CONTAINER_NODE_2)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME + "_2", managed = false)
    protected static WebArchive employee2() {
        return employee();
    }
}
