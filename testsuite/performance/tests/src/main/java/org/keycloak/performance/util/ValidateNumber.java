package org.keycloak.performance.util;

import org.apache.commons.validator.routines.IntegerValidator;

/**
 *
 * @author tkyjovsk
 */
public class ValidateNumber {

    private static IntegerValidator validate() {
        return IntegerValidator.getInstance();
    }

    public static void minValue(int i, int min, String message) {
        if (!validate().minValue(i, min)) {
            throw new IllegalArgumentException(String.format("Value '%s' lower than the expected minimum: %s. %s", i, min, message));
        }
    }

    public static void minValue(int i, int min) {
        minValue(i, min, "");
    }

    public static void maxValue(int i, int max, String message) {
        if (!validate().maxValue(i, max)) {
            throw new IllegalArgumentException(String.format("Value '%s' greater than the expected maximum: %s. %s", i, max, message));
        }
    }

    public static void maxValue(int i, int max) {
        maxValue(i, max, "");
    }

    public static void isInRange(int i, int min, int max, String message) {
        if (!validate().isInRange(i, min, max)) {
            throw new IllegalArgumentException(String.format("Value '%s' is outside of the expected range: <%s, %s>. %s", i, min, max, message));
        }
    }

    public static void isInRange(int i, int min, int max) {
        isInRange(i, min, max, "");
    }

}
