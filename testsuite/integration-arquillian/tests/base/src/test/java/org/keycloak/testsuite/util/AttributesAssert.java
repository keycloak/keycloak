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
