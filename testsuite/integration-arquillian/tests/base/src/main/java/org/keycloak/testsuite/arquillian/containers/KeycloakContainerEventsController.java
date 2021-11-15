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

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.ContainerMultiControlEvent;
import org.jboss.arquillian.container.spi.event.StartClassContainers;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
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
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.helpers.DropAllServlet;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.annotation.RestartContainer;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.Archive;
import org.keycloak.testsuite.util.ContainerAssume;

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
        if (event.getTestClass().isAnnotationPresent(RestartContainer.class)) {
            afterOriginalContainerStop(event.getTestClass().getAnnotation(RestartContainer.class));
        }
    }

    @Override
    public void execute(BeforeClass event) {
        if (event.getTestClass().isAnnotationPresent(RestartContainer.class)) {

            // stop executing the test - remote container cannot be restarted
            ContainerAssume.assumeNotAuthServerRemote();

            RestartContainer restartContainer = event.getTestClass().getAnnotation(RestartContainer.class);

            beforeOriginalContainerStop(restartContainer);

            container.fire(new StopManualContainers());
            container.fire(new StopSuiteContainers());

            beforeNewContainerStart(restartContainer);

            container.fire(new StartClassContainers());
            container.fire(new StartSuiteContainers());
        }
        super.execute(event);
    }

    /**
     * Actions to take before the original container stops.
     * @param restartContainer to be able to evaluate other parameters passed via {@link RestartContainer} annotation.
     */
    protected void beforeOriginalContainerStop(RestartContainer restartContainer) {
        if (restartContainer.initializeDatabase()) {
            deployAndDropAllTables(restartContainer);
        }
    }

    /**
     * Actions to take after the original container stops.
     * @param restartContainer to be able to evaluate other parameters passed via {@link RestartContainer} annotation.
     */
    protected void afterOriginalContainerStop(RestartContainer restartContainer) {
        if (restartContainer.withoutKeycloakAddUserFile()) {
            copyKeycloakAddUserFile();
        }
    }

    /**
     * Actions to take before new container starts.
     * @param restartContainer to be able to evaluate other parameters passed via {@link RestartContainer} annotation.
     */
    protected void beforeNewContainerStart(RestartContainer restartContainer) {
        if (restartContainer.withoutKeycloakAddUserFile()) {
            removeKeycloakAddUserFile();
        }

        if (restartContainer.initializeDatabase()) {
            clearMapStorageFiles();
        }
    }

    /**
     * Drop all KeycloakDS database tables using liquibase dropAll method.
     * @param restartContainer to pass more information from test annotation
     */
    private void deployAndDropAllTables(RestartContainer restartContainer) {
        for (Container c: containerRegistry.get().getContainers()) {
            String containerName = c.getName();
            log.infof("Deploy and dropAll at '%s'", containerName);
            if (containerName == null || ! containerName.startsWith("auth-server")) {
                log.infof("Skipping deployAndDropAllTables for '%s'", containerName);
                continue;
            }
            ContainerDef conf = c.getContainerConfiguration();
            String mgmtPort = conf.getContainerProperty("managementPort");
            if (mgmtPort == null || mgmtPort.isEmpty()) {
                log.warnf("Skipping deployAndDropAllTables for '%s' due to not defined 'managementPort' property.", containerName);
                continue;
            }
            OnlineManagementClient client = null;
            try {
                client = ManagementClient.online(OnlineOptions
                        .standalone()
                        .hostAndPort("localhost", Integer.valueOf(mgmtPort).intValue())
                        .build()
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                WebArchive war = ShrinkWrap.create(WebArchive.class, DropAllServlet.WAR_NAME)
                        .addClass(DropAllServlet.class)
                        .addAsWebInfResource(new StringAsset(DropAllServlet.jbossDeploymentStructureContent), "jboss-deployment-structure.xml");

                client.apply(new Deploy.Builder(
                        war.as(ZipExporter.class).exportAsInputStream(),
                        DropAllServlet.WAR_NAME,
                        true).build());

                if (restartContainer.intializeDatabaseWait() > 0) {
                    try {
                        Thread.sleep(restartContainer.intializeDatabaseWait());
                    } catch (InterruptedException e) {
                        log.warn(e);
                    }
                }

                client.apply(new Undeploy.Builder(DropAllServlet.WAR_NAME).build());
            } catch (CommandFailedException e) {
                log.error(e);
                throw new RuntimeException(e);
            }

        }

    }

    private void clearMapStorageFiles() {
        String filePath = System.getProperty("project.build.directory", "target/map");

        File f = new File(filePath);
        if (!f.exists()) return;

        Arrays.stream(f.listFiles())
                .filter(file -> file.getName().startsWith("map-") && file.getName().endsWith(".json"))
                .forEach(File::delete);
    }

    /**
     * Copy keycloak-add-user.json only if it is jboss container (has jbossHome property).
     */
    private void copyKeycloakAddUserFile() {
        for (Container c: containerRegistry.get().getContainers()) {
            log.tracef("Copy keycloak-add-user.json for container [%s]", c.getName());
            ContainerDef conf = c.getContainerConfiguration();
            String jbossHome = conf.getContainerProperty("jbossHome");
            if (jbossHome != null && !jbossHome.isEmpty()) {
                File originalUserAddJsonFile = new File("target/test-classes/keycloak-add-user.json");
                File userAddJsonFile = new File(conf.getContainerProperty("jbossHome")
                        + "/standalone/configuration/keycloak-add-user.json");
                try {
                    FileUtils.copyFile(originalUserAddJsonFile, userAddJsonFile);
                    log.infof("original user file (%s) has been copied to (%s)",
                            originalUserAddJsonFile.getAbsolutePath(), userAddJsonFile.getAbsolutePath());
                } catch (IOException e) {
                    log.warnf(e, "Problem: keycloak-add-user.json file not copied to %s.", userAddJsonFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Remove keycloak-add-user.json file from server config if exists.
     * It should be removed by previous successful start of the server.
     * This method is there just to make sure it is removed.
     */
    private void removeKeycloakAddUserFile() {
        for (Container c: containerRegistry.get().getContainers()) {
            ContainerDef conf = c.getContainerConfiguration();
            String jbossHome = conf.getContainerProperty("jbossHome");
            if (jbossHome != null && !jbossHome.isEmpty()) {
                File adminUserJsonFile = new File(jbossHome
                        + "/standalone/configuration/keycloak-add-user.json");
                if (log.isTraceEnabled()) {
                    log.tracef("File %s exists=%s", adminUserJsonFile.getAbsolutePath(), adminUserJsonFile.exists());
                }
                adminUserJsonFile.delete();
            }
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
