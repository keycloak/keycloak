package org.keycloak.testsuite.adapter;

/**
 *
 * @author tkyjovsk
 */
public enum AdapterLibsMode {
    
    PROVIDED("provided"),
    BUNDLED("bundled");

    private final String type;

    private AdapterLibsMode(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static AdapterLibsMode getByType(String type) {
        for (AdapterLibsMode s : AdapterLibsMode.values()) {
            if (s.getType().equals(type)) {
                return s;
            }
        }
        return null;
    }
}
