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

import java.util.Collection;
import java.util.Map;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.config.descriptor.api.GroupDef;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.logging.Logger;
import static org.keycloak.testsuite.arquillian.containers.SecurityActions.isClassPresent;
import static org.keycloak.testsuite.arquillian.containers.SecurityActions.loadClass;

/**
 * Registers all container adapters.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Stefan Miklosovic <smikloso@redhat.com>
 * @author Tomas Kyjovsky <tkyjovsk@redhat.com>
 * @author Vlasta Ramik <vramik@redhat.com>
 */
public class RegistryCreator {

    protected final Logger log = Logger.getLogger(this.getClass());

    @Inject
    @ApplicationScoped
    private InstanceProducer<ContainerRegistry> registry;

    @Inject
    private Instance<Injector> injector;

    @Inject
    private Instance<ServiceLoader> loader;
    
    private String authContainer;
    private String migrationContainer;
    
    public void createRegistry(@Observes ArquillianDescriptor event) {
        ContainerRegistry reg = new Registry(injector.get());
        ServiceLoader serviceLoader = loader.get();

        @SuppressWarnings("rawtypes")
        Collection<DeployableContainer> containers = serviceLoader.all(DeployableContainer.class);

        if (containers.isEmpty()) {
            throw new IllegalStateException("There are not any container adapters on the classpath");
        }

        for (ContainerDef container : event.getContainers()) {
            if (isCreatingContainer(container, containers)) {
                if (isEnabled(container)) {
                    checkMultipleEnabledContainers(container);
                    reg.create(container, serviceLoader);
                } else {
                    log.info("Container is disabled: " + container.getContainerName());
                }
            }
        }

        for (GroupDef group : event.getGroups()) {
            for (ContainerDef container : group.getGroupContainers()) {
                if (isCreatingContainer(container, containers)) {
                    if (isEnabled(container)) {
                        //TODO add checkMultipleEnabledContainers according to groups
                        reg.create(container, serviceLoader);
                    } else {
                        log.info("Container is disabled: " + container.getContainerName());
                    }
                }
            }
        }

        registry.set(reg);
    }

    private static final String ENABLED = "enabled";

    private boolean isEnabled(ContainerDef containerDef) {
        Map<String, String> props = containerDef.getContainerProperties();
        return !props.containsKey(ENABLED)
                || (props.containsKey(ENABLED) && props.get(ENABLED).equals("true"));
    }
    
    private void checkMultipleEnabledContainers(ContainerDef containerDef) {
        String containerName = containerDef.getContainerName();
        
        if (containerName.startsWith("keycloak")) {
            if (migrationContainer == null) {
                migrationContainer = containerName;
            } else {
                throw new RuntimeException("There is more than one migration container "
                        + "enabled in arquillian.xml. It has to be enabled at most one. "
                        + "Do not activate more than one migration profile.");
            }
        } else if (containerName.startsWith("auth-server")) {
            if (authContainer == null) {
                authContainer = containerName;
            } else {
                throw new RuntimeException("There is more than one auth containec enabled "
                        + "in arquillian.xml. It has to be enabled exactly one. Do not "
                        + "activate more than one auth profile.");
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean isCreatingContainer(ContainerDef containerDef, Collection<DeployableContainer> containers) {

        if (hasAdapterImplClassProperty(containerDef)) {
            if (isClassPresent(getAdapterImplClassValue(containerDef))) {
                return DeployableContainer.class.isAssignableFrom(
                        loadClass(getAdapterImplClassValue(containerDef)));
            }
        }

        return false;
    }

    public static boolean hasAdapterImplClassProperty(ContainerDef containerDef) {
        for (Map.Entry<String, String> entry : containerDef.getContainerProperties().entrySet()) {
            if (entry.getKey().equals(ADAPTER_IMPL_CONFIG_STRING)) {
                return true;
            }
        }
        return false;
    }

    public static String getAdapterImplClassValue(ContainerDef containerDef) {
        return containerDef.getContainerProperties().get(ADAPTER_IMPL_CONFIG_STRING).trim();
    }
    public static final String ADAPTER_IMPL_CONFIG_STRING = "adapterImplClass";

    @SuppressWarnings("rawtypes")
    public static DeployableContainer<?> getContainerAdapter(String adapterImplClass, Collection<DeployableContainer> containers) {
        Validate.notNullOrEmpty(adapterImplClass, "The value of " + ADAPTER_IMPL_CONFIG_STRING + " can not be a null object "
                + "nor an empty string!");

        Class<?> foundAdapter = null;

        if (isClassPresent(adapterImplClass)) {
            foundAdapter = loadClass(adapterImplClass);
        } else {
            return null;
        }

        for (DeployableContainer<?> container : containers) {
            if (foundAdapter.isInstance(container)) {
                return container;
            }
        }

        return null;
    }

}
