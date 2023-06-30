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

package org.keycloak.testsuite.arquillian.fuse.container;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;
import org.keycloak.testsuite.arquillian.container.AppServerContainerProvider;
import org.keycloak.testsuite.utils.arquillian.fuse.CustomFuseContainer;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class Fuse63AppServerProvider implements AppServerContainerProvider {

    private Node configuration;
    private static final String containerName = "fuse63";

    private final String appServerHome;
    private final String appServerJavaHome;
    private final String managementUser;
    private final String managementPassword;

    public Fuse63AppServerProvider() {
        appServerHome = System.getProperty("app.server.home");
        appServerJavaHome = System.getProperty("app.server.java.home");
        managementUser = System.getProperty("app.server.management.user");
        managementPassword = System.getProperty("app.server.management.password");

        Validate.notNullOrEmpty(appServerHome, "app.server.home is not set.");
        Validate.notNullOrEmpty(appServerJavaHome, "app.server.java.home is not set.");
        Validate.notNullOrEmpty(managementUser, "app.server.management.user is not set.");
        Validate.notNullOrEmpty(managementPassword, "app.server.management.password is not set.");
    }

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public List<Node> getContainers() {
        List<Node> containers = new ArrayList<>();

        containers.add(standaloneContainer());

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
        createChild("adapterImplClass", CustomFuseContainer.class.getName());
        createChild("autostartBundle", "false");
        createChild("karafHome", appServerHome);
        createChild("javaHome", appServerJavaHome);
        createChild("javaVmArguments", 
                System.getProperty("app.server.karaf.jvm.debug.args", "") + " " +
                System.getProperty("adapter.test.props", " ")
        );
        
        createChild("jmxServiceURL", "service:jmx:rmi://127.0.0.1:44444/jndi/rmi://127.0.0.1:1099/karaf-root");
        createChild("jmxUsername", managementUser);
        createChild("jmxPassword", managementPassword);

        return container;
    }
}
