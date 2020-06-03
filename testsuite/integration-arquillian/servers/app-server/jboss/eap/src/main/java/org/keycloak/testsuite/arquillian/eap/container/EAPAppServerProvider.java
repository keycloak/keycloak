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

package org.keycloak.testsuite.arquillian.eap.container;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.as.arquillian.container.managed.ManagedDeployableContainer;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;
import org.keycloak.testsuite.arquillian.container.AppServerContainerProvider;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class EAPAppServerProvider implements AppServerContainerProvider {

    private Node configuration;

    private final String containerName;
    private final String appServerHome;
    private final String appServerJavaHome;
    private final String appServerPortOffset;
    private final String managementProtocol;
    private final String managementPort;
    private final String startupTimeoutInSeconds;

    public EAPAppServerProvider() {
        containerName = System.getProperty("app.server");
        appServerHome = System.getProperty("app.server.home");
        appServerJavaHome = System.getProperty("app.server.java.home");
        appServerPortOffset = System.getProperty("app.server.port.offset");
        managementProtocol = System.getProperty("app.server.management.protocol");
        managementPort = System.getProperty("app.server.management.port");
        startupTimeoutInSeconds = System.getProperty("app.server.startup.timeout");

        Validate.notNullOrEmpty(containerName, "app.server is not set.");
        Validate.notNullOrEmpty(appServerHome, "app.server.home is not set.");
        Validate.notNullOrEmpty(appServerJavaHome, "app.server.java.home is not set.");
        Validate.notNullOrEmpty(appServerPortOffset, "app.server.port.offset is not set.");
        Validate.notNullOrEmpty(managementProtocol, "app.server.management.protocol is not set.");
        Validate.notNullOrEmpty(managementPort, "app.server.management.port is not set.");
        Validate.notNullOrEmpty(startupTimeoutInSeconds, "app.server.startup.timeout is not set.");
    }

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public List<Node> getContainers() {
        List<Node> containers = new ArrayList<>();

        containers.add(standaloneContainer());
        containers.add(clusterGroup());

        return containers;
    }

    private void createChild(String name, String text) {
        configuration.createChild("property").attribute("name", name).text(text);
    }

    private Node standaloneContainer() {
        Node container = new Node("container");
        container.attribute("mode", "manual");
        container.attribute("qualifier", AppServerContainerProvider.APP_SERVER + "-" + containerName);

        configuration = container.createChild("configuration");
        createChild("enabled", "true");
        createChild("adapterImplClass", ManagedDeployableContainer.class.getName());
        createChild("jbossHome", appServerHome);
        createChild("javaHome", appServerJavaHome);
        createChild("jbossArguments", 
                "-Djboss.server.base.dir=" + appServerHome + "/standalone-test " +
                "-Djboss.server.config.dir=" + appServerHome + "/standalone-test/configuration " +
                "-Djboss.server.log.dir=" + appServerHome + "/standalone-test/log " +
                "-Djboss.socket.binding.port-offset=" + appServerPortOffset + " " +
                System.getProperty("adapter.test.props", " ") +
                System.getProperty("kie.maven.settings", " ")
        );
        createChild("javaVmArguments", 
                System.getProperty("app.server.jboss.jvm.debug.args", "") + " " +
                System.getProperty("app.server.memory.settings", "") + " " +
                "-Djava.net.preferIPv4Stack=true" + " " +
                System.getProperty("app.server.jvm.args.extra")
        );
        createChild("managementProtocol", managementProtocol);
        createChild("managementPort", managementPort);
        createChild("startupTimeoutInSeconds", startupTimeoutInSeconds);

        return container;
    }

    private Node clusterGroup() {
        Node group = new Node("group");
        group.attribute("qualifier", "app-server-eap-clustered");
        addHaNodeContainer(group, 1);
        addHaNodeContainer(group, 2);
        return group;
    }

    private void addHaNodeContainer(Node group, int number) {
        String portOffset = System.getProperty("app.server." + number + ".port.offset");
        String managementPort = System.getProperty("app.server." + number + ".management.port");

        Validate.notNullOrEmpty(portOffset, "app.server." + number + ".port.offset is not set.");
        Validate.notNullOrEmpty(managementPort, "app.server." + number + ".management.port is not set.");

        Node container = group.createChild("container");
        container.attribute("mode", "manual");
        container.attribute("qualifier", AppServerContainerProvider.APP_SERVER + "-" + containerName + "-ha-node-" + number);

        configuration = container.createChild("configuration");
        createChild("enabled", "true");
        createChild("adapterImplClass", ManagedDeployableContainer.class.getName());
        createChild("jbossHome", appServerHome);
        createChild("javaHome", appServerJavaHome);
        //cleanServerBaseDir cannot be used until WFARQ-44 is fixed
//        createChild("cleanServerBaseDir", appServerHome + "/standalone-ha-node-" + number);
        createChild("serverConfig", "standalone-ha.xml");
        createChild("jbossArguments", 
                "-Djboss.server.base.dir=" + appServerHome + "/standalone-ha-node-" + number + " " +
                "-Djboss.socket.binding.port-offset=" + portOffset + " " +
                "-Djboss.node.name=ha-node-" + number + " " +
                getCrossDCProperties(number, portOffset) +
                System.getProperty("adapter.test.props", " ") +
                System.getProperty("kie.maven.settings", " ")
        );
        createChild("javaVmArguments",
                System.getProperty("app.server." + number + ".jboss.jvm.debug.args") + " " +
                System.getProperty("app.server.memory.settings", "") + " " +
                "-Djava.net.preferIPv4Stack=true" + " " +
                System.getProperty("app.server.jvm.args.extra")
        );
        createChild("managementProtocol", managementProtocol);
        createChild("managementPort", managementPort);
        createChild("startupTimeoutInSeconds", startupTimeoutInSeconds);
    }
    
    private String getCrossDCProperties(int number, String portOffset) {
        if (System.getProperty("cache.server") == null || System.getProperty("cache.server").equals("undefined")) {
            return "";
        }
        String cacheHotrodPortString = System.getProperty("cache.server." + number + ".port.offset");
        Validate.notNullOrEmpty(cacheHotrodPortString, "cache.server." + number + ".port.offset is not set.");

        int tcppingPort = 7600 + Integer.parseInt(portOffset);
        int cacheHotrodPort = 11222 + Integer.parseInt(cacheHotrodPortString);
        
        //properties used in servers/app-server/jboss/common/cli/configure-crossdc-config.cli
        return "-Dtcpping.port=" + tcppingPort + " -Dcache.hotrod.port=" + cacheHotrodPort + " ";
    }
}
