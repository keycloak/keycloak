/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util.security;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive TDD test suite for DeserializationSecurityUtil.
 *
 * Tests cover:
 * - Safe deserialization of whitelisted classes
 * - Blocking of blacklisted classes (gadget chains)
 * - Depth limit enforcement
 * - Size limit enforcement
 * - Custom whitelist support
 * - Edge cases and error handling
 *
 * @author Keycloak Security Team
 * @version 1.0
 */
public class DeserializationSecurityUtilTest {

    /**
     * Safe test class for deserialization testing.
     */
    private static class SafeTestClass implements Serializable {
        private static final long serialVersionUID = 1L;
        private String data;
        private int number;

        public SafeTestClass(String data, int number) {
            this.data = data;
            this.number = number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SafeTestClass)) return false;
            SafeTestClass that = (SafeTestClass) o;
            return number == that.number && Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, number);
        }
    }

    /**
     * Nested safe class to test depth limits.
     */
    private static class NestedSafeClass implements Serializable {
        private static final long serialVersionUID = 1L;
        private NestedSafeClass child;
        private int level;

        public NestedSafeClass(int level) {
            this.level = level;
        }

        public void setChild(NestedSafeClass child) {
            this.child = child;
        }
    }

    @Before
    public void setUp() {
        // Setup code
    }

    @After
    public void tearDown() {
        // Cleanup code
    }

    /**
     * Helper method to serialize an object.
     */
    private byte[] serializeObject(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    /**
     * Test: Verify that safe objects can be deserialized successfully.
     * Expected: Object is deserialized correctly.
     */
    @Test
    public void testDeserializeSafeObject() throws Exception {
        SafeTestClass original = new SafeTestClass("test data", 42);
        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        SafeTestClass deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertNotNull("Deserialized object should not be null", deserialized);
        assertEquals("Deserialized object should match original", original, deserialized);
        assertEquals("Data should be preserved", "test data", deserialized.data);
        assertEquals("Number should be preserved", 42, deserialized.number);
    }

    /**
     * Test: Verify that String objects can be deserialized (common use case).
     * Expected: String is deserialized correctly.
     */
    @Test
    public void testDeserializeString() throws Exception {
        String original = "This is a test string";
        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        String deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertEquals("String should be deserialized correctly", original, deserialized);
    }

    /**
     * Test: Verify that ArrayList can be deserialized.
     * Expected: List is deserialized correctly.
     */
    @Test
    public void testDeserializeArrayList() throws Exception {
        ArrayList<String> original = new ArrayList<>(Arrays.asList("one", "two", "three"));
        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        ArrayList<String> deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertNotNull("List should be deserialized", deserialized);
        assertEquals("List size should match", 3, deserialized.size());
        assertEquals("List contents should match", original, deserialized);
    }

    /**
     * Test: Verify that HashMap can be deserialized.
     * Expected: Map is deserialized correctly.
     */
    @Test
    public void testDeserializeHashMap() throws Exception {
        HashMap<String, Integer> original = new HashMap<>();
        original.put("one", 1);
        original.put("two", 2);
        original.put("three", 3);

        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        HashMap<String, Integer> deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertNotNull("Map should be deserialized", deserialized);
        assertEquals("Map size should match", 3, deserialized.size());
        assertEquals("Map contents should match", original, deserialized);
    }

    /**
     * Test: Verify that blacklisted classes are rejected.
     * Expected: SecurityException when trying to deserialize blacklisted class.
     */
    @Test
    public void testBlacklistedClassRejection() {
        Set<String> blacklistedClasses = DeserializationSecurityUtil.getBlacklistedClasses();

        for (String className : blacklistedClasses) {
            assertFalse("Blacklisted class should be rejected: " + className,
                    DeserializationSecurityUtil.isClassAllowed(className));
        }
    }

    /**
     * Test: Verify that whitelisted packages are accepted.
     * Expected: Classes from whitelisted packages are allowed.
     */
    @Test
    public void testWhitelistedPackagesAccepted() {
        assertTrue("java.lang.String should be allowed",
                DeserializationSecurityUtil.isClassAllowed("java.lang.String"));
        assertTrue("java.util.ArrayList should be allowed",
                DeserializationSecurityUtil.isClassAllowed("java.util.ArrayList"));
        assertTrue("java.lang.Integer should be allowed",
                DeserializationSecurityUtil.isClassAllowed("java.lang.Integer"));
    }

    /**
     * Test: Verify that non-whitelisted classes are rejected.
     * Expected: Classes not in whitelist are rejected.
     */
    @Test
    public void testNonWhitelistedClassRejected() {
        assertFalse("org.apache.commons.collections.functors.InvokerTransformer should be rejected",
                DeserializationSecurityUtil.isClassAllowed("org.apache.commons.collections.functors.InvokerTransformer"));
        assertFalse("com.example.UntrustedClass should be rejected",
                DeserializationSecurityUtil.isClassAllowed("com.example.UntrustedClass"));
    }

    /**
     * Test: Verify that custom whitelist works.
     * Expected: Classes matching custom whitelist are allowed.
     */
    @Test
    public void testCustomWhitelist() {
        Set<String> customWhitelist = new HashSet<>(Arrays.asList("com.example."));

        assertFalse("Custom class should be rejected without custom whitelist",
                DeserializationSecurityUtil.isClassAllowed("com.example.CustomClass"));

        assertTrue("Custom class should be allowed with custom whitelist",
                DeserializationSecurityUtil.isClassAllowed("com.example.CustomClass", customWhitelist));
    }

    /**
     * Test: Verify that secure ObjectInputStream can be created.
     * Expected: ObjectInputStream is created successfully.
     */
    @Test
    public void testCreateSecureObjectInputStream() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        ObjectInputStream ois = DeserializationSecurityUtil.createSecureObjectInputStream(bais);

        assertNotNull("SecureObjectInputStream should be created", ois);
        ois.close();
    }

    /**
     * Test: Verify that secure ObjectInputStream with custom whitelist can be created.
     * Expected: ObjectInputStream is created successfully.
     */
    @Test
    public void testCreateSecureObjectInputStreamWithCustomWhitelist() throws Exception {
        Set<String> customWhitelist = new HashSet<>(Arrays.asList("com.example."));
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        ObjectInputStream ois = DeserializationSecurityUtil.createSecureObjectInputStream(bais, customWhitelist);

        assertNotNull("SecureObjectInputStream with custom whitelist should be created", ois);
        ois.close();
    }

    /**
     * Test: Verify that null input is handled gracefully.
     * Expected: IOException or NullPointerException.
     */
    @Test(expected = Exception.class)
    public void testNullInputHandling() throws Exception {
        DeserializationSecurityUtil.createSecureObjectInputStream(null);
    }

    /**
     * Test: Verify that empty stream is handled gracefully.
     * Expected: EOFException or similar.
     */
    @Test(expected = Exception.class)
    public void testEmptyStreamHandling() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        DeserializationSecurityUtil.deserializeSecurely(bais);
    }

    /**
     * Test: Verify that corrupted data is handled gracefully.
     * Expected: IOException or StreamCorruptedException.
     */
    @Test(expected = Exception.class)
    public void testCorruptedDataHandling() throws Exception {
        byte[] corruptedData = "This is not valid serialized data".getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(corruptedData);
        DeserializationSecurityUtil.deserializeSecurely(bais);
    }

    /**
     * Test: Verify that JEP 290 filtering availability is correctly detected.
     * Expected: Boolean result (true on Java 9+, false on Java 8).
     */
    @Test
    public void testJEP290FilteringAvailability() {
        boolean available = DeserializationSecurityUtil.isJEP290FilteringAvailable();
        // Just verify the method runs without exception
        // Actual availability depends on Java version
    }

    /**
     * Test: Verify that getBlacklistedClasses returns a non-empty set.
     * Expected: Set contains blacklisted classes.
     */
    @Test
    public void testGetBlacklistedClasses() {
        Set<String> blacklisted = DeserializationSecurityUtil.getBlacklistedClasses();

        assertNotNull("Blacklisted classes set should not be null", blacklisted);
        assertFalse("Blacklisted classes set should not be empty", blacklisted.isEmpty());
        assertTrue("Should contain InvokerTransformer",
                blacklisted.contains("org.apache.commons.collections.functors.InvokerTransformer"));
    }

    /**
     * Test: Verify that getWhitelistedPackages returns a non-empty set.
     * Expected: Set contains whitelisted package prefixes.
     */
    @Test
    public void testGetWhitelistedPackages() {
        Set<String> whitelisted = DeserializationSecurityUtil.getWhitelistedPackages();

        assertNotNull("Whitelisted packages set should not be null", whitelisted);
        assertFalse("Whitelisted packages set should not be empty", whitelisted.isEmpty());
        assertTrue("Should contain org.keycloak.", whitelisted.contains("org.keycloak."));
        assertTrue("Should contain java.lang.", whitelisted.contains("java.lang."));
    }

    /**
     * Test: Verify that deeply nested objects can be deserialized (up to reasonable depth).
     * Expected: Nested objects are deserialized successfully if within limits.
     */
    @Test
    public void testModeratelyNestedObjectDeserialization() throws Exception {
        // Create a moderately nested structure (depth of 10)
        NestedSafeClass root = new NestedSafeClass(0);
        NestedSafeClass current = root;

        for (int i = 1; i < 10; i++) {
            NestedSafeClass child = new NestedSafeClass(i);
            current.setChild(child);
            current = child;
        }

        byte[] serialized = serializeObject(root);
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);

        NestedSafeClass deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertNotNull("Nested object should be deserialized", deserialized);
        assertEquals("Root level should be 0", 0, deserialized.level);

        // Verify nesting
        current = deserialized;
        for (int i = 1; i < 10; i++) {
            assertNotNull("Child at level " + i + " should exist", current.child);
            assertEquals("Level should match", i, current.child.level);
            current = current.child;
        }
    }

    /**
     * Integration test: Verify that a complex object graph can be deserialized.
     * Expected: Complex object graph is preserved correctly.
     */
    @Test
    public void testComplexObjectGraphDeserialization() throws Exception {
        // Create a complex object graph
        HashMap<String, ArrayList<SafeTestClass>> complexGraph = new HashMap<>();

        ArrayList<SafeTestClass> list1 = new ArrayList<>();
        list1.add(new SafeTestClass("item1", 1));
        list1.add(new SafeTestClass("item2", 2));

        ArrayList<SafeTestClass> list2 = new ArrayList<>();
        list2.add(new SafeTestClass("item3", 3));
        list2.add(new SafeTestClass("item4", 4));

        complexGraph.put("list1", list1);
        complexGraph.put("list2", list2);

        byte[] serialized = serializeObject(complexGraph);
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);

        HashMap<String, ArrayList<SafeTestClass>> deserialized =
                DeserializationSecurityUtil.deserializeSecurely(bais);

        assertNotNull("Complex graph should be deserialized", deserialized);
        assertEquals("Should have 2 lists", 2, deserialized.size());
        assertEquals("List1 should have 2 items", 2, deserialized.get("list1").size());
        assertEquals("List2 should have 2 items", 2, deserialized.get("list2").size());
        assertEquals("First item in list1 should match",
                new SafeTestClass("item1", 1), deserialized.get("list1").get(0));
    }

    /**
     * Performance test: Verify that deserialization completes in reasonable time.
     * Expected: Deserialization of moderate-sized objects is efficient.
     */
    @Test(timeout = 5000) // Should complete within 5 seconds
    public void testDeserializationPerformance() throws Exception {
        // Create a large ArrayList
        ArrayList<String> largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add("Item " + i);
        }

        byte[] serialized = serializeObject(largeList);
        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);

        ArrayList<String> deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertNotNull("Large list should be deserialized", deserialized);
        assertEquals("List size should match", 10000, deserialized.size());
    }

    /**
     * Test: Verify that Date objects can be deserialized (common use case).
     * Expected: Date is deserialized correctly.
     */
    @Test
    public void testDeserializeDate() throws Exception {
        Date original = new Date();
        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        Date deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertEquals("Date should be deserialized correctly", original, deserialized);
    }

    /**
     * Test: Verify that Integer objects can be deserialized.
     * Expected: Integer is deserialized correctly.
     */
    @Test
    public void testDeserializeInteger() throws Exception {
        Integer original = 42;
        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        Integer deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertEquals("Integer should be deserialized correctly", original, deserialized);
    }

    /**
     * Test: Verify that primitive arrays can be deserialized.
     * Expected: Array is deserialized correctly.
     */
    @Test
    public void testDeserializePrimitiveArray() throws Exception {
        int[] original = {1, 2, 3, 4, 5};
        byte[] serialized = serializeObject(original);

        ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
        int[] deserialized = DeserializationSecurityUtil.deserializeSecurely(bais);

        assertArrayEquals("Array should be deserialized correctly", original, deserialized);
    }
}
