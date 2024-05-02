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
import java.util.Map;

/**
 * <p>Provides a way for obtaining the KeycloakSession
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 *
 * @deprecated use org.keycloak.util.KeycloakSessionUtil instead
 */
@Deprecated
public final class Resteasy {

    private static final ThreadLocal<Map<Class<?>, Object>> contextualData = new ThreadLocal<Map<Class<?>, Object>>() {
        @Override
        protected Map<Class<?>, Object> initialValue() {
            return new HashMap<>(1);
        };
    };

    /**
     * Push the given {@code instance} with type/key {@code type} to the context associated with the current thread.
     * <br>Should not be called directly
     *
     * @param type the type/key to associate the {@code instance} with
     * @param instance the instance
     */
    public static <R> R pushContext(Class<R> type, R instance) {
        return (R) contextualData.get().put(type, instance);
    }

    /**
     * Clear the context associated with the current thread.
     * <br>Should not be called directly
     */
    public static void clearContextData() {
        contextualData.remove();
    }

    /**
     * Lookup the instance associated with the given type/key {@code type} from the context associated with the current thread.
     * <br> Should only be used to obtain the KeycloakSession
     *
     * @param type the type/key to lookup
     * @return the instance associated with the given {@code type} or null if non-existent.
     */
    public static <R> R getContextData(Class<R> type) {
        return (R) contextualData.get().get(type);
    }

    /**
     * Push the given {@code instance} with type/key {@code type} to the Resteasy global context.
     *
     * @param type the type/key to associate the {@code instance} with
     * @param instance the instance
     */
    @Deprecated
    public static void pushDefaultContextObject(Class type, Object instance) {
        pushContext(type, instance);
    }

}
