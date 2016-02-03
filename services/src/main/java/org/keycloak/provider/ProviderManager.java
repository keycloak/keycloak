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
package org.keycloak.provider;

import org.keycloak.services.ServicesLogger;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ProviderManager {

    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private List<ProviderLoader> loaders = new LinkedList<ProviderLoader>();
    private Map<String, List<ProviderFactory>> cache = new HashMap<String, List<ProviderFactory>>();

    public ProviderManager(ClassLoader baseClassLoader, String... resources) {
        List<ProviderLoaderFactory> factories = new LinkedList<ProviderLoaderFactory>();
        for (ProviderLoaderFactory f : ServiceLoader.load(ProviderLoaderFactory.class, getClass().getClassLoader())) {
            factories.add(f);
        }

        logger.debugv("Provider loaders {0}", factories);

        loaders.add(new DefaultProviderLoader(baseClassLoader));

        if (resources != null) {
            for (String r : resources) {
                String type = r.substring(0, r.indexOf(':'));
                String resource = r.substring(r.indexOf(':') + 1, r.length());

                boolean found = false;
                for (ProviderLoaderFactory f : factories) {
                    if (f.supports(type)) {
                        loaders.add(f.create(baseClassLoader, resource));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Provider loader for " + r + " not found");
                }
            }
        }
    }

    public synchronized List<ProviderFactory> load(Spi spi) {
        List<ProviderFactory> factories = cache.get(spi.getName());
        if (factories == null) {
            factories = new LinkedList<ProviderFactory>();
            IdentityHashMap factoryClasses = new IdentityHashMap();
            for (ProviderLoader loader : loaders) {
                List<ProviderFactory> f = loader.load(spi);
                if (f != null) {
                    for (ProviderFactory pf: f) {
                        // make sure there are no duplicates
                        if (!factoryClasses.containsKey(pf.getClass())) {
                            factories.add(pf);
                            factoryClasses.put(pf.getClass(), pf);
                        }
                    }
                }
            }
        }
        return factories;
    }

    public synchronized ProviderFactory load(Spi spi, String providerId) {
        for (ProviderFactory f : load(spi)) {
            if (f.getId().equals(providerId)) {
                return f;
            }
        }
        return null;
    }

}
