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
package org.keycloak.testsuite.adapter.crossdc;

import org.keycloak.testsuite.adapter.page.EmployeeServletDistributable;
import org.keycloak.testsuite.arquillian.annotation.*;

import java.io.*;

import org.keycloak.testsuite.adapter.servlet.cluster.AbstractSAMLAdapterClusterTest;
import org.keycloak.testsuite.adapter.servlet.SendUsernameServlet;

import org.apache.commons.lang3.math.NumberUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.wildfly.extras.creaper.core.*;
import org.wildfly.extras.creaper.core.online.*;
import org.wildfly.extras.creaper.core.online.operations.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.keycloak.testsuite.adapter.AbstractServletsAdapterTest.samlServletDeployment;

/**
 *
 * @author hmlnarik
 */
@Ignore("Infinispan version 5 does not support remote cache events, hence this test is left here for development purposes only")
@AppServerContainer("app-server-eap6")
public class EAP6SAMLAdapterCrossDCTest extends AbstractSAMLAdapterClusterTest {

    @BeforeClass
    public static void checkCrossDcTest() {
        Assume.assumeThat("Seems not to be running cross-DC tests", System.getProperty("cache.server"), not(is("undefined")));
    }

    protected static final int PORT_OFFSET_CACHE_1 = NumberUtils.toInt(System.getProperty("cache.server.port.offset"), 0);
    protected static final int CACHE_HOTROD_PORT_CACHE_1 = 11222 + PORT_OFFSET_CACHE_1;
    protected static final int PORT_OFFSET_CACHE_2 = NumberUtils.toInt(System.getProperty("cache.server.2.port.offset"), 0);
    protected static final int CACHE_HOTROD_PORT_CACHE_2 = 11222 + PORT_OFFSET_CACHE_2;

    private final int[] CACHE_HOTROD_PORTS = new int[] { CACHE_HOTROD_PORT_CACHE_1, CACHE_HOTROD_PORT_CACHE_2 };
    private final int[] TCPPING_PORTS = new int[] { 7600 + PORT_OFFSET_NODE_1, 7600 + PORT_OFFSET_NODE_2 };

    private static final String SESSION_CACHE_NAME = EmployeeServletDistributable.DEPLOYMENT_NAME + "-cache";
    private static final String SSO_CACHE_NAME = SESSION_CACHE_NAME + ".ssoCache";

    private static final Address SESSION_CACHE_ADDR = Address.subsystem("infinispan")
      .and("cache-container", "web")
      .and("replicated-cache", SESSION_CACHE_NAME);
    private static final Address SSO_CACHE_ADDR = Address.subsystem("infinispan")
      .and("cache-container", "web")
      .and("replicated-cache", SSO_CACHE_NAME);

    private static final String JBOSS_WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<jboss-web>\n"
      + "    <replication-config>\n"
      + "        <replication-granularity>SESSION</replication-granularity>\n"
      + "        <cache-name>" + "web." + SESSION_CACHE_NAME + "</cache-name>\n"
      + "    </replication-config>\n"
      + "</jboss-web>";

    @TargetsContainer(value = "app-server-eap6-" + NODE_1_NAME)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive employee() {
        return samlServletDeployment(EmployeeServletDistributable.DEPLOYMENT_NAME,
          EmployeeServletDistributable.DEPLOYMENT_NAME + "/WEB-INF/web.xml",
          SendUsernameServlet.class)
          .addAsWebInfResource(new StringAsset(JBOSS_WEB_XML), "jboss-web.xml");
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
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "initial_hosts"), Values.of("value", "localhost[" + TCPPING_PORTS[nodeIndex] + "]"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "port_range"), Values.of("value", "0"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "num_initial_members"), Values.of("value", "1"));
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

        op.add(Address.of("socket-binding-group", "standard-sockets").and("remote-destination-outbound-socket-binding", "cache-server"),
          Values.of("host", "localhost")
            .and("port", CACHE_HOTROD_PORTS[nodeIndex]));

        op.add(SESSION_CACHE_ADDR, Values.of("statistics-enabled", "true").and("mode", "SYNC"));
        op.add(SESSION_CACHE_ADDR.and("remote-store", "REMOTE_STORE"),
          Values.of("remote-servers", ModelNode.fromString("[{\"outbound-socket-binding\"=>\"cache-server\"}]"))
            .and("cache", SESSION_CACHE_NAME)
            .and("passivation", false)
            .and("purge", false)
            .and("preload", false)
            .and("shared", true)
        );

        op.add(SSO_CACHE_ADDR, Values.of("statistics-enabled", "true").and("mode", "SYNC"));
        op.add(SSO_CACHE_ADDR.and("remote-store", "REMOTE_STORE"),
          Values.of("remote-servers", ModelNode.fromString("[{\"outbound-socket-binding\"=>\"cache-server\"}]"))
            .and("cache", SSO_CACHE_NAME)
            .and("passivation", false)
            .and("purge", false)
            .and("preload", false)
            .and("shared", true)
        );

        Assert.assertTrue(op.writeAttribute(Address.subsystem("jgroups"), "default-stack", "tcpping").isSuccess());
        Assert.assertTrue(op.writeAttribute(Address.subsystem("web"), "instance-id", "${jboss.node.name}").isSuccess());
        op.add(Address.extension("org.keycloak.keycloak-saml-adapter-subsystem"), Values.of("module", "org.keycloak.keycloak-saml-adapter-subsystem"));
        op.add(Address.subsystem("keycloak-saml"));

        clientWorkerNodeClient.execute("reload");

        log.infov("Worker node ({0}) Prepared", managementPort);
    }

}
