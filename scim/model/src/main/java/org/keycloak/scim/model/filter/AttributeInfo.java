package org.keycloak.scim.model.filter;

import java.util.Objects;

public class AttributeInfo {

    private final String keycloakName;
    private final boolean primary;    // true = belong to the main resource entity, false = belong to a related entity (e.g. user attributes)
    private final String attributeType;

    public AttributeInfo(String keycloakName, boolean primary, String attributeType) {
        this.keycloakName = keycloakName;
        this.primary = primary;
        this.attributeType = attributeType;
    }

    public String getKeycloakName() {
        return keycloakName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isTimestamp() {
        return Objects.equals(attributeType, "timestamp");
    }

    public boolean isBoolean() {
        return Objects.equals(attributeType, "boolean");
    }
}
