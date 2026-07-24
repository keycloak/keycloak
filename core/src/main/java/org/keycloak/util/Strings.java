package org.keycloak.util;

public class Strings {

    private Strings() {
    }

    /**
     * Returns true if string is null, empty, or only contains spaces
     * @param str
     * @return
     */
    public static Boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

}
