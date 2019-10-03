/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * <p>Provides a layer of indirection to abstract invocations to Resteasy internal APIs. Making also possible to use different
 * versions of Resteasy (e.g.: v3 and v4) depending on the stack that the server is running.
 *
 * <p>The methods herein provided are basically related with accessing context data from Resteasy, which changed in latest versions of Resteasy.
 *
 * <p>It is important to use this class when access to context data is necessary in order to avoid incompatibilities with future
 * versions of Resteasy.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Resteasy {

    private static final BiConsumer<Class, Object> PUSH_CONTEXT;
    private static final BiConsumer<Class, Object> PUSH_DEFAULT_OBJECT;
    private static final Function<Class, Object> PULL_CONTEXT;
    private static final Runnable CLEAR_CONTEXT;

    static {
        if (isRestEasy4()) {
            PUSH_CONTEXT = new BiConsumer<Class, Object>() {
                @Override
                public void accept(Class p1, Object p2) {
                    ResteasyContext.pushContext(p1, p2);
                }
            };
            PUSH_DEFAULT_OBJECT = new BiConsumer<Class, Object>() {
                @Override
                public void accept(Class p1, Object p2) {
                    ResteasyContext.getContextData(org.jboss.resteasy.spi.Dispatcher.class).getDefaultContextObjects()
                            .put(p1, p2);
                }
            };
            PULL_CONTEXT = new Function<Class, Object>() {
                @Override
                public Object apply(Class p1) {
                    return ResteasyContext.getContextData(p1);
                }
            };
            CLEAR_CONTEXT = new Runnable() {
                @Override
                public void run() {
                    ResteasyContext.clearContextData();
                }
            };
        } else {
            PUSH_CONTEXT = new BiConsumer<Class, Object>() {
                @Override
                public void accept(Class p1, Object p2) {
                    ResteasyProviderFactory.getInstance().pushContext(p1, p2);
                }
            };
            PUSH_DEFAULT_OBJECT = new BiConsumer<Class, Object>() {
                @Override
                public void accept(Class p1, Object p2) {
                    ResteasyProviderFactory.getInstance().getContextData(Dispatcher.class).getDefaultContextObjects()
                            .put(p1, p2);
                }
            };
            PULL_CONTEXT = new Function<Class, Object>() {
                @Override
                public Object apply(Class p1) {
                    return ResteasyProviderFactory.getInstance().getContextData(p1);
                }
            };
            CLEAR_CONTEXT = new Runnable() {
                @Override
                public void run() {
                    ResteasyProviderFactory.getInstance().clearContextData();
                }
            };
        }
    }

    /**
     * Push the given {@code instance} with type/key {@code type} to the Resteasy context associated with the current thread.
     * 
     * @param type the type/key to associate the {@code instance} with 
     * @param instance the instance
     */
    public static void pushContext(Class type, Object instance) {
        PUSH_CONTEXT.accept(type, instance);
    }

    /**
     * Lookup the instance associated with the given type/key {@code type} from the Resteasy context associated with the current thread.
     *
     * @param type the type/key to lookup 
     * @return the instance associated with the given {@code type} or null if non-existent.                
     */
    public static <R> R getContextData(Class<R> type) {
        return (R) PULL_CONTEXT.apply(type);
    }

    /**
     * Clear the Resteasy context associated with the current thread.
     */
    public static void clearContextData() {
        CLEAR_CONTEXT.run();
    }

    /**
     * Push the given {@code instance} with type/key {@code type} to the Resteasy global context.
     *
     * @param type the type/key to associate the {@code instance} with
     * @param instance the instance
     */
    public static void pushDefaultContextObject(Class type, Object instance) {
        PUSH_DEFAULT_OBJECT.accept(type, instance);
    }

    private static boolean isRestEasy4() {
        try {
            return Class.forName("org.jboss.resteasy.core.ResteasyContext") != null;
        } catch (ClassNotFoundException ignore) {
            return false;
        }
    }

    /**
     * Only necessary because keycloak-common is constrained to JDK 1.7.
     */
    private interface BiConsumer<T, S> {
        void accept(T p1, S p2);
    }

    /**
     * Only necessary because keycloak-common is constrained to JDK 1.7.
     */
    private interface Function<T, R> {
        R apply(T p1);
    }
}
