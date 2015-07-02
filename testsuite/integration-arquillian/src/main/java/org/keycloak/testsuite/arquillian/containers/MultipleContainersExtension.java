/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.arquillian.containers;

import java.util.logging.Logger;

import org.jboss.arquillian.container.impl.client.ContainerDeploymentContextHandler;
import org.jboss.arquillian.container.impl.client.container.ContainerDeployController;
import org.jboss.arquillian.container.impl.client.container.ContainerLifecycleController;
import org.jboss.arquillian.container.impl.client.container.DeploymentExceptionHandler;
import org.jboss.arquillian.container.impl.client.deployment.ArchiveDeploymentExporter;
import org.jboss.arquillian.container.impl.context.ContainerContextImpl;
import org.jboss.arquillian.container.impl.context.DeploymentContextImpl;
import org.jboss.arquillian.core.spi.LoadableExtension;

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

        builder.context(ContainerContextImpl.class).context(DeploymentContextImpl.class);

        builder.observer(RegistryCreator.class)
                .observer(ContainerDeploymentContextHandler.class)
                .observer(ContainerLifecycleController.class)
                .observer(ContainerDeployController.class)
                .observer(ArchiveDeploymentExporter.class)
                .observer(DeploymentExceptionHandler.class);
    }

}
