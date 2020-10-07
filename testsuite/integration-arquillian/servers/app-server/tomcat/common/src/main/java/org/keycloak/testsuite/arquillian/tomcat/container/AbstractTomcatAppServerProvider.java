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

package org.keycloak.testsuite.arquillian.tomcat.container;

import org.jboss.arquillian.core.spi.Validate;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;
import org.keycloak.testsuite.arquillian.container.AppServerContainerProvider;
import org.keycloak.testsuite.utils.arquillian.tomcat.TomcatAppServerConfigurationUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTomcatAppServerProvider implements AppServerContainerProvider {

    private final String catalinaHome;
    private final String bindHttpPort;
    private final String jmxPort;
    private final String startupTimeoutInSeconds;
    private final String USER = "manager";
    private final String PASS = "arquillian";


    public AbstractTomcatAppServerProvider() {
        catalinaHome = System.getProperty("app.server.home");
        bindHttpPort = determineHttpPort();
        jmxPort = System.getProperty("app.server.management.port");
        startupTimeoutInSeconds = System.getProperty("app.server.startup.timeout");

        Validate.notNullOrEmpty(catalinaHome, "app.server.home is not set.");
        Validate.notNullOrEmpty(bindHttpPort, "app.server.http.port is not set.");
        Validate.notNullOrEmpty(jmxPort, "app.server.management.port is not set.");
        Validate.notNullOrEmpty(startupTimeoutInSeconds, "app.server.startup.timeout is not set.");
    }

    protected abstract String getContainerClassName();

    private String determineHttpPort() {
        String httpPort = System.getProperty("app.server.http.port");

        String portOffset = System.getProperty("app.server.port.offset", "0");
        if (!portOffset.equals("0")) {
            httpPort = String.valueOf(Integer.valueOf(httpPort) + Integer.valueOf(portOffset));
        }

        return httpPort;
    }

    @Override
    public List<Node> getContainers() {
        List<Node> containers = new ArrayList<>();

        containers.add(standaloneContainer());

        return containers;
    }

    private Node standaloneContainer() {
        Node container = new Node("container");
        container.attribute("mode", "manual");
        container.attribute("qualifier", AppServerContainerProvider.APP_SERVER + "-" + getName());

        return TomcatAppServerConfigurationUtils
                .getStandaloneConfiguration(container, getContainerClassName(), catalinaHome,
                        bindHttpPort, jmxPort, USER, PASS, startupTimeoutInSeconds);
    }
}
