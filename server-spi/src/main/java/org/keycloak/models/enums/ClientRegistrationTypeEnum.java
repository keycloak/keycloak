package org.keycloak.models.enums;

public enum ClientRegistrationTypeEnum {
    //AUTOMATIC("automatic"),
    EXPLICIT("explicit");

    private final String value;

    ClientRegistrationTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
