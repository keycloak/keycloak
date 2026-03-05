package org.keycloak.protocol.oid4vc.policy;

public abstract class CredentialPolicy<T> {

    private final String name;
    private final String attrKey;
    private final Class<T> type;
    private final T expectedValue;
    private final T defaultValue;

    public CredentialPolicy(String name, String attrKey, Class<T> type, T expectedValue, T defaultValue) {
        this.name = name;
        this.attrKey = attrKey;
        this.type = type;
        this.expectedValue = expectedValue;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getAttrKey() {
        return attrKey;
    }

    public Class<T> getType() {
        return type;
    }

    public T getExpectedValue() {
        return expectedValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }
}
