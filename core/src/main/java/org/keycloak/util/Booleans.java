package org.keycloak.util;

public class Booleans {

    /**
     * Checks if a boolean is true, including support for null values where null is considered false
     *
     * @param b the boolean to check
     * @return true if non-null and true
     */
    public static Boolean isTrue(Boolean b) {
        return b != null && b;
    }

    /**
     * Checks if a boolean is false, including support for null values where null is considered false
     *
     * @param b the boolean to check
     * @return true if null and false
     */
    public static Boolean isFalse(Boolean b) {
        return b == null || !b;
    }

    /**
     * Compares two boolean, including support for null values where null is considered false
     *
     * @param a the first boolean to compare
     * @param b the second boolean to compare
     * @return true if both values have resolves to the same value
     */
    public static Boolean equals(Boolean a, Boolean b) {
        a = a != null && a;
        b = b != null && b;
        return a.equals(b);
    }

}
