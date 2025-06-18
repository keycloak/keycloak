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
package org.keycloak.saml.processing.core.saml.v2.factories;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged Blocks
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 9, 2008
 */
class SecurityActions {

    /**
     * <p>
     * Loads a {@link Class} using the <code>fullQualifiedName</code> supplied. This method tries first to load from
     * the
     * specified {@link Class}, if not found it will try to load from using TCL.
     * </p>
     *
     * @param theClass
     * @param fullQualifiedName
     *
     * @return
     */
    static Class<?> loadClass(final Class<?> theClass, final String fullQualifiedName) {
        SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                public Class<?> run() {
                    ClassLoader classLoader = theClass.getClassLoader();

                    Class<?> clazz = loadClass(classLoader, fullQualifiedName);
                    if (clazz == null) {
                        classLoader = Thread.currentThread().getContextClassLoader();
                        clazz = loadClass(classLoader, fullQualifiedName);
                    }
                    return clazz;
                }
            });
        } else {
            ClassLoader classLoader = theClass.getClassLoader();

            Class<?> clazz = loadClass(classLoader, fullQualifiedName);
            if (clazz == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
                clazz = loadClass(classLoader, fullQualifiedName);
            }
            return clazz;
        }
    }

    /**
     * <p>
     * Loads a class from the specified {@link ClassLoader} using the <code>fullQualifiedName</code> supplied.
     * </p>
     *
     * @param classLoader
     * @param fullQualifiedName
     *
     * @return
     */
    static Class<?> loadClass(final ClassLoader classLoader, final String fullQualifiedName) {
        SecurityManager sm = System.getSecurityManager();

        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                public Class<?> run() {
                    try {
                        return classLoader.loadClass(fullQualifiedName);
                    } catch (ClassNotFoundException e) {
                    }
                    return null;
                }
            });
        } else {
            try {
                return classLoader.loadClass(fullQualifiedName);
            } catch (ClassNotFoundException e) {
            }
            return null;
        }
    }

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

}