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

package org.keycloak.testsuite.arquillian.undertow.container;

import java.util.ArrayList;
import java.util.List;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;
import org.keycloak.testsuite.arquillian.undertow.UndertowAppServer;
import org.keycloak.testsuite.arquillian.container.AppServerContainerProvider;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class UndertowAppServerProvider implements AppServerContainerProvider {

    private Node configuration;
    private static final String containerName = "undertow";

    @Override
    public String getName() {
        return containerName;
    }

    @Override
    public List<Node> getContainers() {
        List<Node> containers = new ArrayList<>();

        containers.add(standaloneContainer());

        //not supported yet
//        containers.add(haNodeContainer(1));
//        containers.add(haNodeContainer(2));

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
        createChild("bindAddress", "0.0.0.0");
        createChild("bindHttpPort", "8280");
        createChild("adapterImplClass", UndertowAppServer.class.getName());

        return container;
    }

    private Node haNodeContainer(int number) {
        Node container = new Node("container");
        container.attribute("mode", "manual");
        container.attribute("qualifier", AppServerContainerProvider.APP_SERVER + "-" + containerName + "-ha-node-" + number);

        configuration = container.createChild("configuration");
        createChild("enabled", "true");
        createChild("bindAddress", "localhost");
        createChild("bindHttpPort", "8280");
        createChild("bindHttpPortOffset", Integer.toString(number));
        createChild("adapterImplClass", UndertowAppServer.class.getName());

        return container;
    }

}
