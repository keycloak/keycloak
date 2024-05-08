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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
        Iterator<String> iter = strings.iterator();
        StringBuilder sb = new StringBuilder();
        if(iter.hasNext()){
            sb.append(iter.next());
            while(iter.hasNext()){
                sb.append(separator).append(iter.next());
            }
        }
        return sb.toString();
    }

    // Return true if all items from col1 are in col2 and viceversa. Order is not taken into account
    public static <T> boolean collectionEquals(Collection<T> col1, Collection<T> col2) {
        if (col1.size()!=col2.size()) {
            return false;
        }
        Map<T, Integer> countMap = new HashMap<>();
        for(T o : col1) {
            Integer v = countMap.get(o);
            countMap.put(o, v==null ? 1 : v+1);
        }
        for(T o : col2) {
            Integer v = countMap.get(o);
            if (v==null) {
                return false;
            }
            countMap.put(o, v-1);
        }
        for(Integer count : countMap.values()) {
            if (count!=0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> Set<T> intersection(Collection<T> col1, Collection<T> col2) {
        if (isEmpty(col1) || isEmpty(col2)) return Collections.emptySet();

        final Collection<T> iteratorCollection = col1.size() <= col2.size() ? col1 : col2;
        final Collection<T> searchCollection = iteratorCollection.equals(col1) ? col2 : col1;

        return iteratorCollection.stream()
                .filter(searchCollection::contains)
                .collect(Collectors.toSet());
    }

    public static <T> Set<T> collectionToSet(Collection<T> collection) {
        return collection == null ? null : new HashSet<>(collection);
    }
}
