/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.util;

import java.io.IOException;

import org.keycloak.util.JsonSerialization;

import org.junit.Assert;

/**
 * Utility for comparing JSON objects
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JsonTestUtils {

    /**
     * @param o1
     * @param o2
     * @return true if JSON objects are "equal" to each other
     * @param <T>
     */
    public static <T> void assertJsonEquals(T o1, T o2) {
        try {
            String o1Stripped = JsonSerialization.writeValueAsString(o1);
            String o2Stripped = JsonSerialization.writeValueAsString(o2);
            Assert.assertEquals(o1Stripped, o2Stripped);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Compare Object in the JSON node with the "unparsed" String version of that object
     *
     * @param o1 String with JSON. Assumption is, that it can be "read" to the same object class of object o1
     * @param o2
     * @return
     */
    public static void assertJsonEquals(String o1, Object o2) {
        try {
            Object o1Object = JsonSerialization.readValue(o1, o2.getClass());
            assertJsonEquals(o1Object, o2);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Compares if 2 strings logically refers same JSON.
     *
     * @param o1
     * @param o2
     * @param clazz Java class, which strings o1 and o2 can be read into
     * @return
     */
    public static void assertJsonEquals(String o1, String o2, Class<?> clazz) {
        try {
            Object o1Object = JsonSerialization.readValue(o1, clazz);
            Object o2Object = JsonSerialization.readValue(o2, clazz);
            assertJsonEquals(o1Object, o2Object);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
