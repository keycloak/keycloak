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
package org.keycloak.testsuite.arquillian.containers;

import java.io.IOException;

import org.keycloak.testsuite.arquillian.ContainerInfo;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.ContainerMultiControlEvent;
import org.jboss.arquillian.container.spi.event.StopClassContainers;
import org.jboss.arquillian.container.spi.event.StopManualContainers;
import org.jboss.arquillian.container.spi.event.StopSuiteContainers;
import org.jboss.arquillian.container.spi.event.UnDeployManagedDeployments;
import org.jboss.arquillian.container.test.impl.client.ContainerEventController;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

/**
 * Changes behaviour of original ContainerEventController to stop manual containers 
 * @AfterSuite, not @AfterClass
 * 
 * @see https://issues.jboss.org/browse/ARQ-2186
 * 
 * @author vramik
 * @author pskopek
 * @author mabartos
 */
public class KeycloakContainerEventsController extends ContainerEventController {

    protected static final Logger log = Logger.getLogger(KeycloakContainerEventsController.class);

    @Inject
    private Event<ContainerMultiControlEvent> container;
    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Override
    public void execute(@Observes AfterSuite event) {
        container.fire(new StopManualContainers());
        container.fire(new StopSuiteContainers());
    }

    @Override
    public void execute(@Observes(precedence = 0) AfterClass event) {
        try {
            container.fire(new UnDeployManagedDeployments());
        } finally {
            container.fire(new StopClassContainers());
        }
    }

    public static void deploy(Archive archive, ContainerInfo containerInfo) throws CommandFailedException, IOException {
        ManagementClient.online(OnlineOptions
                .standalone()
                .hostAndPort("localhost", containerInfo.getContextRoot().getPort() + 1547)
                .build())
                .apply(new Deploy.Builder(
                        archive.as(ZipExporter.class).exportAsInputStream(),
                        archive.getName(),
                        true).build());
    }

    public static void undeploy(Archive archive, ContainerInfo containerInfo) throws CommandFailedException, IOException {
        ManagementClient.online(OnlineOptions
                .standalone()
                .hostAndPort("localhost", containerInfo.getContextRoot().getPort() + 1547)
                .build())
                .apply(new Undeploy.Builder(archive.getName()).build());
    }
}
