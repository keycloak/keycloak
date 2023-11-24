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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * A set of privileged actions that are not to leak out of this package
 *
 * @version $Revision: $
 */
final class SecurityActions {

    // -------------------------------------------------------------------------------||
    // Constructor -------------------------------------------------------------------||
    // -------------------------------------------------------------------------------||
    /**
     * No instantiation
     */
    private SecurityActions() {
        throw new UnsupportedOperationException("No instantiation");
    }

    // -------------------------------------------------------------------------------||
    // Utility Methods ---------------------------------------------------------------||
    // -------------------------------------------------------------------------------||
    /**
     * Obtains the Thread Context ClassLoader
     */
    static ClassLoader getThreadContextClassLoader() {
        return AccessController.doPrivileged(GetTcclAction.INSTANCE);
    }

    static boolean isClassPresent(String name) {
        try {
            loadClass(name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, true, getThreadContextClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className, true, SecurityActions.class.getClassLoader());
            } catch (ClassNotFoundException e2) {
                throw new RuntimeException("Could not load class " + className, e2);
            }
        }
    }

    static <T> T newInstance(final String className, final Class<?>[] argumentTypes, final Object[] arguments,
            final Class<T> expectedType) {
        return newInstance(className, argumentTypes, arguments, expectedType, getThreadContextClassLoader());
    }

    static <T> T newInstance(final String className, final Class<?>[] argumentTypes, final Object[] arguments,
            final Class<T> expectedType, ClassLoader classLoader) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className, false, classLoader);
        } catch (Exception e) {
            throw new RuntimeException("Could not load class " + className, e);
        }
        Object obj = newInstance(clazz, argumentTypes, arguments);
        try {
            return expectedType.cast(obj);
        } catch (Exception e) {
            throw new RuntimeException("Loaded class " + className + " is not of expected type " + expectedType, e);
        }
    }

    /**
     * Create a new instance by finding a constructor that matches the
     * argumentTypes signature using the arguments for instantiation.
     *
     * @param className Full classname of class to create
     * @param argumentTypes The constructor argument types
     * @param arguments The constructor arguments
     * @return a new instance
     * @throws IllegalArgumentException if className, argumentTypes, or
     * arguments are null
     * @throws RuntimeException if any exceptions during creation
     * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
     * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
     */
    static <T> T newInstance(final Class<T> implClass, final Class<?>[] argumentTypes, final Object[] arguments) {
        if (implClass == null) {
            throw new IllegalArgumentException("ImplClass must be specified");
        }
        if (argumentTypes == null) {
            throw new IllegalArgumentException("ArgumentTypes must be specified. Use empty array if no arguments");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("Arguments must be specified. Use empty array if no arguments");
        }
        final T obj;
        try {
            Constructor<T> constructor = getConstructor(implClass, argumentTypes);
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            obj = constructor.newInstance(arguments);
        } catch (Exception e) {
            throw new RuntimeException("Could not create new instance of " + implClass, e);
        }

        return obj;
    }

    /**
     * Obtains the Constructor specified from the given Class and argument types
     *
     * @param clazz
     * @param argumentTypes
     * @return
     * @throws NoSuchMethodException
     */
    static <T> Constructor<T> getConstructor(final Class<T> clazz, final Class<?>... argumentTypes)
            throws NoSuchMethodException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor<T>>() {
                @Override
                public Constructor<T> run() throws NoSuchMethodException {
                    return clazz.getDeclaredConstructor(argumentTypes);
                }
            });
        } // Unwrap
        catch (final PrivilegedActionException pae) {
            final Throwable t = pae.getCause();
            // Rethrow
            if (t instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) t;
            } else {
                // No other checked Exception thrown by Class.getConstructor
                try {
                    throw (RuntimeException) t;
                } // Just in case we've really messed up
                catch (final ClassCastException cce) {
                    throw new RuntimeException("Obtained unchecked Exception; this code should never be reached", t);
                }
            }
        }
    }

    /**
     * Set a single Field value
     *
     * @param target The object to set it on
     * @param fieldName The field name
     * @param value The new value
     */
    public static void setFieldValue(final Class<?> source, final Object target, final String fieldName,
            final Object value) throws NoSuchFieldException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    Field field = source.getDeclaredField(fieldName);
                    if (!field.canAccess(target)) {
                        field.setAccessible(true);
                    }
                    field.set(target, value);
                    return null;
                }
            });
        } // Unwrap
        catch (final PrivilegedActionException pae) {
            final Throwable t = pae.getCause();
            // Rethrow
            if (t instanceof NoSuchFieldException) {
                throw (NoSuchFieldException) t;
            } else {
                // No other checked Exception thrown by Class.getConstructor
                try {
                    throw (RuntimeException) t;
                } // Just in case we've really messed up
                catch (final ClassCastException cce) {
                    throw new RuntimeException("Obtained unchecked Exception; this code should never be reached", t);
                }
            }
        }
    }

    public static List<Field> getFieldsWithAnnotation(final Class<?> source,
            final Class<? extends Annotation> annotationClass) {
        List<Field> declaredAccessableFields = AccessController.doPrivileged(new PrivilegedAction<List<Field>>() {
            @Override
            public List<Field> run() {
                List<Field> foundFields = new ArrayList<Field>();
                Class<?> nextSource = source;
                while (nextSource != Object.class) {
                    for (Field field : nextSource.getDeclaredFields()) {
                        if (field.isAnnotationPresent(annotationClass)) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            foundFields.add(field);
                        }
                    }
                    nextSource = nextSource.getSuperclass();
                }
                return foundFields;
            }
        });
        return declaredAccessableFields;
    }

    public static List<Method> getMethodsWithAnnotation(final Class<?> source,
            final Class<? extends Annotation> annotationClass) {
        List<Method> declaredAccessableMethods = AccessController.doPrivileged(new PrivilegedAction<List<Method>>() {
            @Override
            public List<Method> run() {
                List<Method> foundMethods = new ArrayList<Method>();
                Class<?> nextSource = source;
                while (nextSource != Object.class) {
                    for (Method method : nextSource.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(annotationClass)) {
                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            foundMethods.add(method);
                        }
                    }
                    nextSource = nextSource.getSuperclass();
                }
                return foundMethods;
            }
        });
        return declaredAccessableMethods;
    }

    static String getProperty(final String key) {
        try {
            String value = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(key);
                }
            });
            return value;
        } // Unwrap
        catch (final PrivilegedActionException pae) {
            final Throwable t = pae.getCause();
            // Rethrow
            if (t instanceof SecurityException) {
                throw (SecurityException) t;
            }
            if (t instanceof NullPointerException) {
                throw (NullPointerException) t;
            } else if (t instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) t;
            } else {
                // No other checked Exception thrown by System.getProperty
                try {
                    throw (RuntimeException) t;
                } // Just in case we've really messed up
                catch (final ClassCastException cce) {
                    throw new RuntimeException("Obtained unchecked Exception; this code should never be reached", t);
                }
            }
        }
    }

    // -------------------------------------------------------------------------------||
    // Inner Classes -----------------------------------------------------------------||
    // -------------------------------------------------------------------------------||
    /**
     * Single instance to get the TCCL
     */
    private enum GetTcclAction implements PrivilegedAction<ClassLoader> {

        INSTANCE;

        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }

    }
}
