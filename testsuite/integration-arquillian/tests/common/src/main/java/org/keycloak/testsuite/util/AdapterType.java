package org.keycloak.testsuite.util;

/**
 *
 * @author tkyjovsk
 */
public enum AdapterType {
    
    PROVIDED("provided"),
    BUNDLED("bundled");

    private final String type;

    private AdapterType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static AdapterType getByType(String type) {
        for (AdapterType s : AdapterType.values()) {
            if (s.getType().equals(type)) {
                return s;
            }
        }
        return null;
    }
}
