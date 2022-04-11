/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:erik.mulder@docdatapayments.com">Erik Mulder</a>
 * 
 * Classloader implementation to facilitate loading classes and resources from a collection of other classloaders.
 * Effectively it forms a proxy to one or more other classloaders.
 * 
 * The way it works:
 * - Get list of classloaders, which will be used as "delegates" when loaded classes or resources.
 *   - Can be retrived from provided classloaders or alternatively from the provided classes where the "delegate classloaders" will be determined from the classloaders of given classes
 * - For each class or resource that is 'requested':
 *   - First try all provided classloaders and if we have a match, return that
 *   - If no match was found: proceed with 'normal' classloading in 'current classpath' scope
 * 
 * In this particular context: only loadClass and getResource overrides are needed, since those
 * are the methods that a classloading and resource loading process will need.
 */
public class ProxyClassLoader extends ClassLoader {

    private Set<ClassLoader> classloaders;

    /**
     * Init classloader with the list of given delegates
     * @param delegateClassLoaders
     */
    public ProxyClassLoader(ClassLoader... delegateClassLoaders) {
        if (delegateClassLoaders == null || delegateClassLoaders.length == 0) {
            throw new IllegalStateException("At least one classloader to delegate must be provided");
        }
        classloaders = new LinkedHashSet<>();
        classloaders.addAll(Arrays.asList(delegateClassLoaders));
    }

    /**
     * Get all unique classloaders from the provided classes to be used as "Delegate classloaders"
     * @param classes
     */
    public ProxyClassLoader(Collection<Class<?>> classes) {
    	init(classes);
    }

    private void init(Collection<Class<?>> classes) {
        classloaders = new LinkedHashSet<>();
        for (Class<?> clazz : classes) {
            classloaders.add(clazz.getClassLoader());
        }
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader classloader : classloaders) {
            try {
                return classloader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // This particular class loader did not find the class. It's expected behavior that
                // this can happen, so we'll just ignore the exception and let the next one try.
            }
        }
        // We did not find the class in the proxy class loaders, so proceed with 'normal' behavior.
        return super.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader classloader : classloaders) {
            URL resource = classloader.getResource(name);
            if (resource != null) {
                return resource;
            }
            // Resource == null means not found, so let the next one try.
        }
        // We could not get the resource from the proxy class loaders, so proceed with 'normal' behavior.
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final LinkedHashSet<URL> resourceUrls = new LinkedHashSet();

        for (ClassLoader classloader : classloaders) {
            Enumeration<URL> child = classloader.getResources(name);

            while (child.hasMoreElements()) {
                resourceUrls.add(child.nextElement());
            }
        }

        return new Enumeration<URL>() {
            final Iterator<URL> resourceUrlIterator = resourceUrls.iterator();

            public boolean hasMoreElements() {
                return this.resourceUrlIterator.hasNext();
            }

            public URL nextElement() {
                return (URL)this.resourceUrlIterator.next();
            }
        };
    }

    @Override
    public String toString() {
        return "ProxyClassLoader: Delegates: " + classloaders;
    }
}
