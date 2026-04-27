/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.common.util;

import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

public class DelegatingSerializationFilter {
    private static final Logger LOG = Logger.getLogger(DelegatingSerializationFilter.class.getName());

    private static final SerializationFilterAdapter serializationFilterAdapter = isJava6To8() ? createOnJava6To8Adapter() : createOnJavaAfter8Adapter();

    private static boolean isJava6To8() {
        List<String> olderVersions = Arrays.asList("1.6", "1.7", "1.8");
        return olderVersions.contains(System.getProperty("java.specification.version"));
    }

    private DelegatingSerializationFilter() {
    }

    public static DelegatingSerializationFilter.FilterPatternBuilder builder() {
        return new DelegatingSerializationFilter.FilterPatternBuilder();
    }

    private void setFilter(ObjectInputStream ois, String filterPattern) {
        LOG.debug("Using: " + serializationFilterAdapter.getClass().getSimpleName());

        if (serializationFilterAdapter.getObjectInputFilter(ois) == null) {
            serializationFilterAdapter.setObjectInputFilter(ois, filterPattern);
        }
    }

    interface SerializationFilterAdapter {

        Object getObjectInputFilter(ObjectInputStream ois);

        void setObjectInputFilter(ObjectInputStream ois, String filterPattern);
    }

    private static SerializationFilterAdapter createOnJava6To8Adapter() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> objectInputFilterClass = cl.loadClass("sun.misc.ObjectInputFilter");
            Class<?> objectInputFilterConfigClass = cl.loadClass("sun.misc.ObjectInputFilter$Config");
            Method getObjectInputFilter = objectInputFilterConfigClass.getDeclaredMethod("getObjectInputFilter", ObjectInputStream.class);
            Method setObjectInputFilter = objectInputFilterConfigClass.getDeclaredMethod("setObjectInputFilter", ObjectInputStream.class, objectInputFilterClass);
            Method createFilter = objectInputFilterConfigClass.getDeclaredMethod("createFilter", String.class);
            LOG.info("Using OnJava6To8 serialization filter adapter");
            return new OnJava6To8(getObjectInputFilter, setObjectInputFilter, createFilter);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // This can happen for older JDK updates.
            LOG.warn("Could not configure SerializationFilterAdapter. For better security, it is highly recommended to upgrade to newer JDK version update!");
            LOG.warn("For the Java 7, the recommended update is at least 131 (1.7.0_131 or newer). For the Java 8, the recommended update is at least 121 (1.8.0_121 or newer).");
            LOG.warn("Error details", e);
            return new EmptyFilterAdapter();
        }
    }

    private static SerializationFilterAdapter createOnJavaAfter8Adapter() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> objectInputFilterClass = cl.loadClass("java.io.ObjectInputFilter");
            Class<?> objectInputFilterConfigClass = cl.loadClass("java.io.ObjectInputFilter$Config");
            Class<?> objectInputStreamClass = cl.loadClass("java.io.ObjectInputStream");
            Method getObjectInputFilter = objectInputStreamClass.getDeclaredMethod("getObjectInputFilter");
            Method setObjectInputFilter = objectInputStreamClass.getDeclaredMethod("setObjectInputFilter", objectInputFilterClass);
            Method createFilter = objectInputFilterConfigClass.getDeclaredMethod("createFilter", String.class);
            LOG.info("Using OnJavaAfter8 serialization filter adapter");
            return new OnJavaAfter8(getObjectInputFilter, setObjectInputFilter, createFilter);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // This can happen for older JDK updates.
            LOG.warn("Could not configure SerializationFilterAdapter. For better security, it is highly recommended to upgrade to newer JDK version update!");
            LOG.warn("Error details", e);
            return new EmptyFilterAdapter();
        }
    }

    // If codebase stays on Java 8 for a while you could use Java 8 classes directly without reflection
    static class OnJava6To8 implements SerializationFilterAdapter {

        private final Method getObjectInputFilterMethod;
        private final Method setObjectInputFilterMethod;
        private final Method createFilterMethod;

        private OnJava6To8(Method getObjectInputFilterMethod, Method setObjectInputFilterMethod, Method createFilterMethod) {
            this.getObjectInputFilterMethod = getObjectInputFilterMethod;
            this.setObjectInputFilterMethod = setObjectInputFilterMethod;
            this.createFilterMethod = createFilterMethod;
        }

        public Object getObjectInputFilter(ObjectInputStream ois) {
            try {
                return getObjectInputFilterMethod.invoke(null, ois);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.warn("Could not read ObjectFilter from ObjectInputStream: " + e.getMessage());
                return null;
            }
        }

        public void setObjectInputFilter(ObjectInputStream ois, String filterPattern) {
            try {
                Object objectFilter = createFilterMethod.invoke(null, filterPattern);
                setObjectInputFilterMethod.invoke(null, ois, objectFilter);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.warn("Could not set ObjectFilter: " + e.getMessage());
            }
        }
    }


    static class EmptyFilterAdapter implements SerializationFilterAdapter {

        @Override
        public Object getObjectInputFilter(ObjectInputStream ois) {
            return null;
        }

        @Override
        public void setObjectInputFilter(ObjectInputStream ois, String filterPattern) {

        }

    }


    // If codebase moves to Java 9+ could use Java 9+ classes directly without reflection and keep the old variant with reflection
    static class OnJavaAfter8 implements SerializationFilterAdapter {

        private final Method getObjectInputFilterMethod;
        private final Method setObjectInputFilterMethod;
        private final Method createFilterMethod;

        private OnJavaAfter8(Method getObjectInputFilterMethod, Method setObjectInputFilterMethod, Method createFilterMethod) {
            this.getObjectInputFilterMethod = getObjectInputFilterMethod;
            this.setObjectInputFilterMethod = setObjectInputFilterMethod;
            this.createFilterMethod = createFilterMethod;
        }

        public Object getObjectInputFilter(ObjectInputStream ois) {
            try {
                return getObjectInputFilterMethod.invoke(ois);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.warn("Could not read ObjectFilter from ObjectInputStream: " + e.getMessage());
                return null;
            }
        }

        public void setObjectInputFilter(ObjectInputStream ois, String filterPattern) {
            try {
                Object objectFilter = createFilterMethod.invoke(ois, filterPattern);
                setObjectInputFilterMethod.invoke(ois, objectFilter);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOG.warn("Could not set ObjectFilter: " + e.getMessage());
            }
        }
    }


    public static class FilterPatternBuilder {

        private Set<Class> classes = new HashSet<>();
        private Set<String> patterns = new HashSet<>();

        public FilterPatternBuilder() {
            // Add "java.util" package by default (contains all the basic collections)
            addAllowedPattern("java.util.*");
        }

        /**
         * This is used when the caller of this method can't use the {@link #addAllowedClass(Class)}. For example because the
         * particular is private or it is not available at the compile time. Or when adding the whole package like "java.util.*"
         *
         * @param pattern
         * @return
         */
        public FilterPatternBuilder addAllowedPattern(String pattern) {
            this.patterns.add(pattern);
            return this;
        }

        public FilterPatternBuilder addAllowedClass(Class javaClass) {
            this.classes.add(javaClass);
            return this;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            for (Class javaClass : classes) {
                builder.append(javaClass.getName()).append(";");
            }
            for (String pattern : patterns) {
                builder.append(pattern).append(";");
            }

            builder.append("!*");

            return builder.toString();
        }

        public void setFilter(ObjectInputStream ois) {
            DelegatingSerializationFilter filter = new DelegatingSerializationFilter();
            String filterPattern = this.toString();
            filter.setFilter(ois, filterPattern);
        }
    }
}
