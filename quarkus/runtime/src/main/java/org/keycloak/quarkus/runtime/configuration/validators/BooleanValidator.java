package org.keycloak.quarkus.runtime.configuration.validators;

public class BooleanValidator {

    public static Boolean validate(String s) {
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("\"" + s + "\" cannot be parsed as a boolean value.");
        }
    }
}
