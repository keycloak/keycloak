/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.common;


import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils.getMapValueFromSet;
import static org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils.migrateMapToSet;
import static org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils.migrateSetToMap;
import static org.keycloak.models.map.storage.hotRod.common.HotRodTypesUtils.removeFromSetByMapKey;

public class HotRodTypesUtilsTest {

    @Test
    public void testMigrateMapToSet() {
        // Test null map
        assertThat(migrateMapToSet((Map<String, String>) null, Map.Entry::getKey), nullValue());

        Map<String, String> m = new HashMap<>();
        m.put("key", "value");

        assertThat(migrateMapToSet(m, e -> e.getKey() + "#" + e.getValue()),
                contains("key#value"));
    }

    @Test
    public void testMigrateSetToMap() {
        // Test null map
        assertThat(migrateSetToMap((Set<String>) null, Function.identity(), Function.identity()), nullValue());

        Set<String> s = new HashSet<>();
        s.add("key#value");

        Map<String, String> result = HotRodTypesUtils.migrateSetToMap(s, e -> e.split("#")[0], e -> e.split("#")[1]);

        assertThat(result.keySet(), hasSize(1));
        assertThat(result, hasEntry("key", "value"));
    }

    @Test
    public void testRemoveFromSetByMapKey() {
        assertThat(removeFromSetByMapKey((Set<String>) null, null, Function.identity()), is(false));
        assertThat(removeFromSetByMapKey(Collections.emptySet(), null, Function.identity()), is(false));

        Set<String> s = new HashSet<>();
        s.add("key#value");
        s.add("key1#value1");
        s.add("key2#value2");

        // Remove existing
        Set<String> testSet = new HashSet<>(s);
        assertThat(removeFromSetByMapKey(testSet, "key", e -> e.split("#")[0]), is(true));
        assertThat(testSet, hasSize(2));
        assertThat(testSet, containsInAnyOrder("key1#value1", "key2#value2"));

        // Remove not existing
        testSet = new HashSet<>(s);
        assertThat(removeFromSetByMapKey(testSet, "key3", e -> e.split("#")[0]), is(false));
        assertThat(testSet, hasSize(3));
        assertThat(testSet, containsInAnyOrder("key#value", "key1#value1", "key2#value2"));
    }

    @Test
    public void testGetMapValueFromSet() {
        assertThat(getMapValueFromSet((Set<String>) null, null, Function.identity(), Function.identity()), nullValue());
        assertThat(getMapValueFromSet(Collections.emptySet(), "key", Function.identity(), Function.identity()), nullValue());

        Set<String> s = new HashSet<>();
        s.add("key#value");
        s.add("key1#value1");
        s.add("key2#value2");

        // search existing
        assertThat(getMapValueFromSet(s, "key", e -> e.split("#")[0], e -> e.split("#")[1]), is("value"));
        assertThat(getMapValueFromSet(s, "key1", e -> e.split("#")[0], e -> e.split("#")[1]), is("value1"));

        // Search not existing
        assertThat(getMapValueFromSet(s, "key3", e -> e.split("#")[0], e -> e.split("#")[1]), nullValue());
    }
}