package org.keycloak.models.enums;

public enum EntityTypeEnum {
    //OPENID_RELAYING_PARTY("openid_relying_party");
    OPENID_PROVIDER("openid_provider");

    private final String value;

    EntityTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
