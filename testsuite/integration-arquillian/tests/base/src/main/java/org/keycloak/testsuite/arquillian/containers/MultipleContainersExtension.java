/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.jboss.arquillian.container.impl.client.ContainerDeploymentContextHandler;
import org.jboss.arquillian.container.impl.client.container.ContainerLifecycleController;
import org.jboss.arquillian.container.impl.client.container.DeploymentExceptionHandler;
import org.jboss.arquillian.container.impl.client.deployment.ArchiveDeploymentExporter;
import org.jboss.arquillian.container.impl.context.ContainerContextImpl;
import org.jboss.arquillian.container.impl.context.DeploymentContextImpl;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

import java.util.logging.Logger;

/**
 * Enables multiple container adapters on classpath.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Stefan Miklosovic <smikloso@redhat.com>
 * @author Tomas Kyjovsky <tkyjovsk@redhat.com>
 */
public class MultipleContainersExtension implements LoadableExtension {

    private static final Logger logger = Logger.getLogger(MultipleContainersExtension.class.getName());

    @Override
    public void register(ExtensionBuilder builder) {

        logger.info("Multiple containers extension registering.");

        builder.service(DeployableContainer.class, KeycloakQuarkusServerDeployableContainer.class)
                .service(DeployableContainer.class, InfinispanServerDeployableContainer.class);

        builder.context(ContainerContextImpl.class).context(DeploymentContextImpl.class);

        builder.observer(RegistryCreator.class)
                .observer(ContainerDeploymentContextHandler.class)
                .observer(ContainerLifecycleController.class)
                .observer(ArchiveDeploymentExporter.class)
                .observer(DeploymentExceptionHandler.class);
    }

}
