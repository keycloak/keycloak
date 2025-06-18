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

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author tkyjovsk
 */
public class MultivaluedHashMapTest {

    public <T, R> void equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(MultivaluedHashMap<T, R> map, MultivaluedHashMap<T, R> equalMap) {
        assertTrue(String.format("MultivaluedHashMap.equalsIgnoreValueOrder() should return `true` for the same object. \nmap: %s", map),
                map.equalsIgnoreValueOrder(map));
        assertTrue(String.format("MultivaluedHashMap.equalsIgnoreValueOrder() should return `true` for maps that are equal. \nmap1: %s \nmap2: %s", map, equalMap),
                map.equalsIgnoreValueOrder(equalMap));
        assertTrue(String.format("MultivaluedHashMap.equalsIgnoreValueOrder() should return `true` for maps that are equal. \nmap1: %s \nmap2: %s", equalMap, map),
                equalMap.equalsIgnoreValueOrder(map));
    }

    public <T, R> void equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(MultivaluedHashMap<T, R> map, MultivaluedHashMap<T, R> differentMap) {
        assertFalse(String.format("MultivaluedHashMap.equalsIgnoreValueOrder() should return `false` for maps that are different. \nmap1: %s \nmap2: %s", map, differentMap),
                map.equalsIgnoreValueOrder(differentMap));
        assertFalse(String.format("MultivaluedHashMap.equalsIgnoreValueOrder() should return `false` for maps that are different. \nmap1: %s \nmap2: %s", differentMap, map),
                differentMap.equalsIgnoreValueOrder(map));
    }

    @Test
    public void testEqualsIgnoreValueOrder_exactlyEqual() {
        MultivaluedHashMap<Integer, Integer> map = new MultivaluedHashMap<>();
        MultivaluedHashMap<Integer, Integer> equalMap = new MultivaluedHashMap<>();
        map.put(1, Arrays.asList(1, 2, 3));
        equalMap.put(1, Arrays.asList(1, 2, 3));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);

        map.put(2, Arrays.asList(4, 5, 6));
        equalMap.put(2, Arrays.asList(4, 5, 6));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);

        map.put(3, Arrays.asList(7, 8, 9));
        equalMap.put(3, Arrays.asList(7, 8, 9));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);
    }

    @Test
    public void testEqualsIgnoreValueOrder_sameLengthSameValues() {
        MultivaluedHashMap<Integer, Integer> map = new MultivaluedHashMap<>();
        MultivaluedHashMap<Integer, Integer> equalMap = new MultivaluedHashMap<>();
        map.put(1, Arrays.asList(1, 2, 3));
        equalMap.put(1, Arrays.asList(3, 2, 1));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);

        map.put(2, Arrays.asList(4, 5, 6));
        equalMap.put(2, Arrays.asList(5, 6, 4));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);

        map.put(3, Arrays.asList(7, 8, 9));
        equalMap.put(3, Arrays.asList(9, 7, 8));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);

        map.clear();
        equalMap.clear();
        map.put(1, Arrays.asList(1, 2, 3, 4, 5));
        map.put(2, Arrays.asList(4, 5, 6, 7, 8));
        equalMap.put(1, Arrays.asList(4, 3, 2, 5, 1));
        equalMap.put(2, Arrays.asList(6, 7, 4, 8, 5));
        equalsIgnoreValueOrder_shouldReturnTrueForEqualMaps(map, equalMap);
    }

    @Test
    public void testEqualsIgnoreValueOrder_sameLengthDifferentValues() {
        MultivaluedHashMap<Integer, Integer> map = new MultivaluedHashMap<>();
        MultivaluedHashMap<Integer, Integer> differentMap = new MultivaluedHashMap<>();
        map.put(1, Arrays.asList(1, 2, 3));
        differentMap.put(1, Arrays.asList(1, 2, 2));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap);

        map.clear();
        differentMap.clear();
        map.put(1, Arrays.asList(1, 2, 3));
        map.put(2, Arrays.asList(4, 5, 6));
        differentMap.put(1, Arrays.asList(1, 2, 3));
        differentMap.put(2, Arrays.asList(4, 5, 5));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap);
    }

    @Test
    public void testEqualsIgnoreValueOrder_differentLengthSameValues() {
        MultivaluedHashMap<Integer, Integer> map = new MultivaluedHashMap<>();
        MultivaluedHashMap<Integer, Integer> differentMap = new MultivaluedHashMap<>();
        map.put(1, Arrays.asList(1, 2, 3));
        differentMap.put(1, Arrays.asList(1, 2, 2, 3));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap);

        map.clear();
        differentMap.clear();
        map.put(1, Arrays.asList(1, 2, 3));
        map.put(2, Arrays.asList(4, 5, 6));
        differentMap.put(1, Arrays.asList(1, 2, 3));
        differentMap.put(2, Arrays.asList(4, 5, 5, 4, 6));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap);
    }

    @Test
    public void testEqualsIgnoreValueOrder_differentLengthDifferentValues() {
        MultivaluedHashMap<Integer, Integer> map = new MultivaluedHashMap<>();
        MultivaluedHashMap<Integer, Integer> differentMap = new MultivaluedHashMap<>();
        map.put(1, Arrays.asList(1, 2, 3));
        differentMap.put(1, Arrays.asList(1, 2, 3, 4));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap);

        map.clear();
        differentMap.clear();
        map.put(1, Arrays.asList(1, 2, 3));
        map.put(2, Arrays.asList(4, 5, 6));
        differentMap.put(1, Arrays.asList(1, 2, 3));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap); // diff entrySet size

        differentMap.put(2, Arrays.asList(4, 5, 6, 7));
        equalsIgnoreValueOrder_shouldReturnFalseForDifferentMaps(map, differentMap);
    }

}
