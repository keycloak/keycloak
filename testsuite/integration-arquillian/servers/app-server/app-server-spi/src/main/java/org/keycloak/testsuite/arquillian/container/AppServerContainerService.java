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

package org.keycloak.testsuite.arquillian.container;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.shrinkwrap.descriptor.spi.node.Node;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class AppServerContainerService  {

    private static AppServerContainerService service;
    private final ServiceLoader<AppServerContainerProvider> loader;

    private AppServerContainerService() {
        loader = ServiceLoader.load(AppServerContainerProvider.class);
    }

    public static synchronized AppServerContainerService getInstance() {
        if (service == null) {
            service = new AppServerContainerService();
        }
        return service;
    }

    public List<Node> getContainers(String appServerName) {
        List<Node> containers = null;
        try {
            Iterator<AppServerContainerProvider> definitions = loader.iterator();

            List<AppServerContainerProvider> availableDefinitions = new ArrayList<>();
            while (definitions != null && definitions.hasNext()) {
                availableDefinitions.add(definitions.next());
            }
            for (AppServerContainerProvider def : availableDefinitions) {
                if (def.getName().equals(appServerName)) {
                    containers = def.getContainers();
                }
            }
        } catch (ServiceConfigurationError serviceError) {
            containers = null;
            throw serviceError;
        }
        return containers;
    }
}
