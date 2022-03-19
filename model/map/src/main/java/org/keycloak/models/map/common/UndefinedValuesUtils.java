/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.util.Collection;
import java.util.Map;

/**
 * This Util class defines conditions when objects can be considered undefined
 * <br/>
 * <br/>
 * For example:
 * <ul>
 *  <li>{@link String} is undefined if it is {@code null} or {@code empty}</li>
 *  <li>{@link Collection} is undefined if it is {@code null}, {@code empty} or all items are undefined</li>
 *  <li>{@link Map} is undefined if it is {@code null}, {@code empty}, or all values are undefined</li>
 * </ul>
 */
public class UndefinedValuesUtils {

    /**
     * Decides whether the {@link Object o} is defined or not
     *
     * @param o object to check
     * @return true when the {@link Object o} is undefined, false otherwise
     */
    public static boolean isUndefined(Object o) {
        if (o == null) {
            return true;
        } else if (o instanceof Collection) {
            return isUndefinedCollection((Collection<?>) o);
        } else if (o instanceof Map) {
            return isUndefinedMap((Map<?, ?>) o);
        } else if (o instanceof String) {
            return isUndefinedString((String) o);
        } else {
            return false;
        }
    }

    private static boolean isUndefinedCollection(Collection<?> collection) {
        return collection.isEmpty() || collection.stream().allMatch(UndefinedValuesUtils::isUndefined);
    }

    private static boolean isUndefinedMap(Map<?, ?> map) {
        return map.isEmpty() || map.values().stream().allMatch(UndefinedValuesUtils::isUndefined);
    }

    private static boolean isUndefinedString(String str) {
        return str.trim().isEmpty();
    }
}
