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

import org.keycloak.testsuite.arquillian.ModelTestExecutor;

import org.jboss.arquillian.container.test.impl.ClientTestInstanceEnricher;
import org.jboss.arquillian.container.test.impl.client.LocalCommandService;
import org.jboss.arquillian.container.test.impl.client.container.ClientContainerControllerCreator;
import org.jboss.arquillian.container.test.impl.client.container.ContainerRestarter;
import org.jboss.arquillian.container.test.impl.client.container.command.ContainerCommandObserver;
import org.jboss.arquillian.container.test.impl.client.deployment.ClientDeployerCreator;
import org.jboss.arquillian.container.test.impl.client.deployment.DeploymentGenerator;
import org.jboss.arquillian.container.test.impl.client.deployment.command.DeploymentCommandObserver;
import org.jboss.arquillian.container.test.impl.client.deployment.tool.ArchiveDeploymentToolingExporter;
import org.jboss.arquillian.container.test.impl.client.protocol.ProtocolRegistryCreator;
import org.jboss.arquillian.container.test.impl.client.protocol.local.LocalProtocol;
import org.jboss.arquillian.container.test.impl.deployment.ArquillianDeploymentAppender;
import org.jboss.arquillian.container.test.impl.enricher.resource.ContainerControllerProvider;
import org.jboss.arquillian.container.test.impl.enricher.resource.DeployerProvider;
import org.jboss.arquillian.container.test.impl.enricher.resource.InitialContextProvider;
import org.jboss.arquillian.container.test.impl.enricher.resource.RemoteResourceCommandObserver;
import org.jboss.arquillian.container.test.impl.enricher.resource.URIResourceProvider;
import org.jboss.arquillian.container.test.impl.enricher.resource.URLResourceProvider;
import org.jboss.arquillian.container.test.impl.execution.ClientBeforeAfterLifecycleEventExecuter;
import org.jboss.arquillian.container.test.impl.execution.ClientTestExecuter;
import org.jboss.arquillian.container.test.impl.execution.RemoteTestExecuter;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandService;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.impl.TestContextHandler;
import org.jboss.arquillian.test.impl.context.ClassContextImpl;
import org.jboss.arquillian.test.impl.context.SuiteContextImpl;
import org.jboss.arquillian.test.impl.context.TestContextImpl;
import org.jboss.arquillian.test.impl.enricher.resource.ArquillianResourceTestEnricher;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * KeycloakContainerTestExtension
 * 
 * This Extension Overrides the original ContainerTestExtension. 
 * 
 * Needed to change the behavior of ContainerEventController 
 * to stopManualContainers @AfterSuite instead of @AfterClass
 *
 * @see base/src/main/resources/META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension
 * @see https://issues.jboss.org/browse/ARQ-2186
 * 
 * @author vramik
 * @version $Revision: $
 */
public class KeycloakContainerTestExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        registerOriginal(builder);
        
//      Overriden ContainerEventController
        builder.observer(KeycloakContainerEventsController.class);
        
        // overriden ContainerDeployController
        builder.observer(KeycloakContainerDeployController.class);
    }
    
    private void registerOriginal(ExtensionBuilder builder) {
        // Start -> Copied from TestExtension
        builder.context(SuiteContextImpl.class)
            .context(ClassContextImpl.class)
            .context(TestContextImpl.class);

        builder.observer(TestContextHandler.class)
            .observer(ClientTestInstanceEnricher.class);

        // End -> Copied from TestExtension

        builder.service(AuxiliaryArchiveAppender.class, ArquillianDeploymentAppender.class)
            .service(TestEnricher.class, ArquillianResourceTestEnricher.class)
            .service(Protocol.class, LocalProtocol.class)
            .service(CommandService.class, LocalCommandService.class)
            .service(ResourceProvider.class, URLResourceProvider.class)
            .service(ResourceProvider.class, URIResourceProvider.class)
            .service(ResourceProvider.class, DeployerProvider.class)
            .service(ResourceProvider.class, InitialContextProvider.class)
            .service(ResourceProvider.class, ContainerControllerProvider.class);

//        ContainerEventController is overriden
//        builder.observer(ContainerEventController.class)
          builder.observer(ContainerRestarter.class)
            .observer(DeploymentGenerator.class)
            .observer(ArchiveDeploymentToolingExporter.class)
            .observer(ProtocolRegistryCreator.class)
            .observer(ClientContainerControllerCreator.class)
            .observer(ClientDeployerCreator.class)
            .observer(ClientBeforeAfterLifecycleEventExecuter.class)
            .observer(ClientTestExecuter.class)
//            .observer(LocalTestExecuter.class)
            .observer(ModelTestExecutor.class)
            .observer(RemoteTestExecuter.class)
            .observer(DeploymentCommandObserver.class)
            .observer(ContainerCommandObserver.class)
            .observer(RemoteResourceCommandObserver.class)
            .observer(KeycloakContainerFeaturesController.class);
    }
}
