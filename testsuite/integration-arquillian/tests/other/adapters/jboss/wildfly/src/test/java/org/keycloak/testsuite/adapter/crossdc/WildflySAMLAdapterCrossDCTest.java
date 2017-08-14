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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.BeforeClass;
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
@AppServerContainer("app-server-wildfly")
public class WildflySAMLAdapterCrossDCTest extends AbstractSAMLAdapterClusterTest {

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

    @TargetsContainer(value = "app-server-wildfly-" + NODE_1_NAME)
    @Deployment(name = EmployeeServletDistributable.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive employee() {
        return samlServletDeployment(EmployeeServletDistributable.DEPLOYMENT_NAME,
          EmployeeServletDistributable.DEPLOYMENT_NAME + "/WEB-INF/web.xml",
          SendUsernameServlet.class)
          .addAsWebInfResource(new StringAsset(JBOSS_WEB_XML), "jboss-web.xml");
    }

    @TargetsContainer(value = "app-server-wildfly-" + NODE_2_NAME)
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
          .build());
        Operations op = new Operations(clientWorkerNodeClient);

        Batch b = new Batch();
        Address tcppingStack = Address
          .subsystem("jgroups")
          .and("stack", "tcpping");
        b.add(tcppingStack);
        b.add(tcppingStack.and("transport", "TCP"), Values.of("socket-binding", "jgroups-tcp"));
        b.add(tcppingStack.and("protocol", "TCPPING"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "initial_hosts"), Values.of("value", "localhost[" + TCPPING_PORTS[nodeIndex] + "]"));
        b.add(tcppingStack.and("protocol", "TCPPING").and("property", "port_range"), Values.of("value", "0"));
        b.add(tcppingStack.and("protocol", "MERGE3"));
        b.add(tcppingStack.and("protocol", "FD_SOCK"), Values.of("socket-binding", "jgroups-tcp-fd"));
        b.add(tcppingStack.and("protocol", "FD"));
        b.add(tcppingStack.and("protocol", "VERIFY_SUSPECT"));
        b.add(tcppingStack.and("protocol", "pbcast.NAKACK2"));
        b.add(tcppingStack.and("protocol", "UNICAST3"));
        b.add(tcppingStack.and("protocol", "pbcast.STABLE"));
        b.add(tcppingStack.and("protocol", "pbcast.GMS"));
        b.add(tcppingStack.and("protocol", "MFC"));
        b.add(tcppingStack.and("protocol", "FRAG2"));
        b.writeAttribute(Address.subsystem("jgroups").and("channel", "ee"), "stack", "tcpping");
        op.batch(b);


        op.add(Address.of("socket-binding-group", "standard-sockets").and("remote-destination-outbound-socket-binding", "cache-server"),
          Values.of("host", "localhost")
            .and("port", CACHE_HOTROD_PORTS[nodeIndex]));

        op.add(SESSION_CACHE_ADDR, Values.of("statistics-enabled", "true").and("mode", "SYNC"));
        op.writeAttribute(SESSION_CACHE_ADDR.and("component", "locking"), "isolation", "REPEATABLE_READ");
        op.writeAttribute(SESSION_CACHE_ADDR.and("component", "transaction"), "mode", "BATCH");
        op.add(SESSION_CACHE_ADDR.and("store", "remote"),
          Values.ofList("remote-servers", "cache-server")
            .and("cache", SESSION_CACHE_NAME)
            .and("passivation", false)
            .and("purge", false)
            .and("preload", false)
            .and("shared", true)
        );

        op.add(SSO_CACHE_ADDR, Values.of("statistics-enabled", "true").and("mode", "SYNC"));
        op.add(SSO_CACHE_ADDR.and("store", "remote"),
          Values.ofList("remote-servers", "cache-server")
            .and("cache", SSO_CACHE_NAME)
            .and("passivation", false)
            .and("purge", false)
            .and("preload", false)
            .and("shared", true)
        );

        op.add(Address.extension("org.keycloak.keycloak-saml-adapter-subsystem"), Values.of("module", "org.keycloak.keycloak-saml-adapter-subsystem"));
        op.add(Address.subsystem("keycloak-saml"));

        clientWorkerNodeClient.execute("reload");

        log.infov("Worker node ({0}) Prepared", managementPort);
    }

}
