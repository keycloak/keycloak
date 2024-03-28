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
 * <p>Provides a layer of indirection to abstract invocations to Resteasy internal APIs for obtaining the KeycloakSession
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
            return new HashMap<>(1);
        };
    };

    public static ResteasyProvider getProvider() {
        return provider;
    }

    /**
     * Push the given {@code instance} with type/key {@code type} to the Resteasy context associated with the current thread.
     * <br>Should not be called directly
     *
     * @param type the type/key to associate the {@code instance} with
     * @param instance the instance
     */
    public static <R> R pushContext(Class<R> type, R instance) {
        return (R) contextualData.get().put(type, instance);
    }

    /**
     * Clear the Resteasy context associated with the current thread.
     * <br>Should not be called directly
     */
    public static void clearContextData() {
        contextualData.remove();
    }

    /**
     * Lookup the instance associated with the given type/key {@code type} from the Resteasy context associated with the current thread, or from the provider.
     * <br> Should only be used to obtain the KeycloakSession
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
