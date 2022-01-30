package org.keycloak.testsuite.webauthn.utils;

import org.keycloak.WebAuthnConstants;

import java.util.Arrays;

public enum PropertyRequirement {
    NOT_SPECIFIED(WebAuthnConstants.OPTION_NOT_SPECIFIED),
    YES("Yes"),
    NO("No");

    private final String value;

    PropertyRequirement(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PropertyRequirement fromValue(String value) {
        return Arrays.stream(PropertyRequirement.values())
                .filter(f -> f.getValue().equals(value))
                .findFirst()
                .orElse(NOT_SPECIFIED);
    }
}
