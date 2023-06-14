/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.common;

import java.util.Objects;
import java.util.function.Function;

/**
 * This class contains utility classes for type conversion.
 *
 * @author hmlnarik
 */
public class CastUtils {

    /**
     * Converts value to destination class (if it can).
     * @param value Value to convert
     * @param toClass Class to convert value to
     * @return Value converted to the given class
     * @throws IllegalStateException if the value cannot be converted to the requested class
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value, Class<T> toClass) {
        return value == null ? null : ((Function<Object, T>) getCastFunc(value.getClass(), toClass)).apply(value);
    }

    /**
     * Provides a function to convert value of a given class to destination class (if it can).
     * @param fromClass Class to convert value from
     * @param toClass Class to convert value to
     * @return Function {@code fromClass -> toClass} converting values from the {@code fromClass} to the {@code toClass}
     * @throws IllegalStateException if the value cannot be converted to the requested class
     */
    public static <E extends Enum<E>> Function<?, ?> getCastFunc(Class<?> fromClass, Class<?> toClass) {
        if (fromClass == toClass || toClass.isAssignableFrom(fromClass)) {
            return Function.identity();
        }
        if (toClass == String.class) {
            return Objects::toString;
        }
        if (fromClass == String.class) {
            if (toClass == Integer.class) {
                return (Function<String, ?>) Integer::valueOf;
            } else if (toClass == Long.class) {
                return (Function<String, ?>) Long::valueOf;
            } else if (toClass == Boolean.class) {
                return (Function<String, ?>) Boolean::valueOf;
            } else if (toClass.isEnum()) {
                @SuppressWarnings("unchecked")
                Class<E> enumClass = (Class<E>) toClass;
                return (String value) -> Enum.valueOf(enumClass, value);
            }
        }
        if (fromClass == Long.class) {
            if (toClass == Integer.class) {
                return (Function<Long, ?>) Long::intValue;
            }
        }
        if (fromClass == Integer.class) {
            if (toClass == Long.class) {
                return (Function<Integer, ?>) Integer::longValue;
            }
        }

        throw new IllegalStateException("Unknown cast: " + fromClass + " -> " + toClass);
    }

}

