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
package org.keycloak.saml.processing.core.parsers.util;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged Blocks
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 25, 2010
 */
public class SecurityActions {

    /**
     * <p>Returns a system property value using the specified <code>key</code>. If not found the
     * <code>defaultValue</code> will be returned.</p>
     *
     * @param key
     * @param defaultValue
     *
     * @return
     */
    static String getSystemProperty(final String key, final String defaultValue) {
        SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(key, defaultValue);
                }
            });
        } else {
            return System.getProperty(key, defaultValue);
        }
    }

    /**
     * Load a resource based on the passed {@link Class} classloader. Failing which try with the Thread Context CL
     *
     * @param clazz
     * @param resourceName
     *
     * @return
     */
    static URL loadResource(final Class<?> clazz, final String resourceName) {
        SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<URL>() {
                public URL run() {
                    URL url = null;
                    ClassLoader clazzLoader = clazz.getClassLoader();
                    url = clazzLoader.getResource(resourceName);

                    if (url == null) {
                        clazzLoader = Thread.currentThread().getContextClassLoader();
                        url = clazzLoader.getResource(resourceName);
                    }

                    return url;
                }
            });
        } else {
            URL url = null;
            ClassLoader clazzLoader = clazz.getClassLoader();
            url = clazzLoader.getResource(resourceName);

            if (url == null) {
                clazzLoader = Thread.currentThread().getContextClassLoader();
                url = clazzLoader.getResource(resourceName);
            }

            return url;
        }
    }
}