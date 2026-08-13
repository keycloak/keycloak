package org.keycloak.protocol.oid4vc.clientpolicy;

import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;


public abstract class CredentialClientPolicy<T> {

    private final String name;
    private final String attrName;
    private final Class<T> type;
    private final T expectedValue;
    private final T defaultValue;

    public CredentialClientPolicy(String name, String attrName, Class<T> type, T expectedValue, T defaultValue) {
        this.name = name;
        this.attrName = attrName;
        this.expectedValue = expectedValue;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getAttrName() {
        return attrName;
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

    public abstract T getCurrentValue(CredentialScopeRepresentation credScope);
}
