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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.impl.ContainerCreationException;
import org.jboss.arquillian.container.impl.ContainerImpl;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.core.spi.Validate;

import static org.keycloak.testsuite.arquillian.containers.RegistryCreator.ADAPTER_IMPL_CONFIG_STRING;
import static org.keycloak.testsuite.arquillian.containers.RegistryCreator.getAdapterImplClassValue;
import static org.keycloak.testsuite.arquillian.containers.RegistryCreator.getContainerAdapter;
import static org.keycloak.testsuite.arquillian.containers.SecurityActions.isClassPresent;
import static org.keycloak.testsuite.arquillian.containers.SecurityActions.loadClass;

/**
 * This class registers all adapters which are specified in the arquillian.xml.
 *
 * In the case there is only one adapter implementation on the classpath, it is
 * not necessary to specify it in the container configuration since it will be
 * used automatically. You have to specify it only in the case you are going to
 * use more than one container.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Stefan Miklosovic <smikloso@redhat.com>
 * @author Tomas Kyjovsky <tkyjovsk@redhat.com>
 */
public class Registry implements ContainerRegistry {

    private final List<Container> containers;

    private final Injector injector;

    private static final Logger logger = Logger.getLogger(RegistryCreator.class.getName());

    public Registry(Injector injector) {
        this.containers = new ArrayList<>();
        this.injector = injector;
    }

    @Override
    public Container create(ContainerDef definition, ServiceLoader loader) {
        Validate.notNull(definition, "Definition must be specified");

        try {
            logger.log(Level.FINE, "Registering container: {0}", definition.getContainerName());

            @SuppressWarnings("rawtypes")
            Collection<DeployableContainer> containerAdapters = loader.all(DeployableContainer.class);

            DeployableContainer<?> dcService = null;

            if (containerAdapters.size() == 1) {
                // just one container on cp
                dcService = containerAdapters.iterator().next();
            } else {
                Container domainContainer = domainContainer(loader, definition);
                if (domainContainer != null) {
                    return domainContainer;
                }
                if (dcService == null) {
                    dcService = getContainerAdapter(getAdapterImplClassValue(definition), containerAdapters);
                }
                if (dcService == null) {
                    throw new ConfigurationException("Unable to get container adapter from Arquillian configuration.");
                }
            }

            // before a Container is added to a collection of containers, inject into its injection point
            return addContainer(injector.inject(
                    new ContainerImpl(definition.getContainerName(), dcService, definition)));

        } catch (ConfigurationException e) {
            throw new ContainerCreationException("Could not create Container " + definition.getContainerName(), e);
        }
    }
    
    private Container domainContainer(ServiceLoader loader, ContainerDef definition) {
        for (Container container : containers) {
            String adapterImplClassValue = container.getContainerConfiguration().getContainerProperties()
                    .get(ADAPTER_IMPL_CONFIG_STRING);

            if (isServiceLoaderClassAssignableFromAdapterImplClass(loader, adapterImplClassValue.trim())) {
                try {
                    return addContainer((Container) injector.inject(
                            new ContainerImpl(
                                    definition.getContainerName(),
                                    (DeployableContainer) loader.onlyOne(DeployableContainer.class),
                                    definition)));
                } catch (Exception ex) {
                    throw new ContainerCreationException(
                            "Could not create Container " + definition.getContainerName(), ex);
                }
            }
        }
        return null;
    }

    private boolean isServiceLoaderClassAssignableFromAdapterImplClass(ServiceLoader loader, String adapterImplClassValue) {
        if (adapterImplClassValue == null && loader == null) {
            return false;
        }
        if (isClassPresent(adapterImplClassValue)) {
            Class<?> aClass = loadClass(adapterImplClassValue);
            String loaderClassName = loader.getClass().getName();
            if (loaderClassName.contains("$")) {
                loaderClassName = loaderClassName.substring(0, loaderClassName.indexOf("$"));
            }
            return loadClass(loaderClassName).isAssignableFrom(aClass);
        }
        return false;
    }

    @Override
    public List<Container> getContainers() {
        return Collections.unmodifiableList(new ArrayList<>(containers));
    }

    @Override
    public Container getContainer(TargetDescription target) {
        Validate.notNull(target, "Target must be specified");
        if (TargetDescription.DEFAULT.equals(target)) {
            return findDefaultContainer();
        }
        return findMatchingContainer(target.getName());
    }

    private Container addContainer(Container container) {
        containers.add(container);
        return container;
    }

    private Container findDefaultContainer() {
        if (containers.size() == 1) {
            return containers.get(0);
        }
        for (Container container : containers) {
            if (container.getContainerConfiguration().isDefault()) {
                return container;
            }
        }
        return null;
    }

    private Container findMatchingContainer(String name) {
        for (Container container : containers) {
            if (container.getName().equals(name)) {
                return container;
            }
        }
        return null;
    }

    @Override
    public Container getContainer(String name) {
        return findMatchingContainer(name);
    }

}
