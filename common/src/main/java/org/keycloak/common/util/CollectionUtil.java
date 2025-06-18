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

package org.keycloak.common.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:jeroen.rosenberg@gmail.com">Jeroen Rosenberg</a>
 */
public class CollectionUtil {

    public static String join(Collection<String> strings) {
        return join(strings, ", ");
    }

    public static String join(Collection<String> strings, String separator) {
        return strings.stream().collect(Collectors.joining(String.valueOf(separator)));
    }

    // Return true if all items from col1 are in col2 and viceversa. Order is not taken into account
    public static <T> boolean collectionEquals(Collection<T> col1, Collection<T> col2) {
        if (col1.size()!=col2.size()) {
            return false;
        }
        Map<T, Integer> countMap = new HashMap<>();
        for(T o : col1) {
            countMap.merge(o, 1, (v1, v2) -> v1 + v2);
        }
        for(T o : col2) {
            Integer v = countMap.get(o);
            if (v==null) {
                return false;
            }
            if (v == 1) {
                countMap.remove(o);
            } else {
                countMap.put(o, v-1);
            }
        }
        return countMap.isEmpty();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> Set<T> collectionToSet(Collection<T> collection) {
        return collection == null ? null : new HashSet<>(collection);
    }
}
