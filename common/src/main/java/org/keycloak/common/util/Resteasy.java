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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

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

    private static ResteasyProvider provider;

    static {
        Iterator<ResteasyProvider> iter = ServiceLoader.load(ResteasyProvider.class, Resteasy.class.getClassLoader()).iterator();
        if (iter.hasNext()) {
            provider = iter.next();
        }
    }

    private static final ThreadLocal<Map<Class<?>, Object>> contextualData = new ThreadLocal<Map<Class<?>, Object>>() {
        @Override
        protected Map<Class<?>, Object> initialValue() {
            return new HashMap<>();
        };
    };

    public static ResteasyProvider getProvider() {
        return provider;
    }

    /**
     * Push the given {@code instance} with type/key {@code type} to the Resteasy context associated with the current thread.
     *
     * @param type the type/key to associate the {@code instance} with
     * @param instance the instance
     */
    public static void pushContext(Class type, Object instance) {
        contextualData.get().put(type, instance);
    }

    /**
     * Clear the Resteasy context associated with the current thread.
     */
    public static void clearContextData() {
        contextualData.remove();
    }

    /**
     * Lookup the instance associated with the given type/key {@code type} from the Resteasy context associated with the current thread, or from the provider.
     *
     * @param type the type/key to lookup
     * @return the instance associated with the given {@code type} or null if non-existent.
     */
    public static <R> R getContextData(Class<R> type) {
        R result = (R) contextualData.get().get(type);
        if (result != null) {
            return result;
        }
        return provider.getContextData(type);
    }

    /**
     * Push the given {@code instance} with type/key {@code type} to the Resteasy global context.
     *
     * @param type the type/key to associate the {@code instance} with
     * @param instance the instance
     * @deprecated use {@link #pushContext(Class, Object)}
     */
    @Deprecated
    public static void pushDefaultContextObject(Class type, Object instance) {
        pushContext(type, instance);
    }

}
