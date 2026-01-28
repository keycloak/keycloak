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

package org.keycloak.utils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SearchQueryUtilsTest {
    @Test
    public void testGetFields() {
        testParseQuery("   key1:val1 nokey key2:\"val 2\" key3:val3   ",
                "key1", "val1",
                "key2", "val 2",
                "key3", "val3");

        testParseQuery("   key1:val1   ",
                "key1", "val1");

        testParseQuery("   key1:\"val1\"   ",
                "key1", "val1");

        testParseQuery("key1:val=\"123456\"",
                "key1", "val=\"123456\"");

        testParseQuery("key1:\"val=\\\"12 34 56\\\"\"",
                "key1", "val=\"12 34 56\"");

        testParseQuery("   \"key 1\":val1",
                "key 1", "val1");

        testParseQuery("\"key \\\"1\\\"\":val1",
                "key \"1\"", "val1");

        testParseQuery("\"key \\\"1\\\"\":\"val \\\"1\\\"\"",
                "key \"1\"", "val \"1\"");

        testParseQuery("key\"1\":val1",
                "key\"1\"", "val1");

        testParseQuery("k:val1",
                "k", "val1");
    }

    private void testParseQuery(String query, String... expectedStr) {
        Map<String, String> expected = new HashMap<>();
        if (expectedStr != null) {
            if (expectedStr.length % 2 != 0) {
                throw new IllegalArgumentException("Expected must be key-value pairs");
            }
            for (int i = 0; i < expectedStr.length; i=i+2) {
                expected.put(expectedStr[i], expectedStr[i+1]);
            }
        }

        Map<String, String> actual = SearchQueryUtils.getFields(query);

        assertEquals(expected, actual);
    }

    @Test
    public void testReDoS() {
        long start = System.currentTimeMillis();
        int count = 50000;
        for (int i = 0; i < count; i++) {
            SearchQueryUtils.getFields(" ".repeat(1443) + "\n\n".repeat(1443) + 0);
        }
        long end = System.currentTimeMillis() - start;
        System.out.println("took: " + end + " milliseconds");
    }
}
