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
package org.keycloak.testsuite.adapter.cluster;

import org.keycloak.testsuite.adapter.page.EmployeeServletDistributable;
import org.keycloak.testsuite.arquillian.annotation.*;

import java.io.*;

import org.keycloak.testsuite.adapter.servlet.cluster.AbstractSAMLAdapterClusterTest;
import org.keycloak.testsuite.adapter.servlet.SendUsernameServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.wildfly.extras.creaper.core.*;
import org.wildfly.extras.creaper.core.online.*;
import org.wildfly.extras.creaper.core.online.operations.*;

import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;

/**
 *
 * @author hmlnarik
 */
@AppServerContainer("app-server-eap6")
public class EAP6SAMLAdapterClusterTest extends AbstractSAMLAdapterClusterTest {

    @TargetsContainer(value = "app-server-eap6-" + NODE_1_NAME)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive employee() {
        return samlServletDeployment(EmployeeServletDistributable.DEPLOYMENT_NAME, EmployeeServletDistributable.DEPLOYMENT_NAME + "/WEB-INF/web.xml", SendUsernameServlet.class);
    }

    @TargetsContainer(value = "app-server-eap6-" + NODE_2_NAME)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME + "_2", managed = false)
    protected static WebArchive employee2() {
        return employee();
    }

    @Override
    protected void prepareWorkerNode(int nodeIndex, Integer managementPort) throws IOException, CliException, NumberFormatException {
        log.infov("Preparing worker node ({0} @ {1})", nodeIndex, managementPort);

        OnlineManagementClient clientWorkerNodeClient = ManagementClient.online(OnlineOptions
          .standalone()
          .hostAndPort("localhost", managementPort)
          .protocol(ManagementProtocol.REMOTE)
          .build());
        Operations op = new Operations(clientWorkerNodeClient);

        Batch b = new Batch();
        Address tcppingStack = Address
          .subsystem("jgroups")
          .and("stack", "tcpping");
        b.add(tcppingStack);
        b.add(tcppingStack.and("transport", "TRANSPORT"), Values.of("socket-binding", "jgroups-tcp").and("type", "TCP"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "TCPPING"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "initial_hosts"), Values.of("value", "localhost[" + (7600 + PORT_OFFSET_NODE_1) + "],localhost[" + (7600 + PORT_OFFSET_NODE_2) + "]"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "port_range"), Values.of("value", "0"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "num_initial_members"), Values.of("value", "2"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "timeout"), Values.of("value", "3000"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "MERGE2"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "FD_SOCK").and("socket-binding", "jgroups-tcp-fd"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "FD"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "VERIFY_SUSPECT"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "pbcast.NAKACK"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "UNICAST2"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "pbcast.STABLE"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "pbcast.GMS"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "UFC"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "MFC"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "FRAG2"));
        b.invoke("add-protocol", tcppingStack, Values.of("type", "RSVP"));
        Assert.assertTrue("Could not add TCPPING JGroups stack", op.batch(b).isSuccess());

        Assert.assertTrue(op.writeAttribute(Address.subsystem("jgroups"), "default-stack", "tcpping").isSuccess());
        Assert.assertTrue(op.writeAttribute(Address.subsystem("web"), "instance-id", "${jboss.node.name}").isSuccess());
        Assert.assertTrue(op.add(Address.extension("org.keycloak.keycloak-saml-adapter-subsystem"), Values.of("module", "org.keycloak.keycloak-saml-adapter-subsystem")).isSuccess());
        Assert.assertTrue(op.add(Address.subsystem("keycloak-saml")).isSuccess());

        clientWorkerNodeClient.execute("reload");

        log.infov("Worker node ({0}) Prepared", managementPort);
    }

}
