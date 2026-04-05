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

package org.keycloak.testsuite.util;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author tkyjovsk
 */
public class AttributesAssert {

    public static void assertEqualsStringAttributes(String a1, String a2) {
        if (a1 == null) {
            a1 = "";
        }
        if (a2 == null) {
            a2 = "";
        }
        assertEquals(a1, a2);
    }

    public static void assertEqualsBooleanAttributes(Boolean a1, Boolean a2) {
        if (a1 == null) {
            a1 = false;
        }
        if (a2 == null) {
            a2 = false;
        }
        assertEquals(a1, a2);
    }

    public static void assertEqualsListAttributes(List a1, List a2) {
        if (a1 == null || a1.isEmpty()) {
            a1 = null;
        }
        if (a2 == null || a2.isEmpty()) {
            a2 = null;
        }
        assertEquals(a1, a2);
    }

}
