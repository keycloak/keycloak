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
 *
 * @author hmlnarik
 */
public class CastUtils {
    
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value, Class<T> toClass) {
        return value == null ? null : (T) ((Function) getCastFunc(value.getClass(), toClass)).apply(value);
    }

    public static <E extends Enum<E>> Function<?, ?> getCastFunc(Class<?> fromClass, Class<?> toClass) {
        if (fromClass == toClass || toClass.isAssignableFrom(fromClass)) {
            return Function.identity();
        }
        if (toClass == String.class) {
            return Objects::toString;
        }
        if (fromClass == String.class) {
            if (toClass == Integer.class) {
                return (String s) -> Integer.valueOf(s);
            } else if (toClass == Long.class) {
                return (String s) -> Long.valueOf(s);
            } else if (toClass == Boolean.class) {
                return (String s) -> Boolean.valueOf(s);
            } else if (toClass.isEnum()) {
                @SuppressWarnings("unchecked")
                Class<E> enumClass = (Class<E>) toClass;
                return (String value) -> Enum.valueOf(enumClass, value);
            }
        }
        if (fromClass == Long.class) {
            if (toClass == Integer.class) {
                return (Long l) -> l.intValue();
            }
        }
        if (fromClass == Integer.class) {
            if (toClass == Long.class) {
                return (Integer l) -> l.longValue();
            }
        }

        throw new IllegalStateException("Unknown cast: " + fromClass + " -> " + toClass);
    }

}

