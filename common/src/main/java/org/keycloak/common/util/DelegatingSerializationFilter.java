package org.keycloak.common.util;

import org.jboss.logging.Logger;

import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class DelegatingSerializationFilter {
    private static final Logger LOG = Logger.getLogger(DelegatingSerializationFilter.class.getName());

    private static final SerializationFilterAdapter serializationFilterAdapter = isJava6To8() ? createOnJava6To8Adapter() : new OnJavaAfter8();

    private static boolean isJava6To8() {
        List<String> olderVersions = Arrays.asList("1.6", "1.7", "1.8");
        return olderVersions.contains(System.getProperty("java.specification.version"));
    }

    public void setFilter(ObjectInputStream ois, String filterPattern) {
        LOG.info("Using: " + serializationFilterAdapter.getClass().getSimpleName());

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
            return new OnJava6To8(getObjectInputFilter, setObjectInputFilter, createFilter);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // This can happen for older JDK updates.
            LOG.warn("Could not configure SerializationFilterAdapter. For better security, it is highly recommended to upgrade to newer JDK version update!");
            LOG.warn("For the Java 7, the recommended update is at least 131 (1.7.0_131 or newer). For the Java 8, the recommended update is at least 121 (1.8.0_121 or newer).");
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

        private static final Method getObjectInputFilterMethod;
        private static final Method setObjectInputFilterMethod;
        private static final Method createFilterMethod;

        static {

            Method getObjectInputFilter;
            Method setObjectInputFilter;
            Method createFilter;

            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> objectInputFilterClass = cl.loadClass("java.io.ObjectInputFilter");
                Class<?> objectInputFilterConfigClass = cl.loadClass("java.io.ObjectInputFilter$Config");
                Class<?> objectInputStreamClass = cl.loadClass("java.io.ObjectInputStream");
                getObjectInputFilter = objectInputStreamClass.getDeclaredMethod("getObjectInputFilter");
                setObjectInputFilter = objectInputStreamClass.getDeclaredMethod("setObjectInputFilter", objectInputFilterClass);
                createFilter = objectInputFilterConfigClass.getDeclaredMethod("createFilter", String.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                LOG.warn("Could not configure SerializationFilterAdapter: " + e.getMessage());
                getObjectInputFilter = null;
                setObjectInputFilter = null;
                createFilter = null;
            }

            getObjectInputFilterMethod = getObjectInputFilter;
            setObjectInputFilterMethod = setObjectInputFilter;
            createFilterMethod = createFilter;
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
}
