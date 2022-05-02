package org.keycloak.quarkus.runtime.configuration.validators;

public class PortValidator {

    public static Integer validate(String s) {

        if (s.isEmpty()) {
            throw new IllegalArgumentException("Port cannot be empty.");
        }

        Integer intValue;
        try {
            intValue =  Integer.parseInt(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Input: \"" + s + "\" Is not a valid number.");
        }

        if( intValue < 0 || intValue > 65535 ) {
            throw new IllegalArgumentException("Invalid Input: \"" + intValue + "\" Is not a number between 0 and 65535.");
        }

        return intValue;
    }
}
