package org.keycloak.protocol.ssf.event;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InitiatingEntity {
    ADMIN("admin"),
    USER("user"),
    POLICY("policy"),
    SYSTEM("system"),
    ;

    private final String code;

    InitiatingEntity(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
