/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jboss.logging.Logger;

/**
 * Reads {@code META-INF/keycloak/keycloak-providers.list} resources produced at build time
 * by the Quarkus deployment processor and resolves them to {@link ProviderFactory} classes.
 *
 * Falls back gracefully when the resource is absent (non-Quarkus contexts, embedded usage,
 * tests). Results are cached per classloader since the same lookup is performed once per SPI.
 */
public final class GeneratedProviderRegistry {

    private static final Logger logger = Logger.getLogger(GeneratedProviderRegistry.class);

    public static final String RESOURCE_PATH = "META-INF/keycloak/keycloak-providers.list";

    private static final Map<ClassLoader, Set<Class<? extends ProviderFactory>>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private GeneratedProviderRegistry() {
    }

    /**
     * Returns the {@link ProviderFactory} classes discovered at build time and visible to
     * {@code classLoader}. Order is preserved from the source resource (sorted by FQN by the
     * build step). Returns an empty set when no resource is available.
     */
    public static Set<Class<? extends ProviderFactory>> getProviderFactoryClasses(ClassLoader classLoader) {
        ClassLoader cl = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        Set<Class<? extends ProviderFactory>> cached = CACHE.get(cl);
        if (cached != null) {
            return cached;
        }
        Set<Class<? extends ProviderFactory>> loaded = Collections.unmodifiableSet(loadFromResources(cl));
        CACHE.put(cl, loaded);
        return loaded;
    }

    private static Set<Class<? extends ProviderFactory>> loadFromResources(ClassLoader classLoader) {
        Set<Class<? extends ProviderFactory>> result = new LinkedHashSet<>();
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(RESOURCE_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to look up " + RESOURCE_PATH, e);
        }

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String className = line.trim();
                    if (className.isEmpty() || className.startsWith("#")) {
                        continue;
                    }
                    Class<? extends ProviderFactory> factoryClass = resolveFactoryClass(className, classLoader, url);
                    if (factoryClass != null) {
                        result.add(factoryClass);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + url, e);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends ProviderFactory> resolveFactoryClass(String className, ClassLoader classLoader, URL source) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Generated provider registry " + source
                    + " lists '" + className + "' but the class is not on the classpath", e);
        }
        if (!ProviderFactory.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Class '" + className + "' from " + source
                    + " is not a " + ProviderFactory.class.getName());
        }
        return (Class<? extends ProviderFactory>) clazz;
    }
}
